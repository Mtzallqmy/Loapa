# Recovery Backlog — COMBAT FRONTS (جبهات القتال) v2.1.0

**Audit Baseline:** Post-Phase 03 (v1.1.0) -> Target v2.1.0  
**Status Legend:** `CLOSED` = Implemented & verified by build/tests | `DEFERRED_ENV` = Blocked solely by cloud container environment limitations (no hardware device / release keystore).

| Ticket | Priority | Item Description | Resolution / Closure Evidence | Status |
| :--- | :--- | :--- | :--- | :--- |
| **REC-001** | **P0** | Implement Survival Mode (`البقاء`) with endless escalating waves, resupply, and record saving | Added `GameMode.SURVIVAL` in `CombatSimulation.kt`, `GameModeCatalog.kt`, `SaveModels.kt` (v4), verified in `EndToEndAndStressTest.workflowD`. | **CLOSED** |
| **REC-002** | **P0** | Implement Defense Mode (`الدفاع`) with 5-wave 3D Command Core assault & AI targeting | Added `ObjectiveKind.DEFEND_CORE`, `defenseCoreHealth`, `EnemyAI` core targeting, 3D core mesh in `CombatGLRenderer.kt`, HUD Core HP bar, verified in `workflowD`. | **CLOSED** |
| **REC-003** | **P0** | Implement Challenges Mode (`التحديات`) with 4 timed trials & Bronze/Silver/Gold medals | Added 4 city challenges in `GameModeCatalog.kt`, timer & medal evaluation in `CombatSimulation.kt`, `GameModesScreen.kt`, verified in `workflowD`. | **CLOSED** |
| **REC-004** | **P0** | Build 4-Mode Selection Hub UI with Arabic RTL & English LTR support | Created `ui/GameModesScreen.kt` wired into `MainActivity.kt` and `GameViewModel.kt`; verified by Robolectric Compose test in `SaveAndUiRobolectricTest`. | **CLOSED** |
| **REC-005** | **P0** | Upgrade Save Schema v3 -> v4 with lossless v1/v2/v3 migration | Updated `SaveModels.kt` & `SaveManager.kt` (`SAVE_SCHEMA_VERSION = 4`); verified in `EndToEndAndStressTest.workflowE`. | **CLOSED** |
| **REC-006** | **P0** | Implement Dynamic Weather System (7 conditions, particles, fog/lighting, AI vision, audio) | Created `engine/WeatherSystem.kt`, integrated into `CombatGLRenderer.kt`, `EnemyAI.kt`, `TacticalAudioEngine.kt`; verified in `EndToEndAndStressTest`. | **CLOSED** |
| **REC-007** | **P1** | Implement Scoped Playable Ground Vehicle (Tactical Armored Rover) across all 4 cities | Created `engine/TacticalVehicle.kt`, added `vehicleSpawnPos` in `CityBuilder.kt`, boarding in `WorldInteractionManager.kt`, chase cam in `CameraDirector.kt`, 3D vehicle in `CombatGLRenderer.kt`. | **CLOSED** |
| **REC-008** | **P1** | Add `PerformanceManager` with EMA frame-time budget, adaptive LOD, and bounded pool caps | Created `engine/PerformanceManager.kt`, integrated into `CombatSimulation.kt` & `CombatGLRenderer.kt`; verified in 600-tick stress test. | **CLOSED** |
| **REC-009** | **P0** | Add Automated End-to-End Workflows A–E & 600-Tick Stress Test | Created `EndToEndAndStressTest.kt` (7 tests) + `GameModesScreen` Compose UI test; full suite **26/26 PASSED**. | **CLOSED** |
| **REC-010** | **P1** | Eliminate Kotlin compiler warning in `EnemyAI.kt:353` | Refactored `defenseCorePos` null-check in `EnemyAI.kt`; zero compiler warnings. | **CLOSED** |
| **REC-011** | **P0** | Package verified ARM64-compatible APK (`COMBAT-FRONTS-v2.1.0-ARM64.apk`) & clean Source ZIP | Built via `gradle :app:assembleDebug`, SHA-256 verified, and zipped clean source tree. | **CLOSED** |
| **REC-012** | **P2** | Real-hardware ARM64 FPS/thermal profiling & Play Store upload keystore signing | Requires physical Android 9.0+ device with ADB and developer's private release keystore outside cloud container. | **DEFERRED_ENV** |
