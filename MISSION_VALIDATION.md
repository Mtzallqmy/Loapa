# COMBAT FRONTS (جبهات القتال) — MISSION VALIDATION (v1.1.0)

**Validator:** `com.example.game.engine.MissionValidator.validateAll()`  
**End-to-End Simulation Runner:** `CombatFrontsEngineTest` + `UnifiedBattlefieldRegressionTest`

## Validation Summary
- **Cities Checked:** 4 / 4 (`RIVER_CITY`, `MIRAGE_CITY`, `HARBOR_CITY`, `MOUNTAIN_CITY`)
- **Missions Checked:** 12 / 12 (Easy, Medium, and Hard difficulties)
- **Issues Detected:** `0`

| Theater | Missions | Cover Points | Interactive Objects | Bounds & Spawn Check | End-to-End Completion |
|---|---|---|---|---|---|
| مدينة النهر (River City) | 01, 02, 03 | >= 8 (vaultable barriers + vehicles) | Door, Locked Gate + Switch, Supply Box, Ammo Crate, 2 Barrels, Terminal | PASS | PASS |
| مدينة السراب (Mirage City) | 04, 05, 06 | >= 8 (rocks, sandbags, crates, silos) | Locked Gate + Switch, Supply Box, 2 Barrels, Terminal | PASS | PASS |
| مدينة المرسى (Harbor City) | 07, 08, 09 | >= 8 (containers + cargo crates) | Cargo Door, Ammo Crate, 2 Barrels, Terminal | PASS | PASS |
| مدينة الجبل (Mountain City) | 10, 11, 12 | >= 8 (rock/concrete barricades) | Locked Citadel Gate + Switch, Supply Box, 2 Barrels, Terminal | PASS | PASS (incl. Boss) |
