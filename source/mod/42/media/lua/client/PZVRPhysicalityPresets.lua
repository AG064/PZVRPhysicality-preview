require 'PZAPI/ModOptions'

local page=PZAPI.ModOptions:create('PZVRPhysicalityPresets','PZVR Gameplay Presets')
local choice=page:addComboBox('choice','Preset','Choose a preset, then press Apply. Individual settings remain editable afterward. Body calibration and controller bindings are preserved.')
choice:addItem('Presentation',true);choice:addItem('Comfort',false);choice:addItem('Native controls',false)
local function plan(index)
    local active=index~=3
    local curtains=active and PZVRPhysicality.curtainsAvailable()
    return {
        {'PZVRPhysicality','floorItems',active},
        {'PZVRPhysicalityConsumption','enabled',active},
        {'PZVRPhysicalityConsumption','confirmTrigger',true},
        {'PZVRPhysicalityTaps','enabled',active},
        {'PZVRPhysicalityFingers','enabled',active},
        {'PZVRPlacement','persistentLayout',active},
        {'PZVRPlacement','storageModels',active},
        {'PZVRPlacement','storagePhysics',index==1},
        {'PZVRPhysicalityBody','enabled',active},
        {'PZVRPhysicalityBody','reachCm',index==2 and 20 or 16},
        {'PZVRPhysicalityCurtains','enabled',curtains},
        {'PZVRPhysicalityCrafting','enabled',active},
        {'PZVRPhysicalityBackpack','enabled',active},
        {'PZVRPhysicalityContacts','enabled',active},
        {'PZVRPresentation','showGuideOnStart',active},
        {'PZVRPresentation','indexedModels',active},
        {'PZVRPresentation','autoBackpack',index==1},
        {'PZVRPhysicalityCollision','enabled',active},
        {'PZVRPhysicalityCollision','roomScaleBody',index==1},
        {'PZVRPresentation','moveableModels',index==1},
        {'PZVRPresentation','nativeHeldFallback',index==1},
        {'PZVRPresentation','physicalWindowMotion',index==1},
        {'PZVRPhysicalityWorldVisuals','enabled',index==1},
    }
end
page:addButton('applyPreset','Apply selected preset','Presentation enables supported single-player features and schedules the guide/test backpack for the next world start. Comfort increases body reach and retains native room-scale motion. Native controls disables optional adapters. Applying this preset never grants or equips items. Settings save through native Mod Options.',function()
    if not PZVRPhysicality or not PZVRPhysicality.ready() then print('[PZVR Presets] Preset unavailable: native SDK validation pending.');return end
    local index=choice:getValue();if index~=1 and index~=2 and index~=3 then return end
    local changes,pages,seen={},{},{}
    -- Resolve every option before changing any state. Partial plugin loads cannot apply half a preset.
    for _,entry in ipairs(plan(index)) do
        local target=PZAPI.ModOptions:getOptions(entry[1]);local option=target and target.dict and target.dict[entry[2]]
        if not option or type(option.getValue)~='function' or type(option.setValue)~='function' or type(target.apply)~='function' then
            print('[PZVR Presets] Preset unavailable: missing '..entry[1]..'/'..entry[2]);return
        end
        changes[#changes+1]={option=option,before=option:getValue(),after=entry[3]}
        if not seen[target] then pages[#pages+1]=target;seen[target]=true end
    end
    local failures=PZVRPhysicality.configurationFailures()
    local ok,err=pcall(function()
        for _,entry in ipairs(changes) do entry.option:setValue(entry.after) end
        for _,target in ipairs(pages) do target:apply() end
        local active=index~=3
        local curtains=active and PZVRPhysicality.curtainsAvailable()
        if PZVRPhysicality.configurationFailures()~=failures or not PZVRPhysicality.presetValid(active,active,active,active,index==1,active,curtains,active) or not PZVRPhysicality.presetExtrasValid(active,active) or not PZVRPhysicality.presetPresentationValid(active) or not PZVRPhysicality.presetPlayerCollisionValid(active,index==1) or not PZVRPhysicality.presetMoveableModelsValid(index==1) or not PZVRPhysicality.presetWorldVisualsValid(index==1) or not PZVRPhysicality.presetNativeHeldValid(index==1) or not PZVRPhysicality.presetWindowMotionValid(index==1) then error('Native feature configuration did not match the selected preset') end
    end)
    if not ok then
        for _,entry in ipairs(changes) do local restored,why=pcall(function() entry.option:setValue(entry.before) end);if not restored then print('[PZVR Presets] Restore failed: '..tostring(why)) end end
        for _,target in ipairs(pages) do local restored,why=pcall(function() target:apply() end);if not restored then print('[PZVR Presets] Restore apply failed: '..tostring(why)) end end
        print('[PZVR Presets] Preset failed; previous option values restored. Check feature status: '..tostring(err));return
    end
    PZAPI.ModOptions:save();print('[PZVR Presets] Applied '..({'Presentation','Comfort','Native controls'})[index])
end)
