# COMBAT FRONTS (جبهات القتال) — AI BEHAVIOR TESTS (v1.1.0)

## Enemy AI 2.0 Verification Matrix

| Behavior / Invariant | Verification Method | Result |
|---|---|---|
| 14-State Tactical FSM (`IDLE`, `PATROL`, `SUSPICIOUS`, `INVESTIGATE`, `ALERT`, `SEARCH`, `TAKE_COVER`, `ENGAGE`, `RELOAD`, `REPOSITION`, `FLANK`, `RETREAT`, `REQUEST_SUPPORT`, `DEFEATED`) | `EnemyAI.kt` state transitions + `UnifiedBattlefieldRegressionTest` | PASS |
| Wall-Blocked Progressive Perception (Never Sees Through Walls) | Placed enemy behind solid River City concrete building (`z=31` vs player `z=12`), stepped 15 ticks; verified `awareness == 0f` | PASS |
| Squad Cover Reservation (Zero Cover Stacking) | `SquadCoordinator.reserveCoverFor()` called for two nearby enemies; asserted distinct `CoverPoint` assignments | PASS |
| Staggered Firing Attack Slots | Requested attack slots for 4 enemies on `Difficulty.EASY` (cap = 2); verified exactly 2 granted simultaneously | PASS |
| Hitbox Multipliers & Frontal Ballistic Shield | Verified `HitZone.HEAD` > `HitZone.LIMB` damage and `SHIELD_DEFENDER` frontal raycast returns `HitZone.SHIELD` | PASS |
| Safe Wave Spawning (`>= 12m` from Player) | `CombatDirector.resolveSafeSpawnPosition()` relocated spawn within 1.4m of player to `>= 12m` outside colliders | PASS |
| Environmental Hazard Awareness | Detonating fuel barrel eliminates nearby enemy via `DamageSystem.applyRadialHazardBlast()` and alerts units within 34m | PASS |
