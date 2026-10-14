require 'PZAPI/ModOptions'
require 'ISUI/ISWorldObjectContextMenu'
require 'TimedActions/ISWearClothing'
require 'TimedActions/ISBaseTimedAction'
require 'TimedActions/ISTimedActionQueue'
require 'PZVRPhysicalityGuide'

local items={'Base.Spoon','Base.Fork','Base.Bread','Base.BreadSlices','Base.Apple','Base.Pear',
    'Base.Cheese','Base.Carrots','Base.SoupBowl','Base.SoupBowlClay','Base.StewBowl','Base.StewBowlClay','Base.WaterBottle',
    'Base.Bag_Schoolbag','Base.Belt2','Base.Hammer'}
local page=PZAPI.ModOptions:create('PZVRPresentation','PZVR Presentation')
local showGuide=page:addTickBox('showGuideOnStart','Show controls guide at world start',false,
    'Shows the native controls and live status window once when a world starts. Reopen it with the button below.')
local autoBackpack=page:addTickBox('autoBackpack','Prepare one test backpack at presentation world start',false,
    'Single-player presentation saves only. One world-start attempt adds a missing schoolbag and queues native wear only if the back slot is empty. Existing worn bags are preserved. Applying a preset never grants an item.')
local indexedModels=page:addTickBox('indexedModels','Resolve declared indexed item models',false,
    'For an item whose original model getter returns no model, resolves only its exact declared icon/model pair. Unknown items and foreign model overrides retain native behavior.')
local heldDiagnostics=page:addTickBox('heldModelDiagnostics','Record actual held model placement',false,
    'Records bounded exact native model, attachment and final rendered placement receipts for comparing Native controls and physical modes. It does not change poses or items. This independent diagnostic stays as selected when changing gameplay presets.')
local gripDiagnostics=page:addTickBox('gripAttemptDiagnostics','Keep recent grip attempt reasons',false,
    'Retains eight read-only raw grip press snapshots with the guard, rig, logical hand slots and native request state. Reopen the guide after a failed closed-HUD pickup. This independent diagnostic never creates an input or changes an item.')
local moveableModels=page:addTickBox('moveableModels','Resolve exact ground moveable pack models',false,
    'Uses exact resident selected pack geometry with a matching current full-pose draw and original ownership. Unsupported, over-budget or stale output retains native fallback; no box proxy. Ordinary native held item rendering keeps priority.')
local nativeHeld=page:addTickBox('nativeHeldFallback','Native world-only held model fallback',false,
    'CordlessPhone only: uses its exact original successful FBX upload and current native Prop2/Prop1 pose when no native held row exists. Native weapon, food, preview and existing model rows retain priority. Contact requires the same fresh geometry and rendered transaction.')
local windowMotion=page:addTickBox('physicalWindowMotion','Window sash motion',false,
    'Uses measured supported original sash correspondence and matching progressed glass/contact geometry. Unknown model pairs, visibility or stale native output retain native behavior. This setting does not add generic wall thickness.')
local pending,started,epoch=nil,false,0
local function message(text)
    print('[PZVR Presentation] '..text)
    if PZVRPhysicalityGuide then PZVRPhysicalityGuide.message(text) end
end
local function actor(allowBusy)
    if isClient() or isServer() or not PZVRPhysicality or not PZVRPhysicality.ready() then return nil end
    local p=getSpecificPlayer(0)
    if not p or p:isDead() or p:getVehicle() or (not allowBusy and p:hasTimedActions()) or not p:getSquare() then return nil end
    return p
end
local function supplies()
    local p=actor(false)
    if not p then message('Supplies require a ready single-player world and an idle living player.');return end
    local inventory=p:getInventory()
    if not inventory then return end
    local added,missing=0,0
    local ok,error=pcall(function()
        for _,id in ipairs(items) do
            if actor(false)~=p or p:getInventory()~=inventory then return end
            if not inventory:containsTypeRecurse(id) then
                local item=inventory:AddItem(id)
                if item and item:getFullType()==id and item:getContainer()==inventory and not item:getWorldItem() then added=added+1
                else missing=missing+1 end
            end
        end
    end)
    message('Added '..tostring(added)..' missing demo item types; '..tostring(missing)..' unavailable. Existing items and contents are retained.')
    if not ok then message('Native supplies stopped: '..tostring(error)) end
end
local function sit()
    local p=actor(false)
    if not p then message('Sit requires a ready single-player world and an idle living player outside a vehicle.');return end
    local ok,result=pcall(function() if actor(false)~=p then return false end ISWorldObjectContextMenu.onSitOnGround(0);return true end)
    if not ok then message('Native sit unavailable: '..tostring(result))
    else message(result and 'Requested native Sit on ground. Use the game movement controls to stand.' or 'Sit cancelled because the player state changed.') end
end
local function schoolbag()
    local p=actor(false)
    if not p then message('Backpack setup requires a ready single-player world and an idle living player.');return end
    if pending then
        if pending.character==p and p:hasTimedActions() then message('Native backpack wear is already queued.');return end
        pending=nil
    end
    local inventory=p:getInventory()
    if not inventory then return end
    local ok,err=pcall(function()
        local bag=inventory:getFirstTypeRecurse('Base.Bag_Schoolbag')
        if not bag and not inventory:containsTypeRecurse('Base.Bag_Schoolbag') then bag=inventory:AddItem('Base.Bag_Schoolbag') end
        if actor(false)~=p or p:getInventory()~=inventory then message('Backpack setup stopped because the player changed.');return end
        if not bag or bag:getFullType()~='Base.Bag_Schoolbag' or bag:getContainer()~=inventory or bag:getWorldItem() or not inventory:contains(bag) or not bag:IsInventoryContainer() or bag:isBroken() or bag:canBeEquipped()~=ItemBodyLocation.BACK then
            message('Schoolbag must be an intact native bag in the main inventory. Move an existing nested bag there before retrying.');return
        end
        local worn=p:getWornItem(ItemBodyLocation.BACK)
        if worn then message(worn==bag and 'The test schoolbag is already worn.' or 'Existing worn backpack preserved. The test schoolbag remains in inventory.');return end
        local action=ISWearClothing:new(p,bag)
        if not action then message('Native wear action unavailable.');return end
        local generation=epoch
        local function sameWorld() return generation==epoch and actor(true)==p end
        local function owned()
            return sameWorld() and p:getInventory()==inventory and bag:getContainer()==inventory and not bag:getWorldItem() and inventory:contains(bag) and not bag:isBroken() and bag:canBeEquipped()==ItemBodyLocation.BACK
        end
        local function valid() return owned() and not p:getWornItem(ItemBodyLocation.BACK) end
        local function finish() if pending==action then pending=nil end end
        local nativeValid,nativeStart,nativeUpdate,nativeComplete,nativeStop,nativePerform=action.isValid,action.start,action.update,action.complete,action.stop,action.perform
        action.isValid=function(self) return valid() and nativeValid(self) end
        action.start=function(self) if not valid() then finish();self:forceStop();return end return nativeStart(self) end
        action.update=function(self) if not valid() then finish();self:forceStop();return end return nativeUpdate(self) end
        action.complete=function(self)
            if not valid() then finish();message('Native backpack wear cancelled: actor, ownership or worn slot changed.');return false end
            local completed,result=pcall(nativeComplete,self);finish()
            if not completed then error(result) end
            message(owned() and p:getWornItem(ItemBodyLocation.BACK)==bag and 'Test schoolbag worn through the native action.' or 'Native wear did not equip the test schoolbag.');return result
        end
        action.perform=function(self)
            if not sameWorld() then finish();return end
            local current=p:getWornItem(ItemBodyLocation.BACK)
            if not owned() or (current and current~=bag) then finish();return ISBaseTimedAction.perform(self) end
            local performed,result=pcall(nativePerform,self);finish();if not performed then error(result) end return result
        end
        action.stop=function(self) local stopped,result=pcall(nativeStop,self);finish();if not stopped then error(result) end return result end
        if not action:isValid() then message('Native wear validation rejected the schoolbag.');return end
        pending=action
        local queued,queue=pcall(ISTimedActionQueue.add,action)
        if not queued then finish();error(queue) end
        if owned() and p:getWornItem(ItemBodyLocation.BACK)==bag then finish();message('Test schoolbag worn through the native action.');return end
        local found=false
        if type(queue)=='table' and queue.character==p and type(queue.queue)=='table' then
            for _,entry in ipairs(queue.queue) do if entry==action then found=true;break end end
        end
        if not found then finish();message('Native queue did not accept the test schoolbag wear action. The original bag remains in inventory.');return end
        message('One native schoolbag wear action queued. Existing worn bags will be preserved.')
    end)
    if not ok then message('Native backpack setup stopped: '..tostring(err)) end
end
local function readings()
    if PZVRPhysicalityGuide then PZVRPhysicalityGuide.open() end
end
PZVRPresentationActions={supplies=supplies,sit=sit,schoolbag=schoolbag}
page:addButton('guide','Open controls and live status','Shows gestures, settings pages and current guard reasons in a native game window.',readings)
page:addButton('modelReceipt','Print held model receipt','Prints one current native item/model/attachment and rendered-placement receipt. Enable Record actual held model placement first; compare the same item in Native controls and Presentation.',function()
    if not PZVRPhysicality then message('Held model receipt unavailable: extension missing.');return end
    local ok,value=pcall(PZVRPhysicality.heldModelDiagnosticsStatus)
    message(ok and tostring(value):gsub('[\r\n]',' '):sub(1,4000) or ('Held model receipt unavailable: '..tostring(value)))
end)
page:addButton('clearGripAttempts','Clear grip attempt history','Clears the eight stored diagnostic snapshots. Opening this guide or a HUD leaves the current-world attempt history intact.',function()
    if PZVRPhysicality then PZVRPhysicality.gripAttemptsReset();message('Grip attempt history cleared.') end
end)
page:addButton('sit','Sit on ground','Requests the original native Sit on ground event for the idle local player. Single-player only.',sit)
page:addButton('schoolbag','Give and wear one test schoolbag','Adds one missing Base.Bag_Schoolbag through the native factory and queues native wear only for an empty back slot. Existing bags and contents are preserved. Single-player only.',schoolbag)
page:addButton('supplies','Prepare demo supplies',
    'Adds missing spoon, fork, food, soup/stew bowl, bottle, schoolbag, belt and hammer types through the native inventory factory. Existing items are retained. Single-player only.',supplies)
function page:apply()
    if PZVRPhysicality then
        print('[PZVR Presentation] '..PZVRPhysicality.indexedModelsConfigure(indexedModels:getValue()))
        print('[PZVR Presentation] '..PZVRPhysicality.heldModelDiagnosticsConfigure(heldDiagnostics:getValue()))
        print('[PZVR Presentation] '..PZVRPhysicality.gripAttemptsConfigure(gripDiagnostics:getValue()))
        print('[PZVR Presentation] '..PZVRPhysicality.moveableModelsConfigure(moveableModels:getValue()))
        print('[PZVR Presentation] '..PZVRPhysicality.nativeHeldConfigure(nativeHeld:getValue()))
        print('[PZVR Presentation] '..PZVRPhysicality.windowMotionConfigure(windowMotion:getValue()))
    end
end
Events.OnGameStart.Add(function()
    page:apply()
    if started then return end
    started=true
    if PZVRPhysicality then PZVRPhysicality.gripAttemptsReset() end
    if showGuide:getValue() then readings() end
    if autoBackpack:getValue() then schoolbag() end
end)
Events.OnMainMenuEnter.Add(function()
    started=false;pending=nil;epoch=epoch+1
    if PZVRPhysicality then PZVRPhysicality.indexedModelsConfigure(false) end
    if PZVRPhysicality then PZVRPhysicality.heldModelDiagnosticsConfigure(false) end
    if PZVRPhysicality then PZVRPhysicality.gripAttemptsConfigure(false) end
    if PZVRPhysicality then PZVRPhysicality.gripAttemptsReset() end
    if PZVRPhysicality then PZVRPhysicality.moveableModelsConfigure(false) end
    if PZVRPhysicality then PZVRPhysicality.nativeHeldConfigure(false);PZVRPhysicality.windowMotionConfigure(false) end
    if PZVRPhysicalityGuide then PZVRPhysicalityGuide.close() end
end)
