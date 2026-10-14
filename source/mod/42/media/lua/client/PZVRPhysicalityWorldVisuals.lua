require 'PZAPI/ModOptions'
local page=PZAPI.ModOptions:create('PZVRPhysicalityWorldVisuals','PZVR World Visuals')
local enabled=page:addTickBox('enabled','Counter sink faucets',false,
    'Single-player experimental geometry for the exact selected white kitchen counter sinks. Adds a curved spout and two handles on the measured original deck. Physical taps must be enabled separately to turn handles or use native water. Original counter assets, water quantities, curtains, windows, doors and walls remain owned by their selected providers.')
local function apply()
    if PZVRPhysicality then print('[PZVR World Visuals] '..PZVRPhysicality.worldVisualsConfigure(enabled:getValue())) end
end
function page:apply() apply() end
Events.OnGameStart.Add(apply)
Events.OnMainMenuEnter.Add(function() if PZVRPhysicality then PZVRPhysicality.worldVisualsConfigure(false) end end)
