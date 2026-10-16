require 'PZAPI/ModOptions'
local page=PZAPI.ModOptions:create('PZVRPhysicalityProviders','PZVR curtain model provider')
local pack=page:addTickBox('packCurtains','Use reviewed Curtains V2 rendered cloth',false,
    'Uses the current winning model and captured pose for original native curtain objects. Missing or stale cloth omits contact actions. Hospital curtains, tent flaps and decorative awnings do not gain native curtain actions.')
PZVRPhysicalityProviderOptions=PZVRPhysicalityProviderOptions or {}
function PZVRPhysicalityProviderOptions.prepareCurtains()
    if PZVRPhysicality then return PZVRPhysicality.packCurtainsConfigure(pack:getValue()) end
    return 'Physicality unavailable'
end
function page:apply()
    print('[PZVR Curtain provider] '..PZVRPhysicalityProviderOptions.prepareCurtains())
end
Events.OnGameStart.Add(function() page:apply() end)
Events.OnMainMenuEnter.Add(function() if PZVRPhysicality then PZVRPhysicality.packCurtainsConfigure(false) end end)
