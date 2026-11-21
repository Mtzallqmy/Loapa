# COMBAT FRONTS (جبهات القتال) — INTEGRATION PLAN (v1.1.0)

## Architectural Blueprint: Operation Unified Battlefield
All gameplay systems now communicate through cohesive, authoritative managers owned by `CombatSimulation`:

| Subsystem | Implementation File | Responsibilities |
|---|---|---|
| Central Event Bus | `engine/GameplayEventBus.kt` | Single-dispatch sealed `GameplayEvent` pipeline (damage, fire, reload, enemy alert/defeat, objectives, checkpoints, world interactions, combat phases) |
| Authoritative Damage | `engine/DamageSystem.kt` | Unified player/enemy/fall/hazard damage, armor mitigation, 2.6s respawn protection shield, radial barrel blast resolution |
| Interactive World | `engine/WorldInteractionManager.kt` | Distance + LoS validation, 0.35s input debouncing, dynamic active colliders for doors/gates, switches, ammo/supply crates, destructible barrels, objective terminals |
| Layered Character & Movement | `engine/PlayerController.kt` | 10-state `LowerBodyAnimState` + 8-state `UpperBodyCombatState`, weapon-weight mobility, waist-high cover vaulting, cover leaning/switching, fall damage, reload cancellation, ADS sway |
| Unified Camera | `engine/CameraDirector.kt` | Smooth Left/Right shoulder switching, ADS FOV zoom, cover peek framing, collision-safe raycast boom, reduced-motion compliance |
| Enemy AI 2.0 & Squads | `engine/EnemyAI.kt`, `engine/SquadAndCombatDirector.kt` | 14-state FSM, progressive `PerceptionController` + `AwarenessMemory` (wall-blocked), `SquadCoordinator` cover reservation & staggered attack slots, `CombatDirector` 6-phase pacing & safe wave spawning |
| Mission Verification | `engine/MissionValidator.kt` | Automated bounds, collider-clearance, objective, cover, interactive object, and boss verification across all 4 cities and 12 missions |
| Save System 2.0 | `save/SaveModels.kt`, `save/SaveManager.kt` | Schema v3 atomic writes, `.bak` auto-recovery, seamless v1/v2 → v3 migration |
