require 'TimedActions/ISTimedActionQueue'
require 'TimedActions/ISTakeWaterAction'
require 'ISUI/ISInventoryPaneContextMenu'

-- Adapter for the calibrated world-interaction layer. It does not discover or approximate tap positions.
PZVRPhysicalityWaterActions={}
function PZVRPhysicalityWaterActions.queue(p,source,cup,valid,finished)
    if type(valid)~='function' or type(finished)~='function' then return false end
    if not p or p~=getSpecificPlayer(0) or p:isDead() or p:getVehicle() or p:hasTimedActions()
        or isClient() or isServer() or not source or not source:hasFluid() or not source:getSquare() or not p:getSquare()
        or source:getSquare():isBlockedTo(p:getSquare()) or not source:getSquare():isAdjacentTo(p:getSquare()) then return false end
    if cup and (not p:getInventory():contains(cup) or cup:isSealed()
        or p:getPrimaryHandItem()~=cup and p:getSecondaryHandItem()~=cup
        or not cup:getFluidContainer() or not source:canTransferFluidTo(cup:getFluidContainer())) then return false end
    if not cup and ISInventoryPaneContextMenu.getEatingMask(p,false) then return false end
    local function contact()
        local ok,value=pcall(valid)
        return ok and value==true and p==getSpecificPlayer(0) and not p:isDead() and not p:getVehicle()
            and not isClient() and not isServer() and source:hasFluid() and source:getSquare() and p:getSquare()
            and source:getSquare():isAdjacentTo(p:getSquare()) and not source:getSquare():isBlockedTo(p:getSquare())
            and (cup or not ISInventoryPaneContextMenu.getEatingMask(p,false))
            and (not cup or p:getInventory():contains(cup) and not cup:isSealed()
                and (p:getPrimaryHandItem()==cup or p:getSecondaryHandItem()==cup))
    end
    if not contact() then return false end
    local action=ISTakeWaterAction:new(p,cup,source,source:isTaintedWater())
    if not action:isValid() or type(action.waterUnit)~='number' or action.waterUnit~=action.waterUnit or action.waterUnit<=0 then return false end
    -- Native constructor already limits this by source volume, thirst, free capacity and encumbrance.
    action.waterUnit=math.min(action.waterUnit,.04)
    action.maxTime=action:getDuration()
    local originalValid,originalUpdate,originalComplete,originalStop=action.isValid,action.update,action.complete,action.stop
    local done=false
    local function finish()
        if done then return end
        done=true
        local ok,err=pcall(finished)
        if not ok then print('[PZVR Physicality] Tap cleanup: '..tostring(err)) end
    end
    local function nativeFinish(fn,self,...)
        local function capture(...) return {n=select('#',...),...} end
        local result=capture(pcall(fn,self,...));finish()
        if not result[1] then error(result[2],0) end
        return unpack(result,2,result.n)
    end
    action.isValid=function(self,...) return not done and contact() and originalValid(self,...) end
    action.update=function(self,...)
        if done then return end
        if not contact() then self:forceStop();return end
        return originalUpdate(self,...)
    end
    action.complete=function(self,...)
        if done then return false end
        if not contact() then nativeFinish(originalStop,self);return false end
        return nativeFinish(originalComplete,self,...)
    end
    action.stop=function(self,...)
        if done then return end
        return nativeFinish(originalStop,self,...)
    end
    ISTimedActionQueue.add(action)
    return true
end
