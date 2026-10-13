# PZVR Physicality preview

> **Preview warning:** Multiplayer support is planned. This preview may currently be unstable or unplayable.

Single-player physical item, food, utensil, storage and body interactions for released PZVR and Project Viewpoint. The extension preserves native item identities, inventory ownership, quantities, recipes and original action completion. PZVR continues to own OpenXR, movement, menus, native hand grasps and melee; Viewpoint continues to own rendering.

This is the standalone Physicality extension. Enable it alongside the original dependencies listed in [DEPENDENCIES.md](DEPENDENCIES.md). The earlier experimental ProjectViewpointVR runtime is a separate project and must not be enabled alongside this preview.

## Install and configure

1. Install the matching game, ZombieBuddy, Viewpoint, PZVR and KK's Shelves from their original providers. Better Window supplies the optional calibrated window/curtain geometry.
2. Download the preview ZIP from Releases and compare its SHA-256 with SHA256SUMS.txt. Extract the PZVRPhysicality folder into your own Zomboid mods directory.
3. Enable PZVRPhysicality in the test save's mod list. Approve its Java mod through ZombieBuddy and restart if requested.
4. Start VR through the upstream PZVR controls. In Mod Options, choose PZVR Gameplay Presets, select Presentation and press Apply. Normal profiles start with the optional modes Off.
5. Use a disposable single-player save for the first test. Run the [live acceptance checklist](LIVE-TEST-CHECKLIST.md), then compare your ordinary gameplay against the Native controls preset.

Presentation enables the supported physical features. Comfort increases body reach and disables storage simulation. Native controls disables optional Physicality interaction adapters. Individual settings remain editable in Mod Options, including mouth contact/confirmation, body reach and XYZ anchors, controller calibration, storage modes, backpack access and contact feedback. Presets preserve calibration and bindings and check the actual native mode state before saving.

The PZVR Presentation options provide explicit demo supplies and feature-status output. Supplies use the native item factory only when the button is pressed; existing items are retained. Wear bags and belts through the normal game actions.

## Interaction coverage

| Area | Preview behavior |
|---|---|
| Loose items | Grip pickup and release/drop through native actions, with captured rigid meshes, compound convex collision, gravity, release motion and persisted rotation. |
| Food and drink | Calibrated mouth approach and confirmation use original eating/drinking actions, nutrition, fluid rules, masks, sealed containers and leftovers. |
| Utensils | Plain Spoon/Fork contact takes a visible portion from calibrated bread, fruit, carrot root or cheese regions. Spoons scoop exposed upright soup/stew surfaces. |
| Storage | Actual rendered contents use native transfer actions, deterministic non-overlap layouts, manual/saved placement and optional storage simulation. |
| Body gear | Draw/return the real worn backpack and draw/store eligible native hotbar items using adjustable body regions and original equip/wear/attachment rules. |
| Backpack contents | Supported held bags, including the native skinned Schoolbag, show up to eight real item models per page. Grab/return uses original transfers and capacity checks. Deliberate suspension keeps the same bag in native inventory at its captured visual position; gripping its original mesh recalls it through native equip. |
| Held prop contacts | Supported single rigid props receive contact feedback and configurable shallow correction of the visible wrist/fingers/socket and prop together. Native rotations, tracking, weapon grasps and active action poses remain authoritative. |
| World contacts | Original generated wall, corner/roof/gable and unambiguous barricade triangles, actual animated door leaves, packed furniture and supported Better Window frames/panes feed the same physical environment. Native state and mesh lineage retire stale geometry. |
| Curtains | Reach a supported cloth edge, grip and pull laterally to dispatch exactly one original curtain action. |
| Crafting | Use the free-hand gesture near a supported held tool to open the original recipes-for-tool screen. Native requirements, duration, ingredients and outputs remain authoritative. |
| Hands | Separate trigger/stick/rest/face contacts and trigger/squeeze pressure where the controller exposes them. Validated five-root rigs use separate index and grip estimates; the stock grouped rig retains its corresponding grouped behavior. Native held grasps take priority. |
| Taps | Calibrated white/beige inset-sink handle motion, visible flow, native filling/drinking and native fluid capacity/contamination. |
| Ordinary gameplay | Native menus/actions remain available for combat, building, medical care, farming, fishing, vehicles, clothing, sleep and other gameplay. Physical interaction adapters defer to modal UI, timed actions and native weapon support. |

Utensil profiles cover Bread, BreadSlices, Apple, Pear, Cheese, Carrots, SoupBowl, SoupBowlClay, StewBowl and StewBowlClay. Edible regions exclude stems, greens, rind and vessel surfaces. Visible utensil portions borrow the original mesh/material; taking a portion does not cut a new hole into the source item's rendered mesh. Drinking remains native incremental consumption. Curtain movement uses the original open/close state, and crafting uses the original recipe interface.

For backpack contents, briefly press and release the bag-hand trigger to change pages. To suspend a supported held bag, hold that trigger for at least 0.7 seconds and release grip. Grip the suspended bag's original mesh to recall it. Focus, actor, ownership or recenter changes retire suspension safely while retaining native inventory. Cloth, lids and loose contents do not have separate simulations.

Collision and visual coverage follows the actual supplied model and geometry providers. Missing or unsupported geometry is omitted and reported rather than represented as an invisible solid object. Character models, textures, weather, terrain and furniture packs remain separate mods. Torso anchors infer orientation from the headset; they are configurable and are not independently tracked waist sensors. Controller-derived finger poses are estimates, not optical joint tracking.

## Verification and live acceptance

The release carries an allowlisted package, exact SDK/library pins, offline regression results and standalone graphics/solver evidence. PUBLIC-READBACK.json identifies the archive and classes without publishing local SDK paths, private assets, saves, logs or configuration. The live checklist establishes headset startup, actual controller behavior, visual quality, naturalness, performance and native gameplay parity.

Physicality's physical actions target single-player. Their multiplayer guards remain enabled. Upstream PZVR multiplayer behavior is separate from this extension's physical transaction coverage.

Publication copy note: This main-branch README was updated after the v0.1.16-preview freeze. The README hash in PUBLIC-READBACK.json refers to the original release copy; release assets and the frozen tag remain unchanged.

## Source and attribution

The authored Java/Lua source is under source. BuildPreview.ps1 recompiles against your own matching SDKs into a fresh temporary directory without launching or installing the game. Personal modified builds need their own approval and verification.

Physicality source is MIT licensed. Rust dependency notices are included in the JAR and source distribution. Original game/Workshop SDKs, meshes, textures, decompiled implementations and upstream repository history are excluded. Credits: PZVR by axizt and GingasVR; Project Viewpoint by its authors; ZombieBuddy by zed-0xff; and the separately installed model/geometry providers linked in DEPENDENCIES.md.

For a bug report, include the preview version, dependency versions, controller/runtime, dominant hand, preset, a minimal reproduction, the relevant console excerpt and a short clip. Exclude credentials, saves and unrelated personal data.
