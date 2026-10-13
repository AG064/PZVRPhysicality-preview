require 'Entity/ISEntityUI'
require 'PZAPI/ModOptions'

local enabled,reach
local tools={['Base.Hammer']=true,['Base.Saw']=true,['Base.Screwdriver']=true,['Base.Scissors']=true,['Base.Wrench']=true,['Base.KitchenKnife']=true}
local function tick()
    if not PZVRPhysicality then return end
    PZVRPhysicality.craftingTick()
    local ticket=PZVRPhysicality.craftingTicket()
    if ticket<=0 then return end
    local p=getSpecificPlayer(0)
    local tool,side,epoch=PZVRPhysicality.craftingTool(),PZVRPhysicality.craftingSide(),PZVRPhysicality.craftingGeneration()
    local opened=false
    local ok,err=pcall(function()
        if not p or p~=PZVRPhysicality.craftingPlayer() or p:isDead() or p:getVehicle() or p:hasTimedActions() or isClient() or isServer() or not tool or (side~=0 and side~=1) then return end
        local empty,held
        if side==1 then empty=p:getPrimaryHandItem();held=p:getSecondaryHandItem()
        else empty=p:getSecondaryHandItem();held=p:getPrimaryHandItem() end
        if empty or held~=tool or not tools[tool:getFullType()] or tool:getCondition()<=0 or tool:getContainer()~=p:getInventory() or not p:getInventory():contains(tool) or tool:getWorldItem() then return end
        local script=tool:getScriptItem()
        if not script or script:getFullName()~=tool:getFullType() or not PZVRPhysicality.craftingValid(p,tool,side,ticket,epoch) then return end
        -- This is the game's own "recipes using this item" path. It chooses reachable surfaces,
        -- containers, recipes and original timed actions; the shortcut never starts a recipe.
        if ISEntityUI.IsWindowOpen(p:getPlayerNum(),'HandcraftWindow') then
            local window=ISEntityUI.GetWindowInstance(p:getPlayerNum(),'HandcraftWindow')
            if not window or window.player~=p then return end
            window:bringToTop()
        else
            if not PZVRPhysicality.craftingValid(p,tool,side,ticket,epoch) then return end
            ISEntityUI.OpenHandcraftWindow(p,nil,'*',false,nil,'!'..script:getFullName())
        end
        local window=ISEntityUI.GetWindowInstance(p:getPlayerNum(),'HandcraftWindow')
        opened=window and window.player==p and ISEntityUI.IsWindowOpen(p:getPlayerNum(),'HandcraftWindow') and true or false
        if opened then PZVRPhysicality.craftingShowHud() end
    end)
    PZVRPhysicality.craftingAcknowledge(ticket,opened)
    if not ok then print('[PZVR Crafting] Native crafting window unavailable: '..tostring(err)) end
end
local function apply()
    if PZVRPhysicality then print('[PZVR Crafting] '..PZVRPhysicality.craftingConfigure(enabled:getValue(),reach:getValue())) end
end
local page=PZAPI.ModOptions:create('PZVRPhysicalityCrafting','PZVR Physical Crafting')
enabled=page:addTickBox('enabled','Tool-touch crafting shortcut',false,'Hold a supported tool forward in one hand. Separate hands and release trigger, then bring the empty other hand close and hold its trigger. Opens the native recipes-for-tool window. Choose and confirm a recipe normally; ingredients, tools, duration and outputs stay native. Body storage and curtain grabs take priority. Single-player only.')
reach=page:addSlider('handReachCm','Hand-to-tool contact distance (cm)',8,30,1,14,'Physical hand distance, independent of game world scale.')
function page:apply() apply() end
Events.OnTick.Add(tick)
Events.OnGameStart.Add(apply)
Events.OnMainMenuEnter.Add(function() if PZVRPhysicality then PZVRPhysicality.craftingConfigure(false,14) end end)
