# Preview dependencies

The preview targets Windows x64, Project Zomboid 42.21.0 and the game's bundled Java 25 runtime. Obtain game and companion mods from their original providers. They are not redistributed here.

| Dependency | Accepted version | Provider |
|---|---|---|
| Project Zomboid | 42.21.0 | Your Steam installation |
| PZVR | 0.1.2 | [PZVR](https://steamcommunity.com/sharedfiles/filedetails/?id=3816830877) |
| Project Viewpoint | 0.1.6 | [Project Viewpoint](https://steamcommunity.com/sharedfiles/filedetails/?id=3809306528) |
| ZombieBuddy | 2.3.4 | [Official release](https://github.com/zed-0xff/ZombieBuddy/releases/tag/v2.3.4) |
| KK's Shelves | 0.4.7, default storage provider | [KK's Shelves](https://steamcommunity.com/sharedfiles/filedetails/?id=3815931973) |
| Better Window | 0.1.2, optional window/curtain geometry | [Better Window](https://steamcommunity.com/sharedfiles/filedetails/?id=3814787969) |
| Surface Fix | 1.0, audited October 15 payload | [Surface Fix](https://steamcommunity.com/sharedfiles/filedetails/?id=3814398576) |
| Light Fix | 1.4.7, audited October 13 payload | [Light Fix](https://steamcommunity.com/sharedfiles/filedetails/?id=3811344119) |
| True Weathers & Lighting | 3.11.1, optional visual companion | [True Weathers](https://steamcommunity.com/sharedfiles/filedetails/?id=3813908957) |
| Visible Loot | 1.1.1, optional storage provider | [Visible Loot](https://steamcommunity.com/sharedfiles/filedetails/?id=3818189653) |
| 3D Curtains & Blinds | author V2, optional native curtain model provider | [Curtains V2](https://steamcommunity.com/sharedfiles/filedetails/?id=3818873810) |
| Viewpoint Precision FIX | 0.3.0, enhanced29 selection | [Viewpoint Precision FIX](https://steamcommunity.com/sharedfiles/filedetails/?id=3814074009) |
| Project Viewpoint - 3D Doors, Gates & Garages | 0.2.5, enhanced29 exact native door model provider | [Project Viewpoint - 3D Doors, Gates & Garages](https://steamcommunity.com/sharedfiles/filedetails/?id=3818859807) |

PZVR owns OpenXR, movement, turning, menus, native hand/weapon control and physical melee. Viewpoint owns rendering and its model providers. Physicality uses their existing publications and original Zomboid actions. An active compatible PC OpenXR runtime and connected controllers are required for live use. Follow the original providers' installation instructions.

Runtime owner hashes are checked before enabling physical gameplay. A newer Workshop payload with different contracts requires a new reviewed preview. A matching version label alone is insufficient. The published release and source builder preserve those guards.

Game, PZVR, Viewpoint and ZombieBuddy remain required. A present KK Java owner must match its exact audited hash; missing KK does not certify a replacement provider. Storage remains unavailable without the default verified KK route or a configured, recognized Visible Loot route with a current admitted housing. The optional provider settings start Off in ordinary profiles. Curtains V2 support uses only original native curtain objects and their current emitted cloth; decorative hospital/tent/awning pieces do not gain curtain actions.

The base preset retains the existing 25 selected mod IDs. The separately reviewed provider preset adds only ViewpointVisibleLoot and DGVCurtains. The enhanced29 selection appends ViewpointPrecision and BasileDoors3D after the existing packs and Door Fix. Base25 and providers27 remain supported; the planned test profiles use base25 for comparison and enhanced29 for the requested additions. Exact returned native door model, asset and current pose admission is required for authored contacts; native opening, locks and damage remain authoritative. Garages use closed/overhead states, with no continuous smooth-lift claim. Isolated staging preserves the played source, all non-mod-list save bytes, native player databases, controls and approval decisions. Its deliberate destination mod-list changes are recorded with original rollback bytes. New Java approval remains a normal local user action.

Model packs, furniture/window/door fixes, character textures, terrain/wall/clothing textures, weather and other appearance mods remain separate subscriptions. Keep the selected mod set constant when comparing presets. This preview does not claim compatibility with every third-party behavior, camera, UI or weapon replacement.

Java native access is required for the included Rust solver. Follow ZombieBuddy's normal explicit approval flow for the Physicality Java mod. Never bypass approval by copying another user's approval stores.

The solver is an authored JNI bridge using Rapier and other Rust libraries. Third-party notices are embedded in the JAR under META-INF/licenses/rust-physics and included with the public source. The accepted solver SHA-256 is f359aa66469119124fc3b51e4ed4b776bca294d1f4af0ea4a8e99e8cc342af7c.

To rebuild the public Java/Lua source, run BuildPreview.ps1 with JDK 25 and explicit paths to your own matching SDK JARs and the release's solver DLL. It compiles and packages into a fresh temporary directory without launching or installing the game. The resulting personal build needs its own runtime approval and live acceptance; editing the source does not transfer the published regression result to the edited build.
