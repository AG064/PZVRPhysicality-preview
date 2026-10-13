require 'Hotbar/ISHotbar'
require 'TimedActions/ISTimedActionQueue'
require 'TimedActions/ISUnequipAction'
require 'TimedActions/ISEquipWeaponAction'
require 'TimedActions/ISWearClothing'
require 'TimedActions/ISAttachItemHotbar'
require 'PZAPI/ModOptions'

local page=PZAPI.ModOptions:create('PZVRPhysicalityBody','PZVR Backpack and Body Storage')
local enabled=page:addTickBox('enabled','Physical backpack and native hotbar storage',false,
    'Grab the actual worn backpack over a shoulder. Release it near the shoulder to wear it again. Grab native hotbar gear near its attachment; release a compatible held item near an empty slot to attach it. Original game actions and slot rules apply. Native pistol support and two-hand or heavy items retain normal controls. Backpack contents still use the native inventory screen.')
local radius=page:addSlider('reachCm','Body contact distance (cm)',6,22,1,16,'Contact tolerance in physical metres. This does not change world scale.')
local labels={'Left hip','Right hip','Back and shoulder'}
local names={'left','right','back'}
local defaults={{-22,0,-80},{22,0,-80},{10,-16,-25}}
local anchors={}
for zone=1,3 do
    anchors[zone]={}
    for axis=1,3 do
        local name=({'Side','Forward','Below headset'})[axis]
        anchors[zone][axis]=page:addSlider(names[zone]..axis,labels[zone]..' '..name..' (cm)',axis==3 and -140 or -60,axis==3 and -5 or 60,1,defaults[zone][axis],
            'Adjust the inferred torso anchor. Occupied hotbar slots use a fresh native attachment position when available. Head orientation estimates the torso; independent waist tracking is unavailable.')
    end
end
local calibrationHand=page:addComboBox('calibrationHand','Calibration hand','Choose the physical controller used by the calibration buttons.')
calibrationHand:addItem('Left',true);calibrationHand:addItem('Right',false)
local function apply()
    if not PZVRPhysicality then return end
    for zone=1,3 do PZVRPhysicality.bodyAnchor(zone-1,anchors[zone][1]:getValue()/100,anchors[zone][2]:getValue()/100,anchors[zone][3]:getValue()/100) end
    print('[PZVR Body] '..PZVRPhysicality.bodyConfigure(enabled:getValue(),radius:getValue()))
end
function page:apply() apply() end
for zone=1,3 do
    local selected=zone
    page:addButton('calibrate'..zone,'Calibrate '..labels[zone],'Hold the chosen controller at the desired position, then use this button. Saved through native Mod Options.',function()
        if not PZVRPhysicality then return end
        -- Mod Options owns the HUD while this button runs. Calibration reads a fresh tracked rig without acquiring gameplay input.
        local values={};for axis=1,3 do values[axis]=PZVRPhysicality.bodyCalibration(calibrationHand:getValue()-1,axis-1)*100 end
        for axis=1,3 do local v=values[axis];if v~=v or v<(axis==3 and -140 or -60) or v>(axis==3 and -5 or 60) then print('[PZVR Body] Calibration unavailable; keep the tracked controller inside the supported torso range.');return end end
        for axis=1,3 do anchors[selected][axis]:setValue(math.floor(values[axis]+.5)) end
        apply();PZAPI.ModOptions:save()
    end)
end
page:addButton('readings','Print body storage status','Reads the current native body bridge state.',function() if PZVRPhysicality then print('[PZVR Body] '..PZVRPhysicality.bodyStatus()) end end)

local slots={}
local function hand(p,side) if side==0 then return p:getSecondaryHandItem() end return p:getPrimaryHandItem() end
local function owns(p,item) return item and item:getContainer()==p:getInventory() and p:getInventory():contains(item) and not item:getWorldItem() end
local function location(value,item)
    if not value or not item or not value.definition.def then return nil end
    local target=value.definition.def.attachments[item:getAttachmentType()]
    if value.definition.def.name=='Back' and value.bar.replacements and value.bar.replacements[item:getAttachmentType()] then target=value.bar.replacements[item:getAttachmentType()] end
    if target=='null' or type(target)~='string' or #target>96 then return nil end
    return target
end
local function accepts(p,value,item)
    local target=location(value,item)
    return owns(p,item) and not item:isBroken() and item:getAttachedSlot()==-1 and target and not p:getAttachedItem(target) and value.bar:canBeAttached(value.definition,item)
end
local function current(p,value)
    local bar=getPlayerHotbar(p:getPlayerNum())
    local definition=bar and bar.availableSlot[value.index]
    return bar==value.bar and definition and definition.slotType==value.definition.slotType and definition.def==value.definition.def
end
local function refresh(p)
    slots={};PZVRPhysicality.bodyBegin(p)
    if not p or p~=getSpecificPlayer(0) or isClient() or isServer() then return end
    local bar=getPlayerHotbar(p:getPlayerNum());if not bar then return end
    if bar.needsRefresh then bar:refresh() end
    local count=0
    for index,definition in ipairs(bar.availableSlot) do
        count=count+1;if count>24 then break end
        local name=string.lower(definition.slotType or '')
        local zone=string.find(name,'left',1,true) and 0 or string.find(name,'right',1,true) and 1 or name=='back' and 2 or nil
        if zone and definition.def then
            local id='hotbar:'..index..':'..definition.slotType
            local value={id=id,index=index,definition=definition,bar=bar,item=bar.attachedItems[index]}
            if value.item and (hand(p,0)==value.item or hand(p,1)==value.item) then value.item=nil end
            slots[id]=value
            PZVRPhysicality.bodySlot(id,zone,value.item,value.item and location(value,value.item) or nil,not value.item and accepts(p,value,hand(p,0)) or false,not value.item and accepts(p,value,hand(p,1)) or false)
        end
    end
end
local function capture(...) return {n=select('#',...),...} end
local function guard(action,p,item,side,ticket,epoch,condition,last)
    action.pzvrBodyTicket=ticket
    local valid=action.isValid
    local committed=false
    local completed=false
    local expectedOther=hand(p,1-side)
    local function goal() return last==1 and hand(p,side)==item or last==2 and (p:getClothingItem_Back()==item or item:getAttachedSlot()~=-1 and p:getAttachedItem(item:getAttachedToModel())==item) end
    action.isValid=function(self,...) return self.item==item and p==getSpecificPlayer(0) and owns(p,item) and not isClient() and not isServer() and not p:isDead() and not p:getVehicle() and hand(p,1-side)==expectedOther and not item:isForceDropHeavyItem() and not item:isRequiresEquippedBothHands() and not item:hasTag(ItemTag.REPLACE_PRIMARY) and PZVRPhysicality.bodyValid(p,item,side,ticket,epoch) and condition() and valid(self,...) end
    -- Per-action previews are cosmetic. The tracked native slots remain visible throughout the transition.
    action.setOverrideHandModels=function() end
    local complete=action.complete
    if type(complete)=='function' then action.complete=function(self,...)
        if completed or (not committed and not self:isValid()) or self.item~=item or p~=getSpecificPlayer(0) or not owns(p,item) then return false end
        completed=true
        local result=capture(complete(self,...))
        -- A native true return is not a commit receipt. Attach.complete is intentionally a no-op.
        committed=result[1]~=false and (last and goal() or not last and hand(p,0)~=item and hand(p,1)~=item and p:getClothingItem_Back()~=item)
        if last and (result[1]==false or goal()) then PZVRPhysicality.bodyFinished(ticket,committed and last==1) end
        return unpack(result,1,result.n)
    end end
    local event=action.animEvent
    if type(event)=='function' then action.animEvent=function(self,...) if not self:isValid() then self:forceStop();return end return event(self,...) end end
    local perform=action.perform
    if type(perform)=='function' then action.perform=function(self,...)
        -- Some native actions commit in perform, while others commit in complete.
        if self.item~=item or p~=getSpecificPlayer(0) or not owns(p,item) or isClient() or isServer() or (not committed and not self:isValid()) then self:forceStop();return end
        local result=capture(perform(self,...))
        if last and goal() then committed=true;PZVRPhysicality.bodyFinished(ticket,last==1) end
        return unpack(result,1,result.n)
    end end
    local stop=action.stop
    if type(stop)=='function' then action.stop=function(self,...) local result=capture(stop(self,...));PZVRPhysicality.bodyFinished(ticket,false);return unpack(result,1,result.n) end end
end
local function tick()
    if not PZVRPhysicality then return end
    local p=getSpecificPlayer(0);refresh(p);PZVRPhysicality.bodyTick()
    local kind=PZVRPhysicality.bodyAction();if kind==0 then return end
    p=PZVRPhysicality.bodyPlayer();local item,side=PZVRPhysicality.bodyItem(),PZVRPhysicality.bodySide()
    local ticket,epoch=PZVRPhysicality.bodyTicket(),PZVRPhysicality.bodyGeneration()
    local queued=false
    local ok,err=pcall(function()
        if not p or p~=getSpecificPlayer(0) or p:isDead() or p:getVehicle() or p:hasTimedActions() or isClient() or isServer() or not owns(p,item) or (side~=0 and side~=1) or not PZVRPhysicality.bodyValid(p,item,side,ticket,epoch) then return end
        local other=hand(p,1-side)
        if item:isForceDropHeavyItem() or item:isRequiresEquippedBothHands() or item:hasTag(ItemTag.REPLACE_PRIMARY) or other and (other:isForceDropHeavyItem() or other:isRequiresEquippedBothHands()) then return end
        local actions={};local final
        if kind==1 or kind==2 then
            if not instanceof(item,'InventoryContainer') or item:canBeEquipped()~=ItemBodyLocation.BACK then return end
            if kind==1 then
                if p:getClothingItem_Back()~=item or hand(p,side) then return end
                local remove=ISUnequipAction:new(p,item,50,'drop');guard(remove,p,item,side,ticket,epoch,function() return p:getClothingItem_Back()==item and not hand(p,side) end);actions[1]=remove
                final=ISEquipWeaponAction:new(p,item,50,side==1,false);guard(final,p,item,side,ticket,epoch,function() return not hand(p,side) and not p:getClothingItem_Back() end,1)
            else
                if hand(p,side)~=item or p:getClothingItem_Back() then return end
                local remove=ISUnequipAction:new(p,item,50,'drop');guard(remove,p,item,side,ticket,epoch,function() return hand(p,side)==item and not p:getClothingItem_Back() end);actions[1]=remove
                final=ISWearClothing:new(p,item);guard(final,p,item,side,ticket,epoch,function() return not p:getClothingItem_Back() and not hand(p,side) end,2)
            end
        elseif kind==3 or kind==4 then
            local value=slots[PZVRPhysicality.bodySlotId()];if not value or not current(p,value) then return end
            local target=location(value,item);if not target then return end
            if kind==3 then
                if value.bar.attachedItems[value.index]~=item or hand(p,side) or hand(p,1-side)==item then return end
                final=ISEquipWeaponAction:new(p,item,20,side==1,false)
                guard(final,p,item,side,ticket,epoch,function() return current(p,value) and value.bar.attachedItems[value.index]==item and not hand(p,side) and hand(p,1-side)==other end,1)
            else
                if hand(p,side)~=item or value.bar.attachedItems[value.index] or not accepts(p,value,item) then return end
                local remove=ISUnequipAction:new(p,item,20,'drop');guard(remove,p,item,side,ticket,epoch,function() return hand(p,side)==item and current(p,value) and not value.bar.attachedItems[value.index] end);actions[1]=remove
                final=ISAttachItemHotbar:new(p,item,target,value.index,value.definition.def)
                guard(final,p,item,side,ticket,epoch,function() return current(p,value) and not value.bar.attachedItems[value.index] and hand(p,side)~=item and hand(p,1-side)==other and (not p:getAttachedItem(target) or p:getAttachedItem(target)==item) and value.bar:canBeAttached(value.definition,item) end,2)
            end
        else return end
        actions[#actions+1]=final
        -- Later actions require the earlier native unequip to commit before their validity can succeed.
        if not actions[1]:isValid() or not PZVRPhysicality.bodyAcknowledge(ticket,true) then return end
        for _,action in ipairs(actions) do ISTimedActionQueue.add(action) end
        queued=true
    end)
    if not queued then PZVRPhysicality.bodyAcknowledge(ticket,false);PZVRPhysicality.bodyFinished(ticket,false) end
    if not ok then print('[PZVR Body] Native action retained: '..tostring(err)) end
end
Events.OnTick.Add(tick)
Events.OnGameStart.Add(apply)
Events.OnMainMenuEnter.Add(function() if PZVRPhysicality then PZVRPhysicality.bodyConfigure(false,16) end end)
