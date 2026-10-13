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
    local queued=false
    local ok,err=pcall(function()
        if not p or p~=getSpecificPlayer(0) or p:isDead() or p:getVehicle() or p:hasTimedActions()
            or isClient() or isServer() or not item or not square or (side~=0 and side~=1)
            or not p:getSquare() or square:isBlockedTo(p:getSquare()) or not square:isAdjacentTo(p:getSquare()) then return end
        if kind==1 then
            if hand(p,side) or not source or source:getSquare()~=square or source:getItem()~=item then return end
            local action=ISGrabItemAction:new(p,source,10)
            if not action:isValid() or type(action.transferItem)~='function' then return end
            local original=action.transferItem
            local done=false
            action.transferItem=function(self,world,...)
                local function capture(...) return {n=select('#',...),...} end
                local result=capture(original(self,world,...))
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
            if not action:isValid() or type(action.transferItem)~='function' then return end
            local original=action.transferItem;local done=false
            action.transferItem=function(self,transferred,...)
                local function capture(...) return {n=select('#',...),...} end
                local result=capture(original(self,transferred,...))
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
            if not action:isValid() or type(action.transferItem)~='function' then return end
            local original=action.transferItem;local done=false
            action.transferItem=function(self,transferred,...)
                local function capture(...) return {n=select('#',...),...} end
                local result=capture(original(self,transferred,...))
                if transferred==item and not done then
                    done=true
                    local ok,error=pcall(function()
                        if p==getSpecificPlayer(0) and self.character==p and not isClient() and not isServer()
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
            if not unequip:isValid() then return end
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
            local original=action.complete
            local done=false
            action.complete=function(self,...)
                local function capture(...) return {n=select('#',...),...} end
                local result=capture(original(self,...))
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
            if not unequip:isValid() then return end
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
