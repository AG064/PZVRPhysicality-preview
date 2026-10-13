require 'TimedActions/ISTimedActionQueue'
require 'TimedActions/ISInventoryTransferAction'
require 'TimedActions/ISUnequipAction'
require 'TimedActions/ISEquipWeaponAction'
require 'PZAPI/ModOptions'

local enabled,reach
local function hand(p,side) if side==0 then return p:getSecondaryHandItem() end return p:getPrimaryHandItem() end
local function capture(...) return {n=select('#',...),...} end
local function tick()
    local api=PZVRPhysicality;if not api then return end
    api.backpackTick()
    local ticket=api.backpackTicket();if not ticket or ticket<=0 then return end
    local p,bag,item,side=api.backpackPlayer(),api.backpackBag(),api.backpackItem(),api.backpackSide()
    local epoch,kind=api.backpackGeneration(),api.backpackAction()
    local queued=false;local actions={}
    local ok,err=pcall(function()
        if not p or p~=getSpecificPlayer(0) or p:isDead() or p:getVehicle() or p:hasTimedActions() or isClient() or isServer()
            or not bag or not item or (side~=0 and side~=1) or (kind<1 or kind>4)
            or bag:getContainer()~=p:getInventory() or not p:getInventory():contains(bag)
            or bag:getWorldItem() or bag:canBeEquipped()~=ItemBodyLocation.BACK or p:getClothingItem_Back()==bag
            or (p:getClothingItem_Back() and p:getClothingItem_Back():hasTag(ItemTag.REPLACE_PRIMARY))
            or (kind<=2 and (item==bag or instanceof(item,'InventoryContainer'))) or (kind>=3 and item~=bag)
            or item:isForceDropHeavyItem() or item:isRequiresEquippedBothHands() or item:hasTag(ItemTag.REPLACE_PRIMARY)
            or not api.backpackValid(p,bag,item,side,ticket,epoch,0) then return end
        local inventory=p:getInventory();local contents=bag:getItemContainer();local expectedOther=hand(p,1-side)
        if not contents or contents:getContainingItem()~=bag then return end
        local function valid(action,phase)
            return action.character==p and action.item==item and p==getSpecificPlayer(0) and not isClient() and not isServer()
                and not p:isDead() and not p:getVehicle() and hand(p,1-side)==expectedOther and not item:getWorldItem()
                and api.backpackValid(p,bag,item,side,ticket,epoch,phase)
        end
        local function receipt(final)
            if not final then return item:getContainer()==inventory and inventory:contains(item) and not hand(p,side) end
            if kind==1 or kind==4 then return item:getContainer()==inventory and inventory:contains(item) and hand(p,side)==item end
            if kind==3 then return item:getContainer()==inventory and inventory:contains(item) and hand(p,0)~=item and hand(p,1)~=item end
            return item:getContainer()==contents and contents:contains(item) and not hand(p,side)
        end
        local function guard(action,phase,final,transfer)
            if not action or type(action.isValid)~='function' or type(action.perform)~='function' then error('Native backpack action unavailable') end
            action.pzvrBackpackTicket=ticket
            local originalValid,originalPerform,originalComplete,originalStop=action.isValid,action.perform,action.complete,action.stop
            local committed,completed,performed=false,false,false
            action.setOverrideHandModels=function() end
            action.isValid=function(self,...)
                if committed then return self.character==p and self.item==item and p==getSpecificPlayer(0) and not isClient() and not isServer() and hand(p,1-side)==expectedOther and receipt(final) end
                if not valid(self,phase) then return false end
                local allowed=originalValid(self,...)
                return allowed and self.item==item and valid(self,phase)
            end
            if transfer then
                if type(action.transferItem)~='function' or type(action.setOnComplete)~='function' then error('Native transfer contract unavailable') end
                local originalTransfer=action.transferItem
                local source,destination=kind==1 and contents or inventory,kind==1 and inventory or contents
                -- The native callback API prevents queue merging with unrelated manual transfers.
                action:setOnComplete(function() end)
                action.transferItem=function(self,moved,...)
                    if committed then return end
                    if moved~=item or self.srcContainer~=source or self.destContainer~=destination or not self:isValid() then self:forceStop();return end
                    local result=capture(originalTransfer(self,moved,...))
                    committed=receipt(final)
                    if final then api.backpackFinished(ticket,committed) end
                    return unpack(result,1,result.n)
                end
            elseif type(originalComplete)=='function' then
                action.complete=function(self,...)
                    if completed then return false end
                    if not self:isValid() then return false end
                    completed=true
                    local result=capture(originalComplete(self,...))
                    committed=result[1]~=false and receipt(final)
                    if final then api.backpackFinished(ticket,committed) end
                    return unpack(result,1,result.n)
                end
            else error('Native equip completion unavailable') end
            action.perform=function(self,...)
                if performed then return end
                if not self:isValid() then self:forceStop();return end
                performed=true
                return originalPerform(self,...)
            end
            local event=action.animEvent
            if type(event)=='function' then action.animEvent=function(self,...) if completed then return end;if not self:isValid() then self:forceStop();return end;return event(self,...) end end
            action.stop=function(self,...) api.backpackFinished(ticket,false);if type(originalStop)=='function' then return originalStop(self,...) end end
        end
        if kind==1 then
            if hand(p,side) or item:getContainer()~=contents or not contents:contains(item) then return end
            actions[1]=ISInventoryTransferAction:new(p,item,contents,inventory)
            guard(actions[1],0,false,true)
            actions[2]=ISEquipWeaponAction:new(p,item,50,side==1,false)
            guard(actions[2],1,true,false)
        elseif kind==2 then
            if hand(p,side)~=item or item:getContainer()~=inventory or not inventory:contains(item) or not contents:hasRoomFor(p,item) then return end
            actions[1]=ISUnequipAction:new(p,item,50,'drop')
            guard(actions[1],0,false,false)
            actions[2]=ISInventoryTransferAction:new(p,item,inventory,contents)
            guard(actions[2],1,true,true)
        elseif kind==3 then
            if hand(p,side)~=bag then return end
            actions[1]=ISUnequipAction:new(p,bag,50,'drop')
            guard(actions[1],0,true,false)
        elseif kind==4 then
            if hand(p,side) or hand(p,0)==bag or hand(p,1)==bag then return end
            actions[1]=ISEquipWeaponAction:new(p,bag,50,side==1,false)
            guard(actions[1],0,true,false)
        end
        if not actions[1]:isValid() or not api.backpackAcknowledge(ticket,true) then return end
        for _,action in ipairs(actions) do ISTimedActionQueue.add(action) end
        queued=true
    end)
    if not queued then api.backpackAcknowledge(ticket,false);api.backpackFinished(ticket,false) end
    if not ok then print('[PZVR Backpack] Native inventory retained: '..tostring(err)) end
end

local function apply() if PZVRPhysicality then print('[PZVR Backpack] '..PZVRPhysicality.backpackConfigure(enabled:getValue(),reach:getValue())) end end
local page=PZAPI.ModOptions:create('PZVRPhysicalityBackpack','PZVR Backpack Contents')
enabled=page:addTickBox('enabled','Physical backpack contents and suspension',false,'Single-player: hold one native back-slot bag forward. Up to eight real small item models appear above it. Grip a model for original transfer/equip; release into an empty cell to return it. A short released bag-hand trigger pages contents. Hold that trigger at least 0.7 seconds, then release grip to native-unequip and suspend the same bag at its captured position. Grip the suspended original bag mesh to native-equip it again. Suspension uses its original world model and expires on focus, actor, ownership or recenter changes, retaining native inventory. Missing or large contents use native inventory. This preview does not open the bag mesh or simulate cloth.')
reach=page:addSlider('reachCm','Item contact distance (cm)',3,12,.5,7.5,'Contact requires a freshly captured original rigid item surface.')
function page:apply() apply() end
page:addButton('previous','Previous contents page','Uses the same stable native contents pages as the bag-hand trigger.',function() if PZVRPhysicality then PZVRPhysicality.backpackPage(-1) end end)
page:addButton('next','Next contents page','A spare empty page permits spatial returns when a page is full.',function() if PZVRPhysicality then PZVRPhysicality.backpackPage(1) end end)
page:addButton('status','Print contents status','Reports pages, preview availability and native inventory fallback.',function() if PZVRPhysicality then print('[PZVR Backpack] '..PZVRPhysicality.backpackStatus()) end end)
Events.OnTick.Add(tick)
Events.OnGameStart.Add(apply)
Events.OnMainMenuEnter.Add(function() if PZVRPhysicality then PZVRPhysicality.backpackConfigure(false,7.5) end end)
