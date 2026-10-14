# PZVR Physicality preview: live acceptance checklist

Use the preview's separate profile and a disposable single-player save. Run its Verify-Demo wrapper before Start-Demo. Keep the preserved previous profile available for comparison. Java approval and headset startup are user-run steps.

Record the preview version, game version, controller/runtime, GPU, refresh rate, selected mod IDs, dominant hand, preset and the result of each row. A checked row means the stated outcome was observed, rather than merely that the action was attempted. Record a short clip and the corresponding console excerpt for any failure.

## Acceptance method

1. Run the same task with the Native controls preset and Presentation preset using the same character, items and selected mods.
2. Compare native item identity, count, condition, fluid/nutrition, recipe output and world state before and after. Only the intended action may change them.
3. Repeat hand interactions with left and right physical hands and both dominant-hand settings. Physical hand slots and dominant button mappings are separate.
4. Save, quit and reload. Confirm the resulting native state, then repeat an interaction.
5. Mark parity only after both the native outcome and physical presentation pass. Offline verification covers implementation contracts; this checklist establishes headset and gameplay acceptance.

## Startup, controls and presentation

- [ ] Verify-Demo reports ready and the intended preview version before launch.
- [ ] Approve the Physicality Java mod through ZombieBuddy, restart if requested, and confirm no unavailable SDK or disabled-after-error messages.
- [ ] Start VR and load the disposable save. Both eyes show the same world and UI without incorrect depth, flicker, double rendering or disappearing models.
- [ ] The presentation guide opens once. Close and reopen it through Escape > Options > Mods > PZVR Presentation > Open controls and live status. Confirm mode, tracking/input guard, raw touch and geometry reasons update visibly while the guide is open.
- [ ] Start with the HUD closed and confirm the guide is visible inside the headset through the native HUD. Return to game closes the HUD or reports menu dismissal. Window-only close preserves native HUD controls. Callback failures must not duplicate windows or repeatedly reopen HUD.
- [ ] With no worn backpack, presentation startup adds at most one missing Schoolbag and completes one native wear action. Repeat the button and startup callback without duplicates. With a different backpack worn, its identity, contents and equipped state remain intact.
- [ ] Use the guide's Sit on ground button with an idle living actor outside a vehicle. Native sitting and standing work. Dead, vehicle, timed-action and multiplayer guards reject the helper without changing inventory.
- [ ] Compare ordinary profile defaults and Native controls against the presentation profile. Automatic guide/backpack and indexed-model repair are Off in ordinary profiles; applying a preset alone never grants or wears an item.
- [ ] Enable Record actual held model placement in PZVR Presentation and compare the same item in Native controls and Presentation. Capture exact item/model/attachment and final placement receipts with a clip; changing presets retains this diagnostic selection without changing poses or inventory.
- [ ] Read logical native hand slots with detailed recording Off. An invisible occupied mug still reports its actual slot/type and native model-reference state. Use native inventory to unequip it before an empty-hand pickup test.
- [ ] Enable Keep recent grip attempt reasons, close the guide/HUD, attempt pickup, then reopen the guide. The stored attempt retains its earlier guard, contacts, rig, hand slots and pending request while current status can show UI ownership. Repeat without per-frame duplicates; actor/world changes and explicit Clear retire old history.
- [ ] Prepare the next isolated profile from the closed current played presentation state. Its fresh save-file inventory and every saved byte must match the current source, including progress created after the original prelaunch snapshot.
- [ ] Walk, jog, sprint, turn, crouch physically and by the current upstream binding, pause and recenter. Each action remains responsive with Physicality enabled.
- [ ] Open the palm menu, HUD, inventory, context menu, map, hotbar and Viewpoint settings. Scroll, select, drag, close and return to gameplay without stuck input.
- [ ] Repeat at the desired refresh rate. Record median frame time and visible spikes during a five-minute walk, a stocked room and repeated item transfers.
- [ ] Inspect day/night, rain, snow, indoor lights, flashlight, fire, trees, fences, doors, windows, furniture, zombies, corpses, clothing and carried items. Check material, scale, alignment and stereo depth for each selected model provider.
- [ ] Record actual native model-pack winners. Compare walls, doors and curtains for visual thickness/contact; missing authored geometry is a coverage gap rather than universal compatibility.
- [ ] Test Mug of Coffee in both hands: pickup, held attachment, fluids, drop and reload. Compare original model/attachment receipts in Native controls before judging alignment.
- [ ] Test CordlessPhone and every discovered moveable phone sprite: BeigeRotary location_business_office_generic_02_0; BlackRotary _02_4; RedRotary _02_8; WhiteRotary _02_12; WhiteModern _02_16; BlackModern _02_20; PayPhones walls_decoration_01_39. Record exact identity/provider; microphones and headphones are separate assets.
- [ ] Test phone pickup/carry/full tilt, world contact, drop and ownership. Over-budget, cold or rejected output must retain native fallback, and retired geometry must be excluded before physics stepping.
- [ ] Check near/far authored models in both eyes, then food/water/window glass. Verify shared depth, visible output, native foreground occlusion and no duplicate pass. Test NORTH/WEST windows through intermediate motion, opening, smash, close and Off with matching supported draw/contact.
- [ ] Compare a normal single-player save against a fresh test save. Existing bindings, calibration, inventory and installed mod selection must remain intact.

## Hands and touch sensors

- [ ] Open/close each free hand slowly and quickly. Wrists, arms and item sockets stay aligned without jitter, popping or drift.
- [ ] Rest on and lift from trigger, stick, thumb rest and face buttons separately. Status reports distinct available contacts on supported controllers; touching one control must not report another control as touched.
- [ ] Compare light trigger contact, trigger pressure and squeeze pressure. Touch and analog pressure remain separate signals.
- [ ] Where the selected character rig exposes five separate finger roots, hold trigger pressure steady while varying squeeze, then reverse. Index and the grip fingers must respond independently. On the native grouped rig, inspect its grouped pose behavior instead.
- [ ] Hold a pistol, two-handed weapon, heavy item and a timed-action hand preview. Native grasps and weapon support take priority over free-hand finger estimates.
- [ ] Switch controllers or runtime and repeat. Unavailable contacts retain native poses and normal controls without invented sensor values.
- [ ] Lose tracking briefly, remove headset focus, recenter, pause and restore. No old finger pose, pending action or input press may replay after recovery.

## Floor items, held contacts and world collision

- [ ] Grip a supported loose object with each empty hand while the other hand holds a different object. Only the selected native item moves to the selected hand.
- [ ] Release and gently throw supported objects. Gravity, rotation, release direction and resting position match the motion, with one native world item remaining.
- [ ] Drop asymmetric and concave items onto a table, floor, shelf corner and hollow vessel. Check orientation, supported cavities, non-overlap and stable rest.
- [ ] Move a supported held prop against a table, wall, door, window frame and floor. Check contact feedback, its configured intensity and any supported visual constraint; upstream weapon handling must stay intact.
- [ ] Test a closed door, open door, intact pane, open window, smashed pane and removed glass. Solid visible surfaces block the supported collision path; real apertures remain clear.
- [ ] Check barricades, fences, generated walls and upper-floor/roof edges covered by the selected geometry provider. An unsupported model must not leave an invisible solid proxy in an opening.
- [ ] Change a door/window state while an object rests nearby. Collision updates without stale surfaces or launching the object.
- [ ] Drop, pick up, save and reload the same native item repeatedly. Counts, identity, ownership and persisted orientation remain consistent.
- [ ] Disable physical items while objects are resting, then enable again. Objects remain native objects and do not duplicate, vanish or move unexpectedly.
- [ ] Compare native bathroom/furniture collision footprints against the optional triangle sweep. The native collision path remains active. Record avatar stop/slide separately from physical head/camera offset, which this mode leaves unclamped.
- [ ] For pack-backed loose moveables, identify whether the emitted path is native yaw or the verified full-pose renderer. Tilted/rolling physics must match the actually visible pose. Off, failed, stale or missing full-pose rendering must omit unsupported quaternion simulation while retaining native ownership/drop.

## Food, utensils and water

- [ ] Test bare Bread, BreadSlices, Apple, Pear, Cheese and Carrots in both hands with direct mouth consumption. Native hunger, nutrition and remaining portions change once per confirmed action.
- [ ] Take a portion with a plain Fork or Spoon from a supported edible face, move clear of the food, then bring it to the mouth and confirm. Visible portions follow the utensil and consumption uses the original food item.
- [ ] Touch apple/pear stems, cheese rind, carrot greens and bowl rims. These regions must not produce an edible portion.
- [ ] Scoop upright SoupBowl, SoupBowlClay, StewBowl and StewBowlClay with a Spoon. Forks, inverted bowls and contact outside the exposed surface must not scoop.
- [ ] Finish each bowl. Native Bowl or ClayBowl leftovers appear once and the original food quantities are correct.
- [ ] Lower the food, release confirmation, change held items, cancel the native action or lose focus before completion. No cancelled solid-food action consumes a partial portion.
- [ ] Wear a face covering and try to eat/drink. The covering stays equipped and blocks the interaction.
- [ ] Use sealed food/drink. The original opening recipe is required; Physicality must not bypass it.
- [ ] Tilt an open bottle/cup to drink, then stop mid-action. Native fluid, contamination, capacity and incremental drinking behave as in ordinary gameplay.
- [ ] Touch each calibrated white/beige inset-sink handle with an empty hand and turn the wrist. Exactly one intended valve toggles.
- [ ] Fill an open upright cup under the stream and drink at the calibrated spout. Source depletion, capacity, contamination and mouth confirmation remain native.
- [ ] Leave a stream running without drinking/filling. Cosmetic flow must not silently drain the source.
- [ ] Use a food model without a calibrated utensil profile. Normal native eating remains available and no false geometry or cloned food appears.

## Shelves, storage, backpack and body gear

- [ ] Open cupboard/dresser/shelf storage using native controls. Supported contents use their own models and deterministic layouts; displayed stock decoration is not extra loot.
- [ ] Grip one rendered storage item with each free hand, then return it. Native transfers, capacity and exact item identity remain correct.
- [ ] Compare automatic placement, manual placement, saved layouts and simulated storage poses. An automatic refresh must not overwrite a deliberate or saved pose.
- [ ] Put several asymmetric items in storage. Check scale, rotation, non-overlap and stable rest without resizing them to fit.
- [ ] Draw the real worn backpack from its calibrated back region and return it. The same bag, contents and wearing state survive each transfer.
- [ ] Hold a supported backpack, browse every contents page, take an item with the free hand and return it. No page change may transfer or duplicate an item.
- [ ] Hold the bag-hand trigger for at least 0.7 seconds, then release grip. The same bag suspends at its captured position and remains in native inventory. Grip its original mesh to recall it, then return a contents item on the first release.
- [ ] While a bag is suspended, recenter, lose focus, switch presets and change its native ownership. The old visual retires, the original bag remains accessible through native inventory, and no duplicate or stale grab occurs.
- [ ] Test more than one page, a full bag, nested bags, empty bags and items without available meshes. Native inventory access remains complete and native capacity rules apply.
- [ ] Change bag contents using normal inventory UI while physical contents are visible. Old visual/contact entries retire before they can be grabbed.
- [ ] Draw/store each eligible hotbar item at its occupied body slot. Native attachment compatibility and slot identity remain correct.
- [ ] Adjust hip/back XYZ anchors and reach in Mod Options. Check seated/standing use, both dominant hands and recentering. Controller calibration and unrelated bindings remain saved.
- [ ] Start a foreign timed action while touching body storage or the bag. Physicality must not consume its input or accept its completion as its own transfer.

## Curtains, crafting and ordinary gameplay parity

- [ ] Grip a reachable supported curtain edge and pull laterally. One native curtain action changes the correct curtain once; grazing the window frame does not toggle it.
- [ ] Repeat with no curtain, an obstructed curtain, a changed window, an opening curtain action and loss of focus. No stale request or unintended toggle occurs.
- [ ] Hold Hammer, Saw, Screwdriver, Scissors, Wrench and KitchenKnife in turn. Use the free-hand crafting gesture to open the original recipes-for-tool screen.
- [ ] Craft one available recipe and reject one lacking ingredients/tool/surface. Native requirements, time, item consumption and output must match Native controls.
- [ ] Test building/placing furniture, disassembly and barricading through the original interfaces. Quantities and world mutations occur exactly once.
- [ ] Test bandaging, disinfecting, medicine, clothing/equipment, sleeping/resting and exercise through the native menus/actions. Physicality must not reserve their controls.
- [ ] Test cooking, filling containers, farming, foraging, fishing and reading. Compare native progress, output and quantities with Native controls.
- [ ] Enter/drive/exit a vehicle, operate doors/boot, and return to walking. Physical gestures suspend appropriately inside the vehicle and recover after exit.
- [ ] Test melee, shove/stomp, pistol firing/reload and two-handed support. Hit/action behavior and original controls remain usable while optional contact features are enabled.

## Presets, interruption and persistence

- [ ] Apply Presentation, Comfort and Native controls in game. Status reflects the actual enabled modes, options save, and calibrations/bindings remain intact.
- [ ] Change individual expert options after a preset, save and reload. The chosen values remain editable and persist.
- [ ] Interrupt every physical action with pause, UI/modal opening, tracking loss, focus loss, recenter, save/load, item removal and a changed actor. No delayed action executes on recovery.
- [ ] Repeat twenty pickup/drop, food, storage and body transfers. Check quantities and logs for duplicate callbacks or resources accumulating.
- [ ] Return to the main menu, load another save and restart VR. Each mode initializes cleanly and no previous bag, item, world geometry or sensor record remains active.
- [ ] Open a multiplayer session only to check the guard boundary. The preview's single-player physical actions stay disabled; ordinary upstream multiplayer controls remain available.

## Completion record

Record PASS, FAIL or NOT APPLICABLE for every row, including the applicable model/controller coverage. A full parity result requires all applicable native outcomes, visuals, naturalness, interruptions and persistence checks to pass. A model that is not supplied by the selected providers cannot be counted as a successful 3D/collision test.
