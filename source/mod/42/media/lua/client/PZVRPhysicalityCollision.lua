require 'PZAPI/ModOptions'
local page=PZAPI.ModOptions:create('PZVRPhysicalityCollision','PZVR avatar mesh collision')
local enabled=page:addTickBox('enabled','Restrict avatar against actual world meshes',false,
    'Single-player VR avatar movement uses current original furniture, door, window and wall triangles. Native collision remains active. Missing meshes are omitted rather than replaced with invisible full-tile barriers.')
local physical=page:addTickBox('roomScaleBody','Apply to room-scale avatar movement',true,
    'Also restrict the avatar movement requested by physical walking. Your actual headset pose and natural lean remain unchanged; this cannot physically stop you leaning through a virtual surface.')
local radius=page:addSlider('radiusCm','Avatar contact radius (cm)',10,35,1,18,
    'Horizontal body contact radius against original triangles. Native stairs, climbing, vehicles, UI ownership and multiplayer keep their native movement paths.')
function page:apply()
    if PZVRPhysicality then print('[PZVR Player collision] '..PZVRPhysicality.playerCollisionConfigure(enabled:getValue(),physical:getValue(),radius:getValue())) end
end
Events.OnGameStart.Add(function() page:apply() end)
Events.OnMainMenuEnter.Add(function() if PZVRPhysicality then PZVRPhysicality.playerCollisionConfigure(false,false,radius:getValue()) end end)
Events.OnPostUIDraw.Add(function()
    if not PZVRPhysicality or not enabled:getValue() then return end
    local alpha=PZVRPhysicality.playerCollisionHintAlpha()
    if alpha<=0 then return end
    local text=PZVRPhysicality.playerCollisionHint()
    if text~='' then getTextManager():DrawString(UIFont.Small,24,76,text,1,.8,.25,alpha) end
end)
