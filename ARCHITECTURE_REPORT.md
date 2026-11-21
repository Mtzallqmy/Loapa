# ARCHITECTURE REPORT — COMBAT FRONTS (جبهات القتال) v2.1.0

**برمجة وتطوير معتز العلقمي – تعز – اليمن – 2026م**

---

## 1. Architecture Decision Record (ADR) — Engine & Language Selection

### Project Classification
- **Mode:** Existing Native Android Repository (`applicationId = "com.example"`).
- **Genre:** Offline 3D Third-Person Military Tactical Shooter (TPS).
- **Target Platform:** Native Android (API 24+ / supports Android 9.0+ ARM64 `arm64-v8a`, 100% offline).

### Engine Selection Decision
Per Universal Game Engineering Policy Section 03 ("Preserve an existing functioning engine unless migration is clearly justified"), this project retains and elevates its **custom native Android 3D Engine (Kotlin + OpenGL ES 2.0/3.0 GLSL + Jetpack Compose Material 3)**.
- **Rationale:**
  1. **Zero external runtime bloat:** Pure native APK (~21 MB) with instant startup and deterministic execution in this Android Gradle build environment (where Unity/Unreal/Godot headless editors are not installed).
  2. **Universal Mobile GPU Compatibility:** Pure OpenGL ES 2.0 shaders (`#version 100`) run reliably across Android 9.0+ ARM64 handsets and software/streaming emulators without Vulkan driver regressions.
  3. **100% Offline Sovereignty:** Zero remote SDKs, zero network permissions (`AndroidManifest.xml` declares only `VIBRATE`), zero WebView wrappers.

### Programming Language Responsibilities
| Language | Subsystem Responsibility | Justification |
|---|---|---|
| **Kotlin (JVM 11 / Android)** | Core simulation (`CombatSimulation`), physics/raycasting (`Math3D`, `PlayerController`, `TacticalVehicle`), Enemy AI 2.0 (`EnemyAI`, `SquadCoordinator`), progression & atomic persistence (`SaveManager`), procedural audio (`TacticalAudioEngine`), and adaptive Material 3 HUD/UI (`GameplayScreen`, `ArmoryScreen`, `CustomizationScreen`, etc.). | Native Android interoperability, strong null/type safety, deterministic JUnit + Robolectric testability, zero JNI marshalling overhead. |
| **GLSL ES 1.00 (`ShaderLibrary.kt`)** | Procedural multi-material shading (asphalt, brick windows, sand, steel ribs, rock), directional sun + hemisphere ambient + exponential atmospheric fog + dynamic muzzle/explosion point light (`uFlashPos`, `uFlashColor`), and sky dome gradient. | Direct GPU execution on Android OpenGL ES 2.0/3.0 hardware. |
| **Kotlin Gradle DSL (`.gradle.kts`)** | Build graph, dependency catalog (`gradle/libs.versions.toml`), resource packaging, and APK signing configuration. | Reproducible native Android builds. |
| **XML (`AndroidManifest.xml`, `res/`)** | Adaptive launcher icons, RTL-aware bilingual strings (`values/` Arabic default, `values-en/` English), backup rules, and immersive landscape activity declaration. | Standard Android platform contract. |

---

## 2. System Architecture & Separation of Concerns

```
MainActivity (Lifecycle + Immersive Landscape + RTL CompositionLocal)
   └── GameViewModel (StateFlow UI State + MobileInputBridge + SaveManager + TacticalAudioEngine)
          ├── UI Layer (Jetpack Compose Material 3)
          │     ├── SplashScreen / MainMenuScreen / GameModesScreen
          │     ├── MissionsScreen / CampaignIntroScreen / MissionResultScreen
          │     ├── ArmoryScreen (360° Live 3D Weapon Preview)
          │     ├── CustomizationScreen (360° Live 3D Character Preview)
          │     ├── AchievementsScreen / SettingsScreen / ControlLayoutEditorScreen / AboutCreditsScreen
          │     └── GameplayScreen (Live GLSurfaceView + Multi-touch HUD + Sniper Scope + Kill Feed + Onboarding)
          │
          ├── Rendering Layer (OpenGL ES 2.0 Thread, synchronized via sim.simLock)
          │     ├── CombatGLRenderer (Sky, City LOD, Rover, Articulated Soldiers, Sniper Lasers, Grenades, Explosions, Weather, 3D->2D HUD Projections)
          │     ├── PreviewGLRenderer (360° Turntable for Armory & Character Studio)
          │     ├── MeshFactory (Outward-wound Cube, Cylinder, Sphere, Articulated Soldier Rig)
          │     └── ShaderLibrary (World PBR-lite + Dynamic Flash Point Light + Sky Gradient)
          │
          ├── Unified Simulation Engine (Deterministic Fixed-Clamped Timestep)
          │     ├── CombatSimulation (Authoritative Orchestrator, 4 Game Modes, Grenades, Medkits, Smart Auto-Fire)
          │     ├── PlayerController (Movement, Sprint, Crouch, Jump, Cover, Vault, Weapon Handling, Equipment)
          │     ├── TacticalVehicle (Drivable Armored Rover, Turret Ballistics, Collision, Wheel Spin)
          │     ├── EnemyAI + SquadCoordinator (FSM + Flank/Suppress/Cover/Retreat + Weather Vision Scaling)
          │     ├── DamageSystem + WorldInteractionManager (HitZones, Barrels, Switches, Crates, Objectives)
          │     ├── CameraDirector (Left/Right Shoulder Swap, ADS Zoom, Vehicle Follow, Shake Scaling)
          │     ├── WeatherSystem (Clear Day, Rain, Fog, Sandstorm, Night Ops)
          │     ├── CityBuilder (4 Distinct Cities with AABB Colliders, Cover Points, Interactive Props)
          │     ├── CombatDirector + PerformanceManager (Adaptive LOD & Particle/Tracer Budgets)
          │     └── GameplayEventBus (Decoupled Simulation Event Stream)
          │
          ├── Audio Layer (Offline Procedural PCM Synthesizer)
          │     └── TacticalAudioEngine (Polyphonic AudioTrack Pool + Dynamic Menu/Combat Background Music Thread)
          │
          └── Persistence Layer (Versioned Atomic Local Storage)
                └── SaveManager (Schema v4 JSON, Atomic Temp-File Rename, Legacy v1–v3 Migration, Corrupt-File Fallback)
```

---

## 3. Concurrency, Thread Safety & Memory Budgets
- **GL Render Thread ↔ UI Thread Synchronization:** All `CombatSimulation` mutations and OpenGL reads are guarded by `sim.simLock`. Callbacks (`onAudioEvents`, `onHudUpdate`) execute strictly outside `simLock` to prevent lock inversion.
- **Lock-Free Input Bridge:** `MobileInputBridge` transfers analog joystick vectors, multi-touch look deltas, and latched one-shot triggers (fire, aim, reload, jump, cover, vault, interact, grenade, medkit, shoulder toggle) cleanly between Compose pointer events and GL frames.
- **Bounded Real-Time Pools (`PerformanceManager`):**
  - Low / Medium / High adaptive detail tiers (`detailTier 1..3`) with distance culling.
  - Strictly capped active tracers, impact particles, and weather particles (max 72 weather particles) to avoid GC spikes during sustained firefights.
- **Lifecycle Resilience:** `MainActivity.onPause()` pauses both simulation and dynamic audio (`viewModel.onAppPaused()`), resets touch inputs, and preserves the EGL context; `onResume()` restores audio cleanly.

---

## 4. Security & Privacy Posture
- **Minimal Permissions:** Only `android.permission.VIBRATE` is requested; `INTERNET` is intentionally absent.
- **No Hardcoded Secrets or External Code:** Zero network calls, zero dynamic code loading, zero third-party ad/tracking SDKs.
- **Data Integrity:** `SaveManager` writes atomically (`save.tmp` -> rename) and validates schema bounds on load, safely migrating v1/v2/v3 saves to v4 without erasing player progress.

---

## 5. Existing Project Component Audit Summary
| Subsystem | Audit Status | Notes |
|---|---|---|
| Core Simulation & Physics | **WORKING** | Verified across 600-tick stress test, 0 NaNs, 0 exceptions. |
| 4 Cities & 12 Campaign Missions | **WORKING** | Clear unobstructed spawns verified for Player, Rover, and Defense Core across all 4 cities. |
| 4 Playable Modes (Campaign, Survival, Defense, Challenges) | **WORKING** | Headshot challenge soft-lock prevention verified via unit test. |
| 8 Weapons + Attachments + 360° Preview | **WORKING** | Live stat deltas (mag, range, recoil, suppressor acoustics) verified. |
| Tactical Equipment (Grenades, Medkits) & Smart Auto-Fire | **WORKING** | Integrated end-to-end with HUD, Settings persistence, and regression tests. |
| OpenGL ES 2.0 Renderer + Dynamic Flash Light + Sniper Scope | **WORKING** | Verified compilation & uniform binding; visual appearance on physical device marked NOT VERIFIED in cloud environment. |
| Procedural Audio + Dynamic Background Music | **WORKING** | Polyphonic burst test verified non-blocking (<1.5s for 60 bursts). |
| Save/Load & Migration (Schema v4) | **WORKING** | Atomic save, checkpoint resume, and legacy migration verified. |
| Physical Device / Emulator Manual QA | **NOT VERIFIED** | Cloud build environment has no ADB or physical Android handset attached. |
