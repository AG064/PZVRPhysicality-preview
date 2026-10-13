require 'PZAPI/ModOptions'

local page=PZAPI.ModOptions:create('PZVRPhysicalityContacts','PZVR Held Prop Contacts')
local enabled=page:addTickBox('enabled','Physical contact feedback for held props',false,
    'Uses actual captured rigid prop meshes and supported world geometry. Native weapon support, action previews and controller poses retain priority. Single-player only.')
local frequency=page:addSlider('frequency','Contact feedback frequency (%)',0,100,1,100,
    'Controls how often contact pulses can occur. Zero mutes this feedback. Native haptic amplitude and the upstream haptics preference stay authoritative.')
local constrained=page:addTickBox('visualConstraints','Limit shallow held prop penetration',true,
    'Applies a bounded same-frame correction to the supported prop and its visible hand together. Native weapons, previews and active physical actions retain their original poses.')
local maximum=page:addSlider('maximumCm','Maximum visible contact correction (cm)',1,8,.5,5,
    'Caps the correction for supported single held props. Tracking and native gameplay input stay unchanged. Deep penetration can exceed the correction cap.')
function page:apply()
    if PZVRPhysicality then
        print('[PZVR Contacts] '..PZVRPhysicality.contactsVisualConfigure(constrained:getValue(),maximum:getValue()))
        print('[PZVR Contacts] '..PZVRPhysicality.contactsConfigure(enabled:getValue(),frequency:getValue()/100))
    end
end
Events.OnGameStart.Add(function() page:apply() end)
Events.OnTick.Add(function() if PZVRPhysicality then PZVRPhysicality.contactsTick() end end)
Events.OnMainMenuEnter.Add(function() if PZVRPhysicality then PZVRPhysicality.contactsConfigure(false,1) end end)
