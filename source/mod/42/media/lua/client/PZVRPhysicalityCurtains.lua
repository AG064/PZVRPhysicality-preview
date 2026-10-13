require 'TimedActions/ISTimedActionQueue'
require 'TimedActions/ISOpenCloseCurtain'
require 'PZAPI/ModOptions'

local function tick()
    local api=PZVRPhysicality
    if not api then return end
    api.curtainsTick()
    local ticket=api.curtainsTicket()
    if not ticket or ticket==0 then return end
    local player,source=api.curtainsPlayer(),api.curtainsSource()
    local epoch=api.curtainsGeneration()
    local before=source and source:IsOpen()
    local queued=false
    local action
    local ok,err=pcall(function()
        if not player or player~=getSpecificPlayer(0) or not source or not instanceof(source,'IsoCurtain')
            or isClient() or isServer() or player:isDead() or player:getVehicle() or player:hasTimedActions()
            or not api.curtainsValid(ticket,epoch) then return end
        action=ISOpenCloseCurtain:new(player,source)
        if not action or type(action.isValid)~='function' or type(action.complete)~='function' then return end
        action.pzvrCurtainTicket=ticket
        local originalValid,originalComplete,originalStop=action.isValid,action.complete,action.stop
        local finished=false
        local function finish(completed)
            if finished then return end
            finished=true
            api.curtainsFinished(ticket,completed)
        end
        local function valid(self)
            return not finished and self.character==player and self.item==source
                and player==getSpecificPlayer(0) and not isClient() and not isServer()
                and source:IsOpen()==before and api.curtainsValid(ticket,epoch)
        end
        action.isValid=function(self,...)
            if not valid(self) then finish(false);return false end
            local result=originalValid(self,...)
            if not result then finish(false) end
            return result
        end
        action.stop=function(self,...)
            finish(false)
            if type(originalStop)=='function' then return originalStop(self,...) end
        end
        action.complete=function(self,...)
            if not valid(self) then finish(false);return false end
            local nativeOk,result=pcall(originalComplete,self,...)
            finish(nativeOk and source:IsOpen()~=before)
            if not nativeOk then error(result) end
            return result
        end
        if action:isValid() then
            ISTimedActionQueue.add(action)
            local nativeQueue=ISTimedActionQueue.getTimedActionQueue(player)
            queued=nativeQueue and nativeQueue:indexOf(action)~=-1 or false
        end
    end)
    local accepted=api.curtainsAcknowledge(ticket,ok and queued)
    if queued and not accepted and action then action:stop() end
    if not ok then print('[PZVR Curtains] Native action skipped: '..tostring(err)) end
end

local page=PZAPI.ModOptions:create('PZVRPhysicalityCurtains','PZVR Physical Curtains')
local enabled=page:addTickBox('enabled','Physical curtain grip',false,
    'Single-player: release the grip, touch a visible vertical cloth edge with an empty hand, then grip and slide sideways about 10 cm. Uses the original open/close action and current 3D cloth geometry. Closed glass and window frames block reach. Missing geometry keeps native controls available. Curtain cloth is not simulated.')
local function apply() if PZVRPhysicality then print('[PZVR Curtains] '..PZVRPhysicality.curtainsConfigure(enabled:getValue())) end end
function page:apply() apply() end
Events.OnTick.Add(tick)
Events.OnGameStart.Add(apply)
Events.OnMainMenuEnter.Add(function() if PZVRPhysicality then PZVRPhysicality.curtainsConfigure(false) end end)
