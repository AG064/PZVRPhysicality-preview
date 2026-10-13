require 'PZAPI/ModOptions'

local page=PZAPI.ModOptions:create('PZVRPhysicalityFingers','PZVR Finger Poses')
local enabled=page:addTickBox('enabled','Capacitive finger poses (experimental)',false,
    'Uses supported touch contacts and trigger/squeeze pressure for free hands. Complete five-root rigs separate index and grip poses; the stock rig keeps its grouped finger mapping. Held items keep their native grip. Missing sensors retain normal poses.')
page:addButton('readings','Print current touch readings',
    'Writes separate left/right touch and pressure readings to the game console log. A question mark means the runtime has no active value for that sensor.',
    function() if PZVRPhysicality then print('[PZVR Fingers] '..PZVRPhysicality.fingersStatus()) end end)

local function apply()
    if PZVRPhysicality then print('[PZVR Fingers] '..PZVRPhysicality.fingersConfigure(enabled:getValue())) end
end
function page:apply() apply() end
Events.OnGameStart.Add(apply)
Events.OnMainMenuEnter.Add(function()
    if PZVRPhysicality then PZVRPhysicality.fingersConfigure(false) end
end)
