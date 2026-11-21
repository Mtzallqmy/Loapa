# Asset Licenses & Provenance — COMBAT FRONTS (جبهات القتال) v2.1.0

**Copyright / Developer Credit:** `برمجة وتطوير معتز العلقمي – تعز – اليمن – 2026م`  
**Third-Party Copyrighted Art/Audio Packs Used:** **NONE (0)**

---

## 1. Original Procedural Assets (100% Owned by Project)

All visual, 3D, UI, and audio assets bundled in COMBAT FRONTS v2.1.0 are generated procedurally in source code or authored specifically for this project:

| Asset Category | Implementation Location | License / Ownership |
| :--- | :--- | :--- |
| **3D Geometry (Cities, Characters, 8 Weapons, Tactical Rover, Command Core, Props)** | `render/CombatGLRenderer.kt`, `engine/CityBuilder.kt` | Original procedural OpenGL ES 3.0 vertex buffers authored in-project. |
| **GLSL ES 3.0 Shaders (Lighting, Fog, Weather, Muzzle Flash)** | `render/CombatGLRenderer.kt` | Original GLSL shaders authored in-project. |
| **UI Artwork (Emblem, Hero Backdrop, 4 City Illustrations)** | `ui/TacticalArtwork.kt` | Original resolution-independent Jetpack Compose Canvas artwork. |
| **Adaptive & Legacy Launcher Icons** | `res/drawable/ic_launcher_*.xml`, `res/mipmap-*/ic_launcher*.png` | Original custom tactical vector adaptive icon and rendered PNG32 mipmaps. |
| **Tactical Audio & SFX (Gunshots, Reloads, Footsteps, Explosions, Turret, Weather, Core)** | `audio/TacticalAudioEngine.kt` | Original real-time procedural PCM audio synthesis via Android `AudioTrack`. |
| **Localization Strings (Arabic RTL & English LTR)** | `localization/GameStrings.kt`, `res/values/strings.xml` | Original bilingual copy authored in-project. |

---

## 2. Open-Source Libraries (Standard Android / Jetpack Dependencies)

All runtime libraries are standard Apache-2.0 licensed Android/Jetpack/Kotlin dependencies resolved from official Google Maven and Maven Central repositories via `gradle/libs.versions.toml`:
- Kotlin Standard Library & Coroutines (`Apache-2.0`)
- AndroidX Core, Lifecycle, Activity Compose, Jetpack Compose Material 3 & Material Icons (`Apache-2.0`)
- JUnit 4 (`EPL-1.0`) & Robolectric (`MIT` / `Apache-2.0`) — test-only dependencies (not packaged in release APK).
