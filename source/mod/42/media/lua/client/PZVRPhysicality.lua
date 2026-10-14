require 'TimedActions/ISTimedActionQueue'
require 'TimedActions/ISGrabItemAction'
require 'TimedActions/ISDropWorldItemAction'
require 'TimedActions/ISUnequipAction'
require 'TimedActions/ISInventoryTransferAction'
require 'PZAPI/ModOptions'
require 'PZVRPlacement'

local option
local function hand(p,side)
    if PZVRPhysicality.primaryHand(side) then return p:getPrimaryHandItem() end
    return p:getSecondaryHandItem()
end
local function tick()
    if not PZVRPhysicality then return end
    PZVRPhysicality.tick()
    local kind=PZVRPhysicality.action()
    if kind==0 then return end
    local p,item,source,square=PZVRPhysicality.player(),PZVRPhysicality.item(),PZVRPhysicality.source(),PZVRPhysicality.square()
    local side,epoch=PZVRPhysicality.side(),PZVRPhysicality.generation()
    local ticket=type(PZVRPhysicality.actionTicket)=='function' and PZVRPhysicality.actionTicket() or 0
    local queued=false
    local ok,err=pcall(function()
        if not p or p~=getSpecificPlayer(0) or p:isDead() or p:getVehicle() or p:hasTimedActions()
            or isClient() or isServer() or not item or not square or (side~=0 and side~=1) then return end
        local currentSquare=p:getSquare()
        -- Loot reach admits solid furniture without permitting transfers through walls or doors.
        if not currentSquare or square~=currentSquare and (not square:isAdjacentTo(currentSquare)
            or not currentSquare:canReachTo(square)) then return end
        if type(ticket)~='number' or ticket<=0 or ticket~=ticket or type(PZVRPhysicality.actionAllowedRequest)~='function' then return end
        local function capture(...) return {n=select('#',...),...} end
        local function guard(action,committed)
            local originalValid=action.isValid
            if type(originalValid)~='function' then return nil end
            local admitted,attempted,completed,result=true,false,false,nil
            local function permit(self)
                if not admitted or self.character~=p or not PZVRPhysicality.actionAllowedRequest(p,side,epoch,ticket) then
                    admitted=false
                    return false
                end
                return true
            end
            action.isValid=function(self,...)
                if completed or committed and committed() then return originalValid(self,...) end
                local valid=permit(self) and originalValid(self,...)
                if not valid then admitted=false end
                return valid
            end
            local function before(self)
                if self:isValid() then return true end
                if type(self.forceStop)=='function' then self:forceStop() end
                return false
            end
            local complete,event=action.complete,action.animEvent
            if type(complete)=='function' then
                action.complete=function(self,...)
                    if attempted then if result then return unpack(result,1,result.n) end;return false end
                    if not before(self) then return false end
                    attempted=true
                    result=capture(complete(self,...))
                    completed=true
                    return unpack(result,1,result.n)
                end
            end
            if type(event)=='function' then action.animEvent=function(self,...)
                if completed then return end
                if not before(self) then return end
                return event(self,...)
            end end
            return before
        end
        if kind==1 then
            if hand(p,side) or not source or source:getSquare()~=square or source:getItem()~=item then return end
            local action=ISGrabItemAction:new(p,source,10)
            if not action:isValid() or type(action.transferItem)~='function' then return end
            local done,result=false,nil
            local permit=guard(action,function() return done end);if not permit then return end
            local original=action.transferItem
            action.transferItem=function(self,world,...)
                local function capture(...) return {n=select('#',...),...} end
                if done then return unpack(result,1,result.n) end
                if not permit(self) then return end
                result=capture(original(self,world,...))
                if world==source and not done then
                    done=true
                    local transferred,transferError=pcall(function()
                        if self.character==p and p==getSpecificPlayer(0) and not hand(p,side)
                            and p:getInventory():contains(item) and not item:getWorldItem()
                            and PZVRPhysicality.canReceive(p,item,side,epoch) then
                            if PZVRPhysicality.primaryHand(side) then p:setPrimaryHandItem(item) else p:setSecondaryHandItem(item) end
                            PZVRPhysicality.received(p,item,side,epoch)
                        end
                    end)
                    if not transferred then print('[PZVR Physicality] Native pickup retained: '..tostring(transferError)) end
                end
                return unpack(result,1,result.n)
            end
            ISTimedActionQueue.add(action);queued=true
        elseif kind==3 then
            local inventory=PZVRPhysicality.inventorySource()
            if hand(p,side) or not inventory or item:getWorldItem() or item:getContainer()~=inventory
                or not inventory:contains(item) or not inventory:getParent() or inventory:getParent():getSquare()~=square
                or inventory:getParent():getContainerIndex(inventory)<0 then return end
            local action=ISInventoryTransferAction:new(p,item,inventory,p:getInventory(),10)
            if not action:isValid() or type(action.transferItem)~='function' or type(action.setOnComplete)~='function' then return end
            action:setOnComplete(function() end)
            local done,result=false,nil
            local permit=guard(action,function() return done end);if not permit then return end
            local original=action.transferItem
            action.transferItem=function(self,transferred,...)
                local function capture(...) return {n=select('#',...),...} end
                if done then return unpack(result,1,result.n) end
                if not permit(self) then return end
                result=capture(original(self,transferred,...))
                if transferred==item and not done then
                    done=true
                    local ok,error=pcall(function()
                        if self.character==p and p==getSpecificPlayer(0) and not hand(p,side)
                            and p:getInventory():contains(item) and item:getContainer()==p:getInventory()
                            and not item:getWorldItem() and PZVRPhysicality.canReceive(p,item,side,epoch) then
                            if PZVRPhysicality.primaryHand(side) then p:setPrimaryHandItem(item) else p:setSecondaryHandItem(item) end
                            PZVRPhysicality.received(p,item,side,epoch)
                        end
                    end)
                    if not ok then print('[PZVR Physicality] Native storage pickup retained: '..tostring(error)) end
                end
                return unpack(result,1,result.n)
            end
            ISTimedActionQueue.add(action);queued=true
        elseif kind==4 then
            local destination=PZVRPhysicality.inventorySource()
            if not destination or not destination:getParent() or destination:getParent():getSquare()~=square
                or hand(p,side)~=item or item:getContainer()~=p:getInventory() or item:getWorldItem()
                or not p:getInventory():contains(item) then return end
            local key=PZVRPhysicality.storageKey()
            local vx,vy,vh=PZVRPhysicality.vx(),PZVRPhysicality.vy(),PZVRPhysicality.vh()
            local pose={x=PZVRPhysicality.storageX(),y=PZVRPhysicality.storageY(),z=PZVRPhysicality.storageZ(),
                qx=PZVRPhysicality.storageQx(),qy=PZVRPhysicality.storageQy(),qz=PZVRPhysicality.storageQz(),qw=PZVRPhysicality.storageQw(),raw=true}
            if not PZVRPlacement or not PZVRPlacement.pose(pose) then return end
            local action=ISInventoryTransferAction:new(p,item,p:getInventory(),destination,10)
            if not action:isValid() or type(action.transferItem)~='function' or type(action.setOnComplete)~='function' then return end
            action:setOnComplete(function() end)
            local done,result=false,nil
            local permit=guard(action,function() return done end);if not permit then return end
            local original=action.transferItem
            action.transferItem=function(self,transferred,...)
                local function capture(...) return {n=select('#',...),...} end
                if done then return unpack(result,1,result.n) end
                if not permit(self) then return end
                result=capture(original(self,transferred,...))
                if transferred==item and not done then
                    done=true
                    local ok,error=pcall(function()
                        if p==getSpecificPlayer(0) and self.character==p and not isClient() and not isServer()
                            and PZVRPhysicality.actionAllowedRequest(p,side,epoch,ticket)
                            and item:getContainer()==destination and destination:contains(item) and not item:getWorldItem() then
                            PZVRPlacement.stow(destination,item,key,pose)
                            PZVRPhysicality.storedReleased(p,item,side,epoch,destination,vx,vy,vh)
                        end
                    end)
                    if not ok then print('[PZVR Physicality] Native storage return retained: '..tostring(error)) end
                end
                return unpack(result,1,result.n)
            end
            local unequip=ISUnequipAction:new(p,item,1,'transfer')
            if not unequip:isValid() or not guard(unequip) then return end
            ISTimedActionQueue.add(unequip);ISTimedActionQueue.add(action);queued=true
        elseif kind==2 then
            if hand(p,side)~=item or not p:getInventory():contains(item) or item:getWorldItem() or not square:TreatAsSolidFloor() then return end
            local x,y,z=PZVRPhysicality.x(),PZVRPhysicality.y(),PZVRPhysicality.z()
            if x~=x or y~=y or z~=z or x<0 or x>=1 or y<0 or y>=1 or z<0 or z>.9 then return end
            local vx,vy,vh=PZVRPhysicality.vx(),PZVRPhysicality.vy(),PZVRPhysicality.vh()
            local qx,qy,qz,qw=PZVRPhysicality.qx(),PZVRPhysicality.qy(),PZVRPhysicality.qz(),PZVRPhysicality.qw()
            local action=ISDropWorldItemAction:new(p,item,square,x,y,z,0,false)
            action.isPlaceItem=true
            if not action:isValid() or type(action.complete)~='function' then return end
            local done,result=false,nil
            local permit=guard(action,function() return done end);if not permit then return end
            local original=action.complete
            action.complete=function(self,...)
                local function capture(...) return {n=select('#',...),...} end
                if done then return unpack(result,1,result.n) end
                if not permit(self) then return end
                result=capture(original(self,...))
                if not done and self.character==p and self.item==item then
                    done=true
                    local dropped,dropError=pcall(function()
                        if not isClient() and not isServer() and p==getSpecificPlayer(0)
                            and not p:getInventory():contains(item) and item:getWorldItem() then
                            PZVRPhysicality.droppedOriented(p,item,side,epoch,vx,vy,vh,qx,qy,qz,qw)
                        end
                    end)
                    if not dropped then print('[PZVR Physicality] Native drop retained: '..tostring(dropError)) end
                end
                return unpack(result,1,result.n)
            end
            local unequip=ISUnequipAction:new(p,item,1,'drop')
            if not unequip:isValid() or not guard(unequip) then return end
            ISTimedActionQueue.add(unequip);ISTimedActionQueue.add(action);queued=true
        end
    end)
    PZVRPhysicality.acknowledge(queued)
    if not ok then print('[PZVR Physicality] Native action skipped: '..tostring(err)) end
end

local function apply()
    if PZVRPhysicality then print('[PZVR Physicality] '..PZVRPhysicality.enable(option and option:getValue() or false)) end
end
local page=PZAPI.ModOptions:create('PZVRPhysicality','PZVR Physicality')
option=page:addTickBox('floorItems','Physical item grip pickup/drop (experimental)',false,'Touch a loose item or a freshly rendered item inside open storage and squeeze grip to use the native pickup/transfer action. Release to drop it with gravity. Storage models also need their separate option enabled. Sealed furniture and unavailable models cannot be grabbed through their exterior. Multiplayer physical grabbing remains unavailable.')
function page:apply() apply() end
Events.OnTick.Add(tick)
Events.OnGameStart.Add(apply)
Events.OnMainMenuEnter.Add(function() if PZVRPhysicality then PZVRPhysicality.enable(false) end end)
