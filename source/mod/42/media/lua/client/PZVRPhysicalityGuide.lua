require 'ISUI/ISCollapsableWindow'
require 'ISUI/ISRichTextPanel'
require 'ISUI/ISButton'

PZVRPhysicalityGuide=PZVRPhysicalityGuide or {}
local guide=PZVRPhysicalityGuide
local Window=ISCollapsableWindow:derive('PZVRPhysicalityGuideWindow')
local lastMessage='Choose a gesture below. Status refreshes while this window is open.'
local function clean(value,maximum)
    return tostring(value):gsub('[<>\r\n]',' '):sub(1,maximum or 700)
end
local function status(method)
    if not PZVRPhysicality or type(PZVRPhysicality[method])~='function' then return 'Unavailable: extension or status API missing' end
    local ok,value=pcall(PZVRPhysicality[method])
    local maximum=method=='gripAttemptsStatus' and 14000 or method=='heldModelDiagnosticsStatus' and 4000 or 700
    return ok and clean(value,maximum) or ('Unavailable: '..clean(value))
end
local instructions='<H2> Controls <TEXT> <LINE> '
    ..'Settings: Escape > Options > Mods. Use PZVR Gameplay Presets, then Apply selected preset; individual PZVR pages remain editable. PZVR Presentation reopens this guide. <LINE> '
    ..'Hands and floor items: close inventory/menus, use a tracked empty hand near a visible supported item, squeeze grip to pick up, release grip to drop or throw. Focus loss, pause, vehicles, active actions and unsupported geometry can restore native input. <LINE> '
    ..'If pickup fails, check logical Native hand slots below: an invisible held item still occupies the hand. Unequip it through native inventory. Keep recent grip attempt reasons retains the closed-HUD attempt state for later inspection here. <LINE> '
    ..'Storage: open/select a reachable container through native loot inventory once. Close the HUD, then grip its visible supported contents. Release a supported held item inside that selected container. No visible geometry means use native inventory. <LINE> '
    ..'Food: bring supported held food to the mouth and confirm with its trigger. Utensils: hold spoon/fork and supported food in opposite hands; touch the verified edible face with the actual tip, move away, then bring the loaded utensil to the mouth and trigger. Soup/stew requires an upright supported bowl and spoon. Unsupported food uses native Eat. Native grab, consumed grip and bandaging retain control; release grip and trigger to neutral before retrying a physical action. <LINE> '
    ..'Body and backpack: grip gear near its calibrated native belt/back attachment. Release compatible held gear near an empty slot. Grip a worn bag over the shoulder; hold it away and use the other hand to grip visible contents. Bag-hand trigger pages contents. Use PZVR Backpack and Body Storage calibration if slots are unreachable. <LINE> '
    ..'Sink: empty hand near a supported tap, hold trigger and turn wrist about 35 degrees. Keep an open supported cup upright under the stream. Curtains: grip near supported fabric, pull and release. Crafting: hold a supported tool, bring the other empty hand near it and hold trigger for native recipes. <LINE> '
    ..'Finger touch: PZVR Finger Poses enables available sensor channels and supported finger bones. Unknown channels and native grasp retain their own poses. Grip analog is not a separate capacitive sensor. <LINE> '
    ..'Player collision: PZVR avatar mesh collision restricts avatar movement against captured supported surfaces. Presentation also restricts room-scale body movement; Comfort leaves room-scale movement native. Headset pose and head lean remain upstream tracking. Missing or stale geometry uses native movement. <LINE> '
    ..'Moveable models: supported selected pack items need an exact current full-pose draw before physical carry/contact. Unsupported or over-budget items keep native fallback. Native world-only held model fallback currently supports CordlessPhone, using original FBX geometry and native hand mounts. Existing native held rows, weapons and food keep priority. <LINE> '
    ..'PZVR World Visuals enables supported emitted counter-sink geometry and faucet feedback separately from physical tap actions. Check its current emitted-model and rendered receipt before judging alignment. Window sash motion uses measured supported pairs and matching progressed draw/contact; unknown pairs keep native behavior. Selected walls, doors and curtains retain their provider geometry; extra universal thickness is not added. <LINE> '
    ..'Sit and test backpack: the buttons request native single-player actions. A test schoolbag is added only when missing; another worn backpack is never replaced. Supplies are optional and idempotent. <LINE> '
local fields={{'SDK','sdkStatus'},{'Native guide UI','guideUiStatus'},{'Modes','modesStatus'},{'Native hand slots','handStateStatus'},{'Interaction guards','interactionStatus'},{'Touch input','inputStatus'},
    {'Fingers','fingersStatus'},{'Items','status'},{'Consumption','consumptionStatus'},{'Storage','storageStatus'},{'Storage provider','storageProvidersStatus'},
    {'Taps','tapsStatus'},{'Body','bodyStatus'},{'Backpack','backpackStatus'},{'Curtains','curtainsStatus'},{'Curtain provider','packCurtainsStatus'},
    {'Crafting','craftingStatus'},{'Held contacts','contactsStatus'},{'World geometry','worldContactsStatus'},{'World visual feedback','worldVisualsStatus'},{'Indexed models','indexedModelsStatus'},{'Moveable models','moveableModelsStatus'},{'Native held fallback','nativeHeldStatus'},{'Window sash motion','windowMotionStatus'},{'Held model receipts','heldModelDiagnosticsStatus'},{'Player collision','playerCollisionStatus'},{'Recent grip attempts','gripAttemptsStatus'}}
function Window:createChildren()
    ISCollapsableWindow.createChildren(self)
    local header=self:titleBarHeight()+4
    local buttonHeight=math.max(25,self:titleBarHeight())
    self.text=ISRichTextPanel:new(12,header,self.width-24,self.height-header-buttonHeight-26)
    self.text:initialise();self.text:addScrollBars();self:addChild(self.text)
    local buttonWidth=(self.width-48)/4
    local function button(x,width,label,action)
        local b=ISButton:new(x,self.height-buttonHeight-12,width,buttonHeight,label,self,function()
            if PZVRPresentationActions and PZVRPresentationActions[action] then PZVRPresentationActions[action]() end
        end)
        b:initialise();self:addChild(b)
    end
    local back=ISButton:new(12,self.height-buttonHeight-12,buttonWidth,buttonHeight,'Return to game',self,function() guide.returnToGame() end)
    back:initialise();self:addChild(back)
    button(20+buttonWidth,buttonWidth,'Sit on ground','sit');button(28+buttonWidth*2,buttonWidth,'Test schoolbag','schoolbag');button(36+buttonWidth*3,buttonWidth,'Demo supplies','supplies')
    self:refresh()
end
function Window:refresh()
    local text='<H1> PZVR Controls and Status <TEXT> <LINE> Grip near a supported item with an empty hand; release to drop. Mouth contact and trigger confirm supported consumption. Return to game closes the native HUD before physical interaction. Closing only this window leaves the native HUD under your controls. Default HUD action: tap the secondary button on the controller shown by the native binding mapping below; holding it opens the radial menu. Controller mirroring is separate from aim/UI dominant hand. Current bindings are listed below and configurable under native PZVR Controller bindings. Scroll for the full controls. <LINE> <H2> Last action <TEXT> <LINE> '..clean(lastMessage)..' <LINE> <H2> Live status <TEXT> <LINE> '
    for _,field in ipairs(fields) do text=text..field[1]..': '..status(field[2])..' <LINE> ' end
    text=text..instructions
    if text~=self.lastText then self.lastText=text;self.text:setText(text);self.text:paginate() end
end
function Window:update()
    if self.guideFailed then return end
    local ok=pcall(ISCollapsableWindow.update,self)
    if not ok then guide.cleanup(self);return end
    if not self:getIsVisible() then return end
    local now=getTimestampMs()
    if not self.nextRefresh or now>=self.nextRefresh then
        self.nextRefresh=now+500
        if not pcall(function() guide.ensureHud(self);self:refresh() end) then guide.cleanup(self) end
    end
end
function Window:close() guide.close() end
function guide.cleanup(window)
    local removed=not window or not window.guideAdded
    if window then
        if type(window.setVisible)=='function' then pcall(window.setVisible,window,false) end
        if type(window.removeFromUIManager)=='function' then removed=pcall(window.removeFromUIManager,window) end
        window.guideFailed=not removed
    end
    if removed then if guide.window==window then guide.window=nil end else guide.window=window end
    return removed
end
function guide.ensureHud(window)
    if window.hudAttempted then return end
    if not PZVRPhysicality or type(PZVRPhysicality.ready)~='function' or PZVRPhysicality.ready()~=true then return end
    window.hudAttempted=true
    if type(PZVRPhysicality.guideShowHud)~='function' then return end
    local ok,shown=pcall(PZVRPhysicality.guideShowHud)
    if not ok or shown~=true then lastMessage='Native HUD unavailable. Use native PZVR HUD controls; reopen this guide after SDK recovery.' end
end
function guide.open()
    local window=guide.window
    if window and window.guideFailed then if not guide.cleanup(window) then return end;window=nil end
    local ok=pcall(function()
    if not window then
        local core=getCore();local width=math.min(760,core:getScreenWidth()-24);local height=math.min(680,core:getScreenHeight()-24)
        window=Window:new(12,12,width,height)
        window.title='PZVR Controls and Status';window.resizable=false
        window:initialise();window:instantiate();window.guideAdded=true;window:addToUIManager();guide.window=window
    end
    window.hudAttempted=false;window:setVisible(true);guide.ensureHud(window);window:refresh();window.text:setYScroll(0)
    end)
    if not ok then guide.cleanup(window);lastMessage='Guide UI callback failed; reopen to retry.' end
end
function guide.close() if guide.window and not pcall(guide.window.setVisible,guide.window,false) then guide.cleanup(guide.window) end end
function guide.message(text) lastMessage=clean(text);if guide.window and not pcall(function() guide.window:refresh();guide.window.text:setYScroll(0) end) then guide.cleanup(guide.window) end end
function guide.returnToGame()
    if not PZVRPhysicality or type(PZVRPhysicality.guideReturnToGame)~='function' then guide.message('Native HUD close unavailable; use the HUD binding listed above.');return end
    local ok,closed=pcall(PZVRPhysicality.guideReturnToGame)
    if ok and closed==true then guide.close() else guide.message('Native menu or HUD remains open. Press Return to game again, or use the native HUD binding listed above.') end
end
