require 'PZVRPhysicalityWaterActions'
require 'PZAPI/ModOptions'
local enabled
local function tick()
    if not PZVRPhysicality then return end
    PZVRPhysicality.tapsTick()
    if PZVRPhysicality.tapsAction()==0 then return end
    local p,source,cup=PZVRPhysicality.tapsPlayer(),PZVRPhysicality.tapsSource(),PZVRPhysicality.tapsCup()
    local ticket,epoch=PZVRPhysicality.tapsTicket(),PZVRPhysicality.tapsGeneration()
    local acknowledged=PZVRPhysicality.tapsAcknowledge(ticket,true)
    if not acknowledged then return end
    local ok,queued=pcall(function()
        return PZVRPhysicalityWaterActions.queue(p,source,cup,
            function() return PZVRPhysicality.tapsValid(ticket,epoch) end,
            function() PZVRPhysicality.tapsFinished(ticket) end)
    end)
    if not ok or not queued then PZVRPhysicality.tapsFinished(ticket) end
    if not ok then print('[PZVR Physicality] Tap action skipped: '..tostring(queued)) end
end
local function apply()
    if PZVRPhysicality then print('[PZVR Physicality] '..PZVRPhysicality.tapsConfigure(enabled:getValue())) end
end
local page=PZAPI.ModOptions:create('PZVRPhysicalityTaps','PZVR Physical Taps')
enabled=page:addTickBox('enabled','Physical taps (experimental)',false,'White dots mark calibrated handles. Touch one with an empty hand, hold trigger and turn your wrist by about 35 degrees. Place an open upright cup below the visible stream to fill, or lean to the spout and confirm drinking with trigger. Uses the eating/drinking trigger preference. Supports two inset sink models with native perspective rendering. Handle meshes do not rotate yet; Iris/Panini output is not supported by this pilot.')
function page:apply() apply() end
Events.OnTick.Add(tick)
Events.OnGameStart.Add(apply)
Events.OnMainMenuEnter.Add(function() if PZVRPhysicality then PZVRPhysicality.tapsConfigure(false) end end)
