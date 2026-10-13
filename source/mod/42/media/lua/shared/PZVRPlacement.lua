-- Placement metadata only. Native inventory, loot generation and transfers remain authoritative.
PZVRPlacement = PZVRPlacement or {}
local M = PZVRPlacement
M.module = 'PZVRPlacement'
M.version = 1
M.pageSize = 64
M.maxItems = 2048
M.itemKey = 'PZVRPlacementV1'
M.objectKey = 'PZVRStorageV1'

function M.number(v,lo,hi)
    return type(v)=='number' and v==v and v>=lo and v<=hi
end
function M.integer(v,lo,hi) return M.number(v,lo,hi) and v==math.floor(v) end
function M.pose(p)
    if type(p)~='table' then return nil end
    local low,high=0,1;if p.raw==true then low,high=-.05,1.05 end
    for _,key in ipairs({'x','y','z'}) do if not M.number(p[key],low,high) then return nil end end
    for _,key in ipairs({'qx','qy','qz','qw'}) do if not M.number(p[key],-1,1) then return nil end end
    local n=p.qx*p.qx+p.qy*p.qy+p.qz*p.qz+p.qw*p.qw
    if not M.number(n,.98,1.02) then return nil end
    n=math.sqrt(n)
    return {x=p.x,y=p.y,z=p.z,qx=p.qx/n,qy=p.qy/n,qz=p.qz/n,qw=p.qw/n,raw=p.raw==true}
end
function M.record(r,key,id)
    if not M.integer(id,-2147483648,2147483647) or type(r)~='table' or r.v~=M.version or r.key~=key or r.id~=id
        or not M.integer(r.rev,1,2147483646) or not M.integer(r.slot,0,M.maxItems-1) then return nil end
    local p=M.pose(r)
    if not p then return nil end
    p.v=M.version;p.key=key;p.id=id;p.rev=r.rev;p.slot=r.slot;p.auto=r.auto==true
    return p
end
function M.defaultPose(slot)
    -- Logical cells are stable. The model-specific interior adapter will map them to real bounds.
    return {x=(slot%8+.5)/8,y=(math.floor(slot/8)%8+.5)/8,
        z=(math.floor(slot/64)+.5)/32,qx=0,qy=0,qz=0,qw=1,auto=true}
end
function M.items(container)
    local list=container:getItems()
    if list:size()>M.maxItems then return nil,'item-budget' end
    local out,ids={},{}
    for n=0,list:size()-1 do
        local item=list:get(n);local id=item and item:getID()
        if not item or item:getContainer()~=container or not M.integer(id,-2147483648,2147483647)
            or ids[id] then return nil,'item-identity' end
        ids[id]=true;out[#out+1]=item
    end
    table.sort(out,function(a,b) return a:getID()<b:getID() end)
    return out
end
function M.layout(container,key)
    if type(key)~='string' or #key>128 or #key<1 then return nil,'container-key' end
    local items,why=M.items(container)
    if not items then return nil,why end
    local slots,rows,missing={},{},{}
    -- Reserve every existing cell first, so insertion order cannot move older items.
    for _,item in ipairs(items) do
        local r=M.record(item:getModData()[M.itemKey],key,item:getID())
        if r and not slots[r.slot] then slots[r.slot]=true;rows[#rows+1]=r
        else missing[#missing+1]={item=item,previous=r} end
    end
    local slot,changed=0,0
    for _,entry in ipairs(missing) do
        local item=entry.item
        while slots[slot] do slot=slot+1 end
        local r=entry.previous or M.defaultPose(slot)
        r.v=M.version;r.key=key;r.id=item:getID();r.rev=entry.previous and math.min(2147483646,r.rev+1) or 1;r.slot=slot
        item:getModData()[M.itemKey]=r;slots[slot]=true;rows[#rows+1]=M.record(r,key,r.id)
        slot=slot+1;changed=changed+1
    end
    table.sort(rows,function(a,b) return a.id<b.id end)
    return rows,changed
end
function M.page(rows,offset)
    if not M.integer(offset,0,M.maxItems) then return nil end
    local out={}
    for n=offset+1,math.min(#rows,offset+M.pageSize) do out[#out+1]=rows[n] end
    return out
end
function M.locator(container)
    local parent=container and container:getParent();local square=parent and parent:getSquare()
    if not square or container:getContainingItem() or parent:getObjectIndex()<0 then return nil end
    local index=parent:getContainerIndex(container)
    if not M.integer(index,0,31) then return nil end
    return {x=square:getX(),y=square:getY(),z=square:getZ(),object=parent:getObjectIndex(),container=index}
end
function M.near(player,square)
    local own=player and player:getSquare()
    return own and square and not player:isDead() and not player:getVehicle()
        and own:getZ()==square:getZ() and math.abs(player:getX()-square:getX()-.5)<=2
        and math.abs(player:getY()-square:getY()-.5)<=2
        and square:isAdjacentTo(own) and not square:isBlockedTo(own)
        and SafeHouse.isSafehouseAllowLoot(square,player)
end
function M.storage(container)
    local parent=container:getParent();local loc=M.locator(container)
    if not loc then return nil end
    local data=parent:getModData();local all=data[M.objectKey]
    if type(all)~='table' then all={};data[M.objectKey]=all end
    local index=tostring(loc.container);local storage=all[index]
    if type(storage)~='table' or storage.v~=M.version or type(storage.key)~='string'
        or #storage.key>128 or not M.integer(storage.rev,0,2147483646) then
        local counter=ModData.getOrCreate('PZVRPlacementCountersV1')
        if counter.next==nil then counter.next=0 end
        if not M.integer(counter.next,0,9007199254740000) then return nil end
        counter.next=counter.next+1
        storage={v=M.version,key=tostring(loc.x)..':'..tostring(loc.y)..':'..tostring(loc.z)..':'..tostring(counter.next),rev=0}
        all[index]=storage
    end
    return storage
end
function M.edit(container,storage,id,expected,pose)
    local p=M.pose(pose)
    if not p or not M.integer(id,-2147483648,2147483647) or not M.integer(expected,1,2147483645)
        or storage.rev>=2147483646 then return nil,'invalid-edit' end
    local items,why=M.items(container)
    if not items then return nil,why end
    for _,item in ipairs(items) do if item:getID()==id then
        local old=M.record(item:getModData()[M.itemKey],storage.key,id)
        if not old or old.rev~=expected then return nil,'conflict' end
        p.v=M.version;p.key=storage.key;p.id=id;p.rev=old.rev+1;p.slot=old.slot;p.auto=false
        item:getModData()[M.itemKey]=p;storage.rev=storage.rev+1
        return M.record(p,storage.key,id)
    end end
    return nil,'not-in-container'
end
function M.stow(container,item,key,pose)
    if isClient() or isServer() or not container or not item or item:getContainer()~=container
        or item:getWorldItem() or not container:contains(item) or not M.pose(pose) then return nil end
    local storage=M.storage(container)
    if not storage or storage.key~=key then return nil end
    local rows=M.layout(container,key)
    if not rows then return nil end
    local old=M.record(item:getModData()[M.itemKey],key,item:getID())
    if not old then return nil end
    return M.edit(container,storage,item:getID(),old.rev,pose)
end
return M
