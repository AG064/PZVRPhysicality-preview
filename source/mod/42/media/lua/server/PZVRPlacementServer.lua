require 'PZVRPlacement'
if not isServer() then return end
local M=PZVRPlacement
local clients=setmetatable({},{__mode='k'})
local lastSweep=0
local function now() return getTimestampMs() end
local function sweep(time)
    if time>=lastSweep and time-lastSweep<1000 then return end
    lastSweep=time
    for player,state in pairs(clients) do
        if time<state.time or time-state.time>60000 then clients[player]=nil
        elseif state.watch and time-state.watch.time>10000 then state.watch=nil end
    end
end
local function admit(player,time)
    sweep(time)
    local state=clients[player]
    -- Admission must retire future leases even when the bounded sweep interval has not elapsed.
    if state and (time<state.time or state.watch and time<state.watch.time) then clients[player]=nil;state=nil end
    if not state then
        local count=0;for _ in pairs(clients) do count=count+1 end
        if count>=64 then return nil end
        state={tokens=16,time=time};clients[player]=state
    end
    local elapsed=math.max(0,time-state.time)
    state.tokens=math.min(16,state.tokens+elapsed*.008);state.time=time
    if state.tokens<1 then return nil end
    state.tokens=state.tokens-1
    return state
end
local function resolve(player,args)
    if type(args)~='table' or args.v~=M.version or not M.integer(args.request,1,2147483647)
        or not M.integer(args.x,0,100000) or not M.integer(args.y,0,100000)
        or not M.integer(args.z,-32,32) or not M.integer(args.object,0,255)
        or not M.integer(args.container,0,31) then return nil end
    local square=getCell():getGridSquare(args.x,args.y,args.z)
    if not M.near(player,square) then return nil end
    local objects=square:getObjects()
    if args.object>=objects:size() then return nil end
    local parent=objects:get(args.object)
    local container=parent and parent:getContainerByIndex(args.container)
    if not container or container:getParent()~=parent or container:getContainingItem()
        or not container:isExplored() then return nil end
    return container,parent
end
local function reply(player,command,args) sendServerCommand(player,M.module,command,args) end
local function command(module,operation,player,args)
    if module~=M.module or (operation~='snapshot' and operation~='place') or not player then return end
    local time=now();local state=admit(player,time)
    if not state then return end
    local container,parent=resolve(player,args)
    if not container then return end
    local storage=M.storage(container)
    if not storage then return end
    if operation=='snapshot' then
        if not M.integer(args.offset,0,M.maxItems) then return end
        local watch=state.watch
        if args.offset==0 or not watch or watch.container~=container or watch.key~=storage.key
            or not watch.rows or time-watch.time>10000 then
            local rows,changed=M.layout(container,storage.key)
            if not rows then return end
            if changed>0 then storage.rev=math.min(2147483646,storage.rev+1) end
            watch={container=container,key=storage.key,time=time,rows=rows,rev=storage.rev};state.watch=watch
        end
        watch.time=time;local rows=watch.rows
        reply(player,'snapshot',{v=M.version,request=args.request,key=storage.key,rev=watch.rev,
            offset=args.offset,total=#rows,rows=M.page(rows,args.offset)})
    else
        -- The sender is supplied by the engine, never by a payload player ID.
        -- Placement follows a completed native transfer; this protocol never creates or moves inventory.
        if args.key~=storage.key or not state.watch or state.watch.container~=container
            or state.watch.key~=storage.key or time-state.watch.time>10000 then return end
        local row,why=M.edit(container,storage,args.id,args.expected,args.pose)
        if not row then reply(player,'rejected',{v=M.version,request=args.request,key=storage.key,reason=why});return end
        reply(player,'placed',{v=M.version,request=args.request,key=storage.key,rev=storage.rev,row=row})
        for other,observer in pairs(clients) do
            local watch=observer.watch
            if other~=player and watch and watch.container==container and watch.key==storage.key
                and time>=watch.time and time-watch.time<=10000 and M.near(other,parent:getSquare()) then
                reply(other,'changed',{v=M.version,key=storage.key,rev=storage.rev,row=row})
            end
        end
    end
end
Events.OnClientCommand.Add(function(...)
    local ok,error=pcall(command,...)
    if not ok then print('[PZVR Placement] Command failed: '..tostring(error)) end
end)
Events.OnTick.Add(function() sweep(now()) end)
