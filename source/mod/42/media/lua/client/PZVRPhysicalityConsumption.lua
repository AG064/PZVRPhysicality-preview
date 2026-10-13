require 'TimedActions/ISTimedActionQueue'
require 'TimedActions/ISEatFoodAction'
require 'TimedActions/ISDrinkFluidAction'
require 'ISUI/ISInventoryPaneContextMenu'
require 'PZAPI/ModOptions'

local enabled,confirm,down,reach
local function held(p,side)
    if side~=0 and side~=1 then return nil end
    if PZVRPhysicality.consumptionPrimaryHand(side) then return p:getPrimaryHandItem() end
    return p:getSecondaryHandItem()
end
local function nativeAllowed(p,item)
    if not p or p~=getSpecificPlayer(0) or p:isDead() or p:getVehicle() or isClient() or isServer()
        or not item or item:getContainer()~=p:getInventory() or not p:getInventory():contains(item) or item:getWorldItem() or item:isSealed() then return false end
    -- A face covering stays on until the player removes it through the normal game action.
    if ISInventoryPaneContextMenu.getEatingMask(p,false) then return false end
    return true
end
local function tick()
    if not PZVRPhysicality then return end
    PZVRPhysicality.consumptionTick()
    local kind=PZVRPhysicality.consumptionAction()
    if kind==0 then return end
    local p=getSpecificPlayer(0)
    local item,side=PZVRPhysicality.consumptionItem(),PZVRPhysicality.consumptionSide()
    local tool=kind==3 and PZVRPhysicality.consumptionTool() or nil
    local ticket,epoch=PZVRPhysicality.consumptionTicket(),PZVRPhysicality.consumptionGeneration()
    local percent=PZVRPhysicality.consumptionPortion()
    local queued=false
    local ok,err=pcall(function()
        if not nativeAllowed(p,item) or p:hasTimedActions() or (side~=0 and side~=1)
            or type(percent)~='number' or percent~=percent or percent<=0 or percent>1 then return end
        local function holding()
            if kind~=3 then return held(p,side)==item end
            return tool and tool~=item and held(p,side)==tool and held(p,1-side)==item
                and nativeAllowed(p,tool) and tool:getCondition()>0
                and (tool:hasTag(ItemTag.SPOON) or tool:hasTag(ItemTag.FORK))
                and (item:getEatType()~='2handbowl' or tool:getFullType()=='Base.Spoon' and tool:hasTag(ItemTag.SPOON))
        end
        if not holding() then return end
        local action
        local food=kind==1 or kind==3
        local hunger=food and item:getHungChange() or nil
        if food then
            if not instanceof(item,'Food') or type(hunger)~='number' or hunger~=hunger or hunger>=0 or item:isFrozen()
                or item:getOpeningRecipe() and item:getOpeningRecipe()~='' or item:hasTag(ItemTag.SMOKABLE)
                or item:getScriptItem() and item:getScriptItem():isCantEat()
                or item:getRequireInHandOrInventory() and item:getRequireInHandOrInventory():size()>0 then return end
            action=ISEatFoodAction:new(p,item,percent)
            if kind==3 then
                action.spoon=tool:hasTag(ItemTag.SPOON) and tool or nil
                action.fork=tool:hasTag(ItemTag.FORK) and tool or nil
                action.getSecondItem=function(self) return tool end
                action.useUtensil=true
            end
        elseif kind==2 then
            local fluid=item:getFluidContainer()
            if not fluid or fluid:isEmpty() or not fluid:canPlayerEmpty() or fluid:getCapacity()>3 then return end
            action=ISDrinkFluidAction:new(p,item,percent)
        else return end
        if not action:isValid() or action.isValidStart and not action:isValidStart() then return end
        local originalValid,originalStart,originalUpdate,originalComplete,originalStop=action.isValid,action.start,action.update,action.complete,action.stop
        if type(originalStart)~='function' or type(originalUpdate)~='function' or type(originalComplete)~='function' or type(originalStop)~='function'
            or type(action.setOverrideHandModels)~='function' or food and type(action.serverStop)~='function' then return end
        local function valid(self)
            return self.character==p and self.item==item and nativeAllowed(p,item) and holding()
                and (not food or item:getHungChange()==hunger)
                and PZVRPhysicality.consumptionValid(p,item,side,ticket,epoch)
        end
        local finished=false
        local function finish()
            if finished then return end
            finished=true
            local ok,err=pcall(function() PZVRPhysicality.consumptionFinished(ticket) end)
            if not ok then print('[PZVR Physicality] Consumption cleanup: '..tostring(err)) end
        end
        -- The native action still owns sounds, job progress and cleanup. Its model preview is scoped out
        -- for this action only; PZVR keeps the actual tracked item in each of its existing hands.
        action.setOverrideHandModels=function(self,...) end
        -- Vanilla food stop can apply a progress-weighted Eat. A cancelled physical bite commits nothing.
        -- Fluid actions retain their original incremental updates and stop behavior.
        if food then action.serverStop=function(self,...) end end
        action.isValid=function(self,...) return valid(self) and originalValid(self,...) end
        action.start=function(self,...)
            if finished then return end
            if not valid(self) then self:forceStop();return end
            return originalStart(self,...)
        end
        -- Check before vanilla's incremental fluid update, which owns the consumed volume.
        action.update=function(self,...)
            if finished then return end
            if not valid(self) then self:forceStop();return end
            return originalUpdate(self,...)
        end
        local function nativeFinish(fn,self,...)
            local function capture(...) return {n=select('#',...),...} end
            local result=capture(pcall(fn,self,...))
            finish()
            if not result[1] then error(result[2],0) end
            return unpack(result,2,result.n)
        end
        action.complete=function(self,...)
            if finished then return false end
            if not valid(self) then nativeFinish(originalStop,self);return false end
            return nativeFinish(originalComplete,self,...)
        end
        action.stop=function(self,...)
            if finished then return end
            return nativeFinish(originalStop,self,...)
        end
        -- A mouth gesture takes a small bite/sip; the native effects and leftovers are unchanged.
        local duration=food and 30 or 35
        action.maxTime=math.min(action.maxTime or duration,duration)
        action.getDuration=function(self) return self.maxTime end
        if not PZVRPhysicality.consumptionAcknowledge(ticket,true) then return end
        ISTimedActionQueue.add(action);queued=true
    end)
    if not queued then
        PZVRPhysicality.consumptionAcknowledge(ticket,false)
        PZVRPhysicality.consumptionFinished(ticket)
    end
    if not ok then print('[PZVR Physicality] Consumption action retained by native game: '..tostring(err)) end
end
local function apply()
    if PZVRPhysicality then print('[PZVR Physicality] '..PZVRPhysicality.consumptionConfigure(enabled:getValue(),confirm:getValue(),down:getValue(),reach:getValue())) end
end
local page=PZAPI.ModOptions:create('PZVRPhysicalityConsumption','PZVR Physical Eating and Drinking')
enabled=page:addTickBox('enabled','Physical eating and drinking',false,'Bring held food to your mouth for a bite. A verified spoon or fork can load one bite from supported bread, fruit, carrots or cut cheese held in your other hand. A spoon can take soup or stew from the exposed surface of a supported upright bowl. Then deliver the bite at your mouth. Tilt an open bottle or cup to sip. Sealed containers use the original opening actions.')
confirm=page:addTickBox('confirmTrigger','Hold trigger to eat or drink',true,'When disabled, deliberate mouth contact is enough. Food needs a new approach or trigger release for each bite.')
down=page:addSlider('mouthDownCm','Mouth below headset (cm)',4,20,1,11,'Adjust where your lips sit relative to the tracked headset.')
reach=page:addSlider('mouthRadiusCm','Mouth contact distance (cm)',5,16,.5,10.5,'Adjust contact tolerance. This does not change hand reach or world scale.')
function page:apply() apply() end
Events.OnTick.Add(tick)
Events.OnGameStart.Add(apply)
Events.OnMainMenuEnter.Add(function() if PZVRPhysicality then PZVRPhysicality.consumptionConfigure(false,true,11,10.5) end end)
