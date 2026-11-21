# Changelog — COMBAT FRONTS (جبهات القتال)

## [2.1.0] — Operation: Total Game Completion (`versionCode = 3`)
### Added
- **4 Complete Playable Game Modes (`GameModesScreen`, `GameModeCatalog`, `CombatSimulation`):**
  - **Campaign (`حملة القتال`):** All 12 missions across 4 cities preserved with per-mission dynamic weather presets.
  - **Survival (`البقاء`):** Endless escalating enemy waves with between-wave health/ammo resupply, score multiplier, and per-city best wave/score records.
  - **Defense (`الدفاع`):** 5 escalating enemy waves assaulting a 3D Command Core (400 HP) with live AI Core targeting, Core HP HUD bar, and custom Core damage audio cues.
  - **Challenges (`التحديات`):** 4 timed tactical trials across all 4 cities (`chal_river_marksman`, `chal_mirage_blitz`, `chal_harbor_ironclad`, `chal_mountain_gauntlet`) with live countdown timers and Bronze/Silver/Gold medals.
- **Dynamic Weather System (`WeatherSystem.kt`):** 7 selectable weather conditions (`DAYLIGHT`, `SUNSET`, `NIGHT`, `RAIN`, `FOG`, `DUST`, `SANDSTORM`) with pooled 3D rain/dust/sandstorm particles, dynamic sky/fog/sun lighting overrides, ambient weather audio pulses, and live Enemy AI vision-range scaling (`0.50x`–`1.00x`).
- **Playable Tactical Armored Rover (`TacticalVehicle.kt`):** Parked in unobstructed zones across all 4 cities with context-sensitive boarding/exiting, joystick driving & steering physics with AABB collision sliding, 280 HP hull armor, rotating roof autocannon turret with custom PCM gunfire SFX, dedicated chase camera, and safe ejection on destruction.
- **Adaptive Performance Manager (`PerformanceManager.kt`):** EMA frame-time tracking, automatic LOD tier adjustment, and strict caps on tracers, impact particles, and weather particles.
- **Save Schema v4 (`SaveModels.kt`, `SaveManager.kt`):** Persists Survival best waves/scores, Defense victories/best waves, and Challenge scores/medals while losslessly migrating legacy v1, v2, and v3 saves.
- **End-to-End & Stress Test Suite (`EndToEndAndStressTest.kt`):** Added 7 comprehensive Robolectric tests covering Workflows A–E, Weather & Vehicle integration, and a 600-tick stress test, plus a Compose UI test for `GameModesScreen` (**26/26 tests passing**).
- **Packaging & Audit Deliverables:** Verified APK (`/COMBAT-FRONTS-v2.1.0-ARM64.apk`), clean source archive (`/COMBAT-FRONTS-v2.1.0-SOURCE.zip`), and all 14 Prompt 05 engineering reports.

### Fixed
- Eliminated redundant null-check Kotlin compiler warning in `EnemyAI.kt` (zero compiler warnings).
- Added exhaustive `ObjectiveKind.DEFEND_CORE` handling across simulation, HUD, and test suites.

---

## [1.1.0] — Operation: Unified Battlefield
### Fixed (Critical Crash & Icon Repair)
- Deleted 5 corrupted 0-byte `.jpg` files in `res/drawable/` that caused immediate startup crashes on `SplashScreen` and broken adaptive icons.
- Replaced raster artwork with resolution-independent Compose Canvas artwork (`TacticalArtwork.kt`) for the emblem, main menu hero backdrop, and all 4 cities.
- Rebuilt adaptive launcher icon (`ic_launcher_foreground.xml` vector + `ic_launcher_background.xml` gradient), removed invalid `<monochrome>` raster tag, and regenerated valid PNG32 legacy mipmaps (`mdpi`–`xxxhdpi`).
- Restored `applicationId = "com.example"` for streaming emulator launch compatibility and hardened `GLSurfaceView` EGL setup.

### Added (Operation: Unified Battlefield)
- **Unified Core Architecture:** `GameplayEventBus`, authoritative `DamageSystem`, `WorldInteractionManager`, `SquadCoordinator`, `CombatDirector`, `CameraDirector`, and `MissionValidator`.
- **Advanced Movement & Layered Animation:** 10-state `LowerBodyAnimState` + 8-state `UpperBodyCombatState`, weapon-weight mobility, waist-high obstacle vaulting, cover leaning/switching, fall damage, reload cancellation on sprint/vault/weapon-switch, ADS weapon sway, and 2.6s checkpoint respawn shield.
- **Enemy AI 2.0:** 14-state tactical FSM, wall-blocked progressive perception & awareness memory, non-stacking cover reservation, and difficulty-scaled staggered attack slot tokens.
- **Interactive Worlds Across All 4 Cities:** Tactical doors, locked switch-controlled security gates, supply/ammo crates, destructible explosive barrels, and objective terminals with context-sensitive HUD prompts.
- **Camera & HUD Upgrades:** Left/Right shoulder switching, Combat Phase badge, Respawn Shield indicator, Vault and Interact touch buttons.
- **Save System 2.0:** Schema v3 with seamless migration from v1/v2 saves and persisted camera shoulder preference.
- **Expanded Verification:** Added `UnifiedBattlefieldRegressionTest` (6 cross-system suites) and Robolectric `MainActivity` + launcher icon smoke test (18/18 passing).

---

## [1.0.0] — Operation: Next Level
- Initial full offline 3D native Android release: 4 cities, 12 missions, 8 customizable weapons, 360° Armory & Character Studio, procedural PCM audio, Arabic RTL / English LTR UI, and atomic offline SaveManager.
