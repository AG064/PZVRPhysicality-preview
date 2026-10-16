require 'PZAPI/ModOptions'
local page=PZAPI.ModOptions:create('PZVRPhysicalityStorageProviders','PZVR storage model providers')
local visible=page:addTickBox('visibleLoot','Use reviewed Visible Loot storage geometry',false,
    'Optional provider support. Uses the exact current emitted housing and native inventory actions. A missing, stale or unsupported winning model blocks Physicality contents instead of drawing a second set of stock. KK remains the default provider.')
function page:apply()
    if PZVRPhysicality then print('[PZVR Storage providers] '..PZVRPhysicality.storageProvidersConfigure(visible:getValue())) end
end
Events.OnGameStart.Add(function() page:apply() end)
Events.OnMainMenuEnter.Add(function() if PZVRPhysicality then PZVRPhysicality.storageProvidersConfigure(false) end end)
