require 'PZVRPlacement'
require 'PZAPI/ModOptions'
local M=PZVRPlacement
local enabled=false
local modelOption
local physicsOption
local function models() return modelOption and modelOption:getValue() or physicsOption and physicsOption:getValue() end
local active,cache,assembly,nextRefresh,sequence=nil,nil,nil,0,0
local generation=0
local nextPage,nextSend=nil,0
local nextRenew=0
local pending={}
local function publish()
    if not PZVRPhysicality then return end
    if enabled and models() and active and cache then
        PZVRPhysicality.storagePublish(active,cache.key,cache.rows)
        nextRenew=getTimestampMs()+500
    else nextRenew=0;PZVRPhysicality.storageClear() end
end
local function capacity()
    local count=0;for _ in pairs(pending) do count=count+1 end
    return count<8
end
local function requestId()
    sequence=sequence+1;if sequence>2147483647 then sequence=1 end
    return sequence
end
local function arguments(container,offset)
    local a=M.locator(container)
    if not a then return nil end
    a.v=M.version;a.request=requestId();a.offset=offset
    return a
end
local function membership(container)
    local list=container:getItems()
    local count=list:size()
    if not M.integer(count,0,M.maxItems) then return nil end
    local ids={}
    for n=0,count-1 do
        local item=list:get(n);local id=item and item:getID()
        if not item or item:getContainer()~=container or item:getWorldItem()
            or not M.integer(id,-2147483648,2147483647) or ids[id] then return nil end
        ids[id]=true
    end
    return ids
end
local function snapshot(container,offset)
    if not capacity() then return end
    local a=arguments(container,offset)
    if not a then return end
    local time=getTimestampMs()
    if offset==0 then
        generation=generation+1
        assembly={generation=generation,started=time,next=0,rows={},seen={},count=0,deltaRevs={}}
    elseif not assembly or offset~=assembly.next then return end
    pending[a.request]={container=container,time=time,offset=offset,generation=generation,snapshot=true}
    sendClientCommand(getSpecificPlayer(0),M.module,'snapshot',a)
end
local function restart(time)
    for id,ticket in pairs(pending) do if ticket.snapshot then pending[id]=nil end end
    assembly=nil;nextPage=nil;nextSend=time;nextRenew=time;nextRefresh=time+500
end
local function put(view,row,owned)
    if not view.rows[row.id] then
        if view.count>=M.maxItems then
            for id in pairs(view.rows) do
                if not owned[id] then view.rows[id]=nil;view.deltaRevs[id]=nil;view.count=view.count-1 end
            end
            if view.count>=M.maxItems then return false end
        end
        view.count=view.count+1
    end
    view.rows[row.id]=row
    return true
end
local function delta(view,row,revision,owned)
    if view.baseRev and revision<=view.baseRev then return true end
    local old=view.rows[row.id]
    if old and row.rev<=old.rev then return true end
    if not put(view,row,owned) then return false end
    view.deltaRevs[row.id]=revision
    view.rev=math.max(view.rev or 0,revision)
    return true
end
function M.visibleRows(container)
    if enabled and active==container and cache then return cache.rows,cache.total,cache.key end
    return {},0,nil
end
function M.place(container,id,pose)
    local p=M.pose(pose)
    if not enabled or active~=container or not cache or not p then return false end
    local old=cache.rows[id]
    if not old then return false end
    local owned=membership(container)
    if not owned or not owned[id] then return false end
    if isClient() then
        if not capacity() then return false end
        local a=arguments(container,0)
        if not a then return false end
        a.key=cache.key;a.id=id;a.expected=old.rev
        a.pose=p
        pending[a.request]={container=container,time=getTimestampMs(),edit=true,key=cache.key,id=id}
        sendClientCommand(getSpecificPlayer(0),M.module,'place',a)
    else
        local storage=M.storage(container)
        if not storage or storage.key~=cache.key then return false end
        local row=M.edit(container,storage,id,old.rev,p)
        if not row then return false end
        cache.rows[id]=row;cache.rev=storage.rev
        publish()
    end
    return true
end
local function receive(module,command,a)
    if module~=M.module or not enabled or not active or type(a)~='table' or a.v~=M.version then return end
    local time=getTimestampMs()
    local ticket=pending[a.request]
    if command=='changed' then
        if not cache and not assembly then return end
    elseif not ticket or ticket.container~=active or time<ticket.time or time-ticket.time>5000 then return end
    if command=='snapshot' then
        if not ticket.snapshot or not assembly or ticket.generation~=assembly.generation then return end
        if type(a.key)~='string' or #a.key<1 or #a.key>128 or not M.integer(a.rev,0,2147483646)
            or not M.integer(a.total,0,M.maxItems) or type(a.rows)~='table'
            or a.offset~=ticket.offset or not M.integer(a.offset,0,a.total)
            or a.offset~=assembly.next or #a.rows~=math.min(M.pageSize,a.total-a.offset)
            or time<assembly.started or time-assembly.started>20000 then restart(time);return end
        if a.offset==0 then
            if cache and cache.key==a.key and a.rev<(cache.baseRev or 0) then restart(time);return end
        elseif assembly.key~=a.key or assembly.baseRev~=a.rev or assembly.total~=a.total then restart(time);return end
        local page={}
        for n=1,#a.rows do
            local row=a.rows[n]
            local r=type(row)=='table' and M.record(row,a.key,row.id)
            if not r or page[r.id] or assembly.seen[r.id] then restart(time);return end
            page[r.id]=r
        end
        -- One current native ownership scan serves this page, its merges and final cleanup.
        -- The temporary boolean index retains no native item references between callbacks.
        local owned=membership(active)
        if not owned then restart(time);return end
        -- A detached candidate is never published until every frozen page is present.
        if a.offset==0 then
            if assembly.key and assembly.key~=a.key then assembly.rows={};assembly.deltaRevs={};assembly.count=0;assembly.rev=0 end
            assembly.key=a.key;assembly.baseRev=a.rev;assembly.total=a.total;assembly.rev=math.max(assembly.rev or 0,a.rev)
        end
        for id,r in pairs(page) do
            assembly.seen[id]=true
            if owned[id] then
                local newer=assembly.rows[id]
                if not newer or newer.rev<r.rev then if not put(assembly,r,owned) then restart(time);return end end
            end
        end
        pending[a.request]=nil
        assembly.next=a.offset+#a.rows
        if assembly.next<a.total then nextPage={container=active,offset=assembly.next}
        else
            if cache and cache.key==assembly.key then
                for id,revision in pairs(cache.deltaRevs) do
                    local row=cache.rows[id]
                    if row and revision>assembly.baseRev and owned[id] and not delta(assembly,row,revision,owned) then restart(time);return end
                end
            end
            for id in pairs(assembly.rows) do
                if not owned[id] or not assembly.seen[id] and (assembly.deltaRevs[id] or 0)<=assembly.baseRev then
                    assembly.rows[id]=nil;assembly.deltaRevs[id]=nil;assembly.count=assembly.count-1
                elseif (assembly.deltaRevs[id] or 0)<=assembly.baseRev then assembly.deltaRevs[id]=nil end
            end
            assembly.seen=nil;cache=assembly;assembly=nil;nextPage=nil;nextRefresh=time+1000;publish()
        end
    elseif command=='changed' or command=='placed' then
        if command=='placed' and (not ticket.edit or ticket.key~=a.key) then return end
        if type(a.key)~='string' or #a.key<1 or #a.key>128 or not M.integer(a.rev,0,2147483646) then return end
        if (not cache or a.key~=cache.key) and (not assembly or assembly.key and a.key~=assembly.key) then return end
        local row=type(a.row)=='table' and M.record(a.row,a.key,a.row.id)
        if not row or command=='placed' and row.id~=ticket.id then return end
        local owned=membership(active)
        if not owned or not owned[row.id] then return end
        if cache and cache.key==a.key then
            if not delta(cache,row,a.rev,owned) then restart(time);return end
            publish()
        end
        if assembly and (not assembly.key or assembly.key==a.key) then
            assembly.key=a.key
            if not delta(assembly,row,a.rev,owned) then restart(time);return end
        end
        if command=='placed' then pending[a.request]=nil end
    elseif command=='rejected' then pending[a.request]=nil;restart(time) end
end
local function refresh()
    local time=getTimestampMs()
    if time<nextRefresh-2000 then restart(time);pending={} end
    for id,ticket in pairs(pending) do if time<ticket.time or time-ticket.time>5000 then pending[id]=nil;restart(time) end end
    if assembly and (time<assembly.started or time-assembly.started>20000) then restart(time) end
    if enabled and models() and active and cache and time>=nextRenew then
        nextRenew=time+500
        if PZVRPhysicality and type(PZVRPhysicality.storageRefresh)=='function' then PZVRPhysicality.storageRefresh(active,cache.key) end
    end
    if enabled and nextPage and time>=nextSend and capacity() then
        local panel=getPlayerLoot(0);local selected=panel and panel:getIsVisible() and not panel.isCollapsed and panel.inventoryPane and panel.inventoryPane.inventory
        if not selected and models() then selected=active end
        if selected~=active then active=nil;cache=nil;assembly=nil;pending={};nextPage=nil;publish();return end
        local page=nextPage;nextPage=nil;nextSend=time+250;snapshot(page.container,page.offset);return
    end
    if not enabled or time<nextRefresh then return end
    nextRefresh=time+500
    local p=getSpecificPlayer(0);local panel=p and getPlayerLoot(0)
    local container=panel and panel:getIsVisible() and not panel.isCollapsed and panel.inventoryPane and panel.inventoryPane.inventory
    if not container and models() then container=active end
    local parent=container and container:getParent()
    if not container or not container:isExplored() or not M.near(p,parent and parent:getSquare()) or not M.locator(container) then
        active=nil;cache=nil;assembly=nil;pending={};nextPage=nil;publish();return
    end
    if active~=container then active=container;cache=nil;assembly=nil;pending={};nextPage=nil;publish() end
    local count=0;for _ in pairs(pending) do count=count+1 end
    if count>0 then return end
    if isClient() then snapshot(container,0)
    else
        local storage=M.storage(container)
        if not storage then return end
        local rows,changed=M.layout(container,storage.key)
        if rows then
            if changed>0 then storage.rev=math.min(2147483646,storage.rev+1) end
            cache={key=storage.key,rev=storage.rev,baseRev=storage.rev,total=#rows,rows={},count=#rows,deltaRevs={}}
            for _,r in ipairs(rows) do cache.rows[r.id]=r end
            publish()
        end
    end
end
local page=PZAPI.ModOptions:create('PZVRPlacement','PZVR storage placement')
local option=page:addTickBox('persistentLayout','Persistent storage placements (experimental)',false,
    'Stores stable per-item positions for opened native furniture inventories. Multiplayer uses server validation and paged snapshots. This metadata adapter does not yet draw or articulate containers.')
modelOption=page:addTickBox('storageModels','Native storage item models (experimental)',false,
    'Draws original native item models within nearby opened storage using original furniture bounds. At most 64 nearby items render at once. Enclosed cupboards and wardrobes still need articulated opening; oversized or missing models remain available through native inventory.')
physicsOption=page:addTickBox('storagePhysics','Native storage collision physics (experimental)',false,
    'Uses captured original item meshes and housing triangles in the Rust solver. Saved placements load asleep; new contents and returned items can settle. Unsupported CPU geometry stays in native inventory. Single-player only; articulation and automatic world spills remain unfinished.')
function page:apply()
    enabled=option:getValue() or models();active=nil;cache=nil;assembly=nil;pending={};nextPage=nil;nextRefresh=0;publish()
    if PZVRPhysicality then print('[PZVR Storage] '..PZVRPhysicality.storagePhysicsConfigure(physicsOption:getValue())) end
end
Events.OnGameStart.Add(function() page:apply() end)
Events.OnTick.Add(function()
    local ok,error=pcall(refresh)
    if ok and PZVRPhysicality and physicsOption and physicsOption:getValue() then PZVRPhysicality.storagePhysicsTick() end
    if not ok then enabled=false;active=nil;cache=nil;assembly=nil;pending={};nextPage=nil;pcall(publish);print('[PZVR Placement] Disabled: '..tostring(error)) end
end)
Events.OnServerCommand.Add(function(...)
    local ok,error=pcall(receive,...)
    if not ok then enabled=false;active=nil;cache=nil;assembly=nil;pending={};nextPage=nil;publish();print('[PZVR Placement] View disabled: '..tostring(error)) end
end)
Events.OnMainMenuEnter.Add(function() enabled=false;active=nil;cache=nil;assembly=nil;pending={};nextPage=nil;publish();if PZVRPhysicality then PZVRPhysicality.storagePhysicsConfigure(false) end end)
