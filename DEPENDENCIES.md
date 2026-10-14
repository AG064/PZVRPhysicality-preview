# Preview dependencies

The preview targets Windows x64, Project Zomboid 42.21.0 and the game's bundled Java 25 runtime. Obtain game and companion mods from their original providers. They are not redistributed here.

| Dependency | Accepted version | Provider |
|---|---|---|
| Project Zomboid | 42.21.0 | Your Steam installation |
| PZVR | 0.1.2 | [PZVR](https://steamcommunity.com/sharedfiles/filedetails/?id=3816830877) |
| Project Viewpoint | 0.1.6 | [Project Viewpoint](https://steamcommunity.com/sharedfiles/filedetails/?id=3809306528) |
| ZombieBuddy | 2.3.4 | [Official release](https://github.com/zed-0xff/ZombieBuddy/releases/tag/v2.3.4) |
| KK's Shelves | 0.4.6 | [KK's Shelves](https://steamcommunity.com/sharedfiles/filedetails/?id=3815931973) |
| Better Window | 0.1.2, optional window/curtain geometry | [Better Window](https://steamcommunity.com/sharedfiles/filedetails/?id=3814787969) |

PZVR owns OpenXR, movement, turning, menus, native hand/weapon control and physical melee. Viewpoint owns rendering and its model providers. Physicality uses their existing publications and original Zomboid actions. An active compatible PC OpenXR runtime and connected controllers are required for live use. Follow the original providers' installation instructions.

Runtime owner hashes are checked before enabling physical gameplay. A newer Workshop payload with different contracts requires a new reviewed preview. A matching version label alone is insufficient. The published release and source builder preserve those guards.

Model packs, furniture/window/door fixes, character textures, terrain/wall/clothing textures, weather and other appearance mods remain separate subscriptions. Keep the selected mod set constant when comparing presets. This preview does not claim compatibility with every third-party behavior, camera, UI or weapon replacement.

Java native access is required for the included Rust solver. Follow ZombieBuddy's normal explicit approval flow for the Physicality Java mod. Never bypass approval by copying another user's approval stores.

The solver is an authored JNI bridge using Rapier and other Rust libraries. Third-party notices are embedded in the JAR under META-INF/licenses/rust-physics and included with the public source. The accepted solver SHA-256 is f359aa66469119124fc3b51e4ed4b776bca294d1f4af0ea4a8e99e8cc342af7c.

To rebuild the public Java/Lua source, run BuildPreview.ps1 with JDK 25 and explicit paths to your own matching SDK JARs and the release's solver DLL. It compiles and packages into a fresh temporary directory without launching or installing the game. The resulting personal build needs its own runtime approval and live acceptance; editing the source does not transfer the published regression result to the edited build.
