# Requirements Traceability Matrix — COMBAT FRONTS (جبهات القتال) v2.1.0

**Version:** `2.1.0` (`versionCode = 3`)  
**Package / Application ID:** `com.example`  
**Attribution:** `برمجة وتطوير معتز العلقمي – تعز – اليمن – 2026م`

| Req ID | Requirement (Prompts 01–05) | Implementing Files | Verification Evidence | Status |
| :--- | :--- | :--- | :--- | :--- |
| **REQ-01** | 100% Offline Single-Player (Zero `INTERNET` permission) | `app/src/main/AndroidManifest.xml` | Manifest inspection (only `VIBRATE` declared); offline SaveManager tests | **VERIFIED** |
| **REQ-02** | Arabic RTL Primary + English LTR Secondary + Developer Credit | `localization/GameStrings.kt`, `MainActivity.kt`, `ui/*.kt` | `SaveAndUiRobolectricTest` (Splash, Main Menu, Game Modes, Credit) | **VERIFIED** |
| **REQ-03** | Landscape Mobile 3D Shooter with Over-the-Shoulder Camera | `AndroidManifest.xml` (`sensorLandscape`), `render/CombatGLRenderer.kt`, `engine/CameraDirector.kt` | `CombatFrontsEngineTest`, `UnifiedBattlefieldRegressionTest` | **VERIFIED** |
| **REQ-04** | 4 Distinct Fictional Cities (River, Mirage, Harbor, Mountain) | `engine/CityBuilder.kt`, `data/MissionCatalog.kt`, `ui/TacticalArtwork.kt` | `CombatFrontsEngineTest`, `EndToEndAndStressTest` | **VERIFIED** |
| **REQ-05** | 12 Complete Campaign Missions with Boss Encounters | `data/MissionCatalog.kt`, `engine/MissionValidator.kt`, `engine/CombatSimulation.kt` | `CombatFrontsEngineTest` (validates all 12 missions), `EndToEndAndStressTest` (Workflow A) | **VERIFIED** |
| **REQ-06** | 8 Distinct Weapons + 6 Attachment/Skin Slots | `data/WeaponCatalog.kt`, `ui/ArmoryAndCharacterScreens.kt` | `CombatFrontsEngineTest`, `EndToEndAndStressTest` (Workflow B live combat stats) | **VERIFIED** |
| **REQ-07** | Character Gear Studio (6 Equipment Slots) | `data/CharacterCatalog.kt`, `ui/ArmoryAndCharacterScreens.kt` | `EndToEndAndStressTest` (Workflow C persistence) | **VERIFIED** |
| **REQ-08** | 4 Playable Game Modes (`حملة القتال`, `البقاء`, `الدفاع`, `التحديات`) | `data/GameModeCatalog.kt`, `ui/GameModesScreen.kt`, `engine/CombatSimulation.kt`, `ui/GameViewModel.kt` | `EndToEndAndStressTest` (Workflow D), `SaveAndUiRobolectricTest` (Compose UI) | **VERIFIED** |
| **REQ-09** | Dynamic Weather System (7 Conditions, 3D Particles, AI Vision) | `engine/WeatherSystem.kt`, `render/CombatGLRenderer.kt`, `engine/EnemyAI.kt`, `audio/TacticalAudioEngine.kt` | `EndToEndAndStressTest` (`weatherAndTacticalVehicleIntegrationWorksAcrossCities`) | **VERIFIED** |
| **REQ-10** | Playable Ground Vehicle (Tactical Armored Rover) | `engine/TacticalVehicle.kt`, `engine/CityBuilder.kt`, `engine/WorldInteractionManager.kt`, `render/CombatGLRenderer.kt` | `EndToEndAndStressTest` (board, drive, roof turret, damage, exit/ejection) | **VERIFIED** |
| **REQ-11** | Unified Combat AI 2.0 (14 States, Cover, Staggered Firing, Core Assault) | `engine/EnemyAI.kt`, `engine/SquadCoordinator.kt` | `UnifiedBattlefieldRegressionTest`, `EndToEndAndStressTest` | **VERIFIED** |
| **REQ-12** | Adaptive Performance & Bounded Particle Pools | `engine/PerformanceManager.kt`, `engine/CombatSimulation.kt`, `render/CombatGLRenderer.kt` | `EndToEndAndStressTest` (600-tick stress test: 0 exceptions, 0 NaN, bounded pools) | **VERIFIED_JVM_ROBOLECTRIC** |
| **REQ-13** | Atomic Save System Schema v4 + v1/v2/v3 Migration + Mid-Mission Recovery | `save/SaveModels.kt`, `save/SaveManager.kt` | `SaveAndUiRobolectricTest`, `EndToEndAndStressTest` (Workflow E) | **VERIFIED** |
| **REQ-14** | Procedural Offline Tactical Audio (Weapons, Turret, Weather, Core) | `audio/TacticalAudioEngine.kt` | Unit tests & build verification (hardware speaker verification pending) | **VERIFIED_JVM_ROBOLECTRIC** |
| **REQ-15** | Custom Adaptive Launcher Icon & Zero Broken Resources | `res/drawable/ic_launcher_*.xml`, `res/mipmap-*/ic_launcher*.png` | `SaveAndUiRobolectricTest` (launcher icon load test), `assembleDebug` | **VERIFIED** |
| **REQ-16** | Verified ARM64-Compatible APK & Clean Source Archive | `/COMBAT-FRONTS-v2.1.0-ARM64.apk`, `/COMBAT-FRONTS-v2.1.0-SOURCE.zip` | `gradle :app:assembleDebug`, `unzip -l`, `sha256sum` | **VERIFIED (Debug Signed)** |
| **REQ-17** | Physical ARM64 Android Device 30+ FPS & Thermal Soak Test | Runtime on physical Android 9.0+ hardware | No physical handset / ADB in cloud container | **PENDING_PHYSICAL_DEVICE** |
| **REQ-18** | Godot 4 / Blender Asset Pipeline | N/A in Android SDK container -> Native Kotlin + OpenGL ES 3.0 equivalent | Documented transparently across audit & build reports | **ADAPTED_NATIVE_ANDROID** |
