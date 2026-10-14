# PZVR Physicality preview

Single-player physical item, food, utensil, storage and body interactions for released PZVR and Project Viewpoint. The extension preserves native item identities, inventory ownership, quantities, recipes and original action completion. PZVR continues to own OpenXR, movement, menus, native hand grasps and melee; Viewpoint continues to own rendering.

This is the standalone Physicality extension. Enable it alongside the original dependencies listed in [DEPENDENCIES.md](DEPENDENCIES.md). The earlier experimental ProjectViewpointVR runtime is a separate project and must not be enabled alongside this preview.

## Install and configure

1. Install the matching game, ZombieBuddy, Viewpoint, PZVR and KK's Shelves from their original providers. Better Window supplies the optional calibrated window/curtain geometry.
2. Download the preview ZIP from Releases and compare its SHA-256 with SHA256SUMS.txt. Extract the PZVRPhysicality folder into your own Zomboid mods directory.
3. Enable PZVRPhysicality in the test save's mod list. Approve its Java mod through ZombieBuddy and restart if requested.
4. Start VR through the upstream PZVR controls. In Mod Options, choose PZVR Gameplay Presets, select Presentation and press Apply. Normal profiles start with the optional modes Off.
5. Use a disposable single-player save for the first test. Run the [live acceptance checklist](LIVE-TEST-CHECKLIST.md), then compare your ordinary gameplay against the Native controls preset.

Presentation enables the supported physical features. Comfort increases body reach, disables storage simulation and keeps room-scale body movement native. Native controls disables optional Physicality interaction adapters. Individual settings remain editable in Mod Options, including mouth contact/confirmation, body reach and XYZ anchors, controller calibration, storage modes, backpack access, contact feedback and player collision. Presets preserve calibration and bindings and check the actual native mode state before saving.

Open controls and live status on the PZVR Presentation page shows the native game guide with current mode, tracking/input guards, touch availability and geometry status. The separate presentation profile opens it once at world start. Ordinary profiles leave automatic guide, backpack preparation and indexed-model repair Off. The guide can always be reopened from Mod Options.

The 0.1.18 candidate targets the exact PZVR 0.1.2 payload. Native grabs, consumed grips and physical bandaging retain per-hand action authority; a fresh neutral grip/trigger frame is required before an authored action can resume. Current native binding mirroring controls logical grip and trigger routes independently of aim preference. Actor, world, focus, pause, UI, timed-action and weapon guards remain active.

PZVR Presentation also offers native Sit on ground, Give and wear one test schoolbag, and idempotent demo supplies. Schoolbag setup adds one missing original bag and queues the native wear action only for an empty back slot; ownership and the worn slot are checked again at completion. A different worn backpack stays equipped. The separate presentation profile enables one startup attempt, while applying a preset never grants or equips items. Supplies retain existing native items and contents. Belts use the normal inventory wear action. These helpers require an idle living single-player actor outside a vehicle.

Record actual held model placement on the same page enables bounded native item/model/attachment and final rendered-placement receipts. This independent diagnostic retains its selected value when switching Gameplay presets, so Native controls and physical modes can be compared. It changes neither hand poses nor inventory. The guide shows capture availability and rejection reasons.

Logical native hand slots are shown even with detailed model recording Off. An invisible held item still occupies its original slot. Keep recent grip attempt reasons retains eight raw press snapshots with the guard, contact/rig publication, hand slots and pending request state. Opening the guide or HUD preserves those snapshots, so a failed pickup can be inspected afterward. UI-owned presses have separate labels; actor/cell changes, world exit and explicit Clear reset the history. A recorded request identifies an attempted operation and does not prove its completion.

## Interaction coverage

| Area | Preview behavior |
|---|---|
| Loose items | Grip pickup and release/drop through native actions, with captured rigid meshes, compound convex collision, gravity, release motion and persisted rotation. |
| Food and drink | Calibrated mouth approach and confirmation use original eating/drinking actions, nutrition, fluid rules, masks, sealed containers and leftovers. |
| Utensils | Plain Spoon/Fork contact takes a visible portion from calibrated bread, fruit, carrot root or cheese regions. Spoons scoop exposed upright soup/stew surfaces. |
| Storage | Select a reachable container once through the native loot inventory, close the HUD, then grip its actual rendered contents. Native transfer actions, deterministic non-overlap layouts, manual/saved placement and optional storage simulation retain native ownership. Missing geometry uses native inventory. |
| Body gear | Draw/return the real worn backpack and draw/store eligible native hotbar items using adjustable body regions and original equip/wear/attachment rules. |
| Backpack contents | Supported held bags, including the native skinned Schoolbag, show up to eight real item models per page. Grab/return uses original transfers and capacity checks. Deliberate suspension keeps the same bag in native inventory at its captured visual position; gripping its original mesh recalls it through native equip. |
| Held prop contacts | Supported single rigid props receive contact feedback and configurable shallow correction of the visible wrist/fingers/socket and prop together. Native rotations, tracking, weapon grasps and active action poses remain authoritative. |
| World contacts | Original generated wall, corner/roof/gable and unambiguous barricade triangles, actual animated door leaves, packed furniture and supported Better Window frames/panes feed the same physical environment. Native state and mesh lineage retire stale geometry. |
| Player collision | The optional adapter clips avatar movement against actual supported captured surfaces before the original native collision path. Presentation includes room-scale body movement; Comfort leaves that part native. Headset pose and residual head lean retain upstream tracking. Missing/stale geometry uses native movement. |
| World visual feedback | A separate mode provides supported emitted counter-sink/faucet geometry and feedback with exact model lineage and a recent rendered receipt. Supported bathroom sink/counter movement receives scoped native footprint refinement only after current exact geometry is admitted; unsupported objects keep the original footprint. |
| Ground moveable models | Exact selected pack bindings use the admitted full-pose renderer and contact path for supported ground and held moveables. Per-item current draw admission is required before native icons or proxy simulation are replaced; cold, omitted or unsupported items retain native rendering and ownership. |
| Window sash and curtains | Measured supported sash pieces use the same progressed visual and contact state. Omitted, budget-limited or invalid draws restore native static geometry. Curtains retain one original open/close action from a deliberate cloth-edge pull. |
| Crafting | Use the free-hand gesture near a supported held tool to open the original recipes-for-tool screen. Native requirements, duration, ingredients and outputs remain authoritative. |
| Hands | Separate trigger/stick/rest/face contacts and trigger/squeeze pressure where the controller exposes them. Validated five-root rigs use separate index and grip estimates; the stock grouped rig retains its corresponding grouped behavior. Native held grasps take priority. The separate held fallback captures exact native CordlessPhone/FBX geometry and final placement; unavailable native contracts stay native. |
| Taps | Calibrated white/beige inset-sink handle motion, visible flow, native filling/drinking and native fluid capacity/contamination. |
| Ordinary gameplay | Native menus/actions remain available for combat, building, medical care, farming, fishing, vehicles, clothing, sleep and other gameplay. Physical interaction adapters defer to modal UI, timed actions and native weapon support. |

Utensil profiles cover Bread, BreadSlices, Apple, Pear, Cheese, Carrots, SoupBowl, SoupBowlClay, StewBowl and StewBowlClay. Edible regions exclude stems, greens, rind and vessel surfaces. Visible utensil portions borrow the original mesh/material; taking a portion does not cut a new hole into the source item's rendered mesh. Drinking remains native incremental consumption. Curtain movement uses the original open/close state, and crafting uses the original recipe interface.

For backpack contents, briefly press and release the bag-hand trigger to change pages. To suspend a supported held bag, hold that trigger for at least 0.7 seconds and release grip. Grip the suspended bag's original mesh to recall it. Focus, actor, ownership or recenter changes retire suspension safely while retaining native inventory. Cloth, lids and loose contents do not have separate simulations.

Collision and visual coverage follows the actual supplied model and geometry providers. Missing or unsupported geometry is omitted and reported rather than represented as an invisible solid object. Character models, textures, weather, terrain and furniture packs remain separate mods. Torso anchors infer orientation from the headset; they are configurable and are not independently tracked waist sensors. Controller-derived finger poses are estimates, not optical joint tracking.

## Verification and live acceptance

The release carries an allowlisted package, exact SDK/library pins, offline regression results and standalone graphics/solver evidence. PUBLIC-READBACK.json identifies the archive and classes without publishing local SDK paths, private assets, saves, logs or configuration. The live checklist establishes headset startup, actual controller behavior, visual quality, naturalness, performance and native gameplay parity.

Physicality's physical actions target single-player. Their multiplayer guards remain enabled. Upstream PZVR multiplayer behavior is separate from this extension's physical transaction coverage.

## Source and attribution

The authored Java/Lua source is under source. BuildPreview.ps1 recompiles against your own matching SDKs into a fresh temporary directory without launching or installing the game. TestPreview.ps1 runs the small pure policy subset. TestNativeHeld.ps1 includes the authored native-held fixture/typed-ABI gate using your matching SDKs and compiled production classes. TestInputOwnership.ps1 checks original input ordering through copied bytecode and executes the ownership, touch and finger policies against authored providers. These checks do not reproduce the private asset or whole release GPU gate. Personal modified builds need their own approval and verification.

Physicality source is MIT licensed. Rust dependency notices are included in the JAR and source distribution. Original game/Workshop SDKs, meshes, textures, decompiled implementations and upstream repository history are excluded. Credits: PZVR by axizt and GingasVR; Project Viewpoint by its authors; ZombieBuddy by zed-0xff; and the separately installed model/geometry providers linked in DEPENDENCIES.md.

For a bug report, include the preview version, dependency versions, controller/runtime, aim hand, native binding mirroring, preset, a minimal reproduction, the relevant console excerpt and a short clip. Exclude credentials, saves and unrelated personal data.
