require 'PZAPI/ModOptions'

local items={'Base.Spoon','Base.Fork','Base.Bread','Base.BreadSlices','Base.Apple','Base.Pear',
    'Base.Cheese','Base.Carrots','Base.SoupBowl','Base.SoupBowlClay','Base.StewBowl','Base.StewBowlClay','Base.WaterBottle',
    'Base.Bag_Schoolbag','Base.Belt2','Base.Hammer'}
local function actor()
    if isClient() or isServer() or not PZVRPhysicality or not PZVRPhysicality.ready() then return nil end
    local p=getSpecificPlayer(0)
    if not p or p:isDead() or p:getVehicle() or p:hasTimedActions() or not p:getSquare() then return nil end
    return p
end
local function supplies()
    local p=actor()
    if not p then print('[PZVR Presentation] Supplies require a ready single-player world and an idle living player.');return end
    local inventory=p:getInventory()
    if not inventory then return end
    local added,missing=0,0
    local ok,error=pcall(function()
        for _,id in ipairs(items) do
            if actor()~=p or p:getInventory()~=inventory then return end
            if not inventory:containsTypeRecurse(id) then
                local item=inventory:AddItem(id)
                if item and item:getFullType()==id and item:getContainer()==inventory and not item:getWorldItem() then added=added+1
                else missing=missing+1 end
            end
        end
    end)
    print('[PZVR Presentation] Added '..tostring(added)..' missing demo item types; '..tostring(missing)..' unavailable. Existing items and contents are retained.')
    if not ok then print('[PZVR Presentation] Native supplies stopped: '..tostring(error)) end
end
local function readings()
    if not PZVRPhysicality then return end
    for _,method in ipairs({'status','consumptionStatus','fingersStatus','storageStatus','tapsStatus','bodyStatus','curtainsStatus','craftingStatus','backpackStatus','contactsStatus','worldContactsStatus'}) do
        local value=PZVRPhysicality[method]
        if type(value)=='function' then local ok,result=pcall(value);if ok then print('[PZVR Presentation] '..tostring(result)) end end
    end
end
local page=PZAPI.ModOptions:create('PZVRPresentation','PZVR Presentation')
page:addButton('supplies','Prepare demo supplies',
    'Adds missing spoon, fork, food, soup/stew bowl, bottle, school backpack, belt and hammer types through the native inventory factory. Wear the backpack and belt through the normal inventory action. Use the isolated presentation save. Existing items are not replaced, filled, equipped or repaired. Single-player only.',supplies)
page:addButton('readings','Print feature status',
    'Writes current hands, consumption, item, tap, storage, body, curtain, crafting, backpack, held-contact and world-geometry status to the game console log.',readings)
