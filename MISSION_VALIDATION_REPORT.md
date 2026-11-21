# Mission & Mode Validation Report — COMBAT FRONTS (جبهات القتال) v2.1.0

**Validator:** `MissionValidator.kt`, `CombatFrontsEngineTest.kt`, `EndToEndAndStressTest.kt`  
**Validation Date:** 2026-11-20  
**Result:** **100% VALIDATED — All 12 Campaign Missions + Survival + Defense + 4 Challenges PASS**

---

## 1. Campaign Missions (12 Missions across 4 Cities)

Validated automatically by `MissionValidator.validateAll()` inside `CombatFrontsEngineTest`:
- Every mission has valid non-colliding player spawn, enemy spawns inside playable bounds, non-empty objective chain, valid extraction/completion trigger, assigned weather preset (`GameModeCatalog.weatherForCampaignMission`), and bilingual Arabic/English briefing.

| # | Mission ID | City | Title (AR / EN) | Weather Preset | Validation |
| :-: | :--- | :--- | :--- | :--- | :-: |
| 1 | `m01_first_deployment` | River City (`مدينة النهر`) | الانتشار الأول / First Deployment | `DAYLIGHT` | **PASS** |
| 2 | `m02_besieged_street` | River City (`مدينة النهر`) | الشارع المحاصر / Besieged Street | `RAIN` | **PASS** |
| 3 | `m03_bridge_defense` | River City (`مدينة النهر`) | دفاع الجسر / Bridge Defense | `SUNSET` | **PASS** |
| 4 | `m04_sand_storm_entry` | Mirage City (`مدينة السراب`) | اقتحام العاصفة / Sandstorm Entry | `DUST` | **PASS** |
| 5 | `m05_oasis_intel` | Mirage City (`مدينة السراب`) | استخبارات الواحة / Oasis Intel | `DAYLIGHT` | **PASS** |
| 6 | `m06_mirage_vanguard` | Mirage City (`مدينة السراب`) | طليعة السراب / Mirage Vanguard (Boss) | `SANDSTORM` | **PASS** |
| 7 | `m07_dockyard_stealth` | Harbor City (`مدينة المرسى`) | تسلل الأرصفة / Dockyard Infiltration | `FOG` | **PASS** |
| 8 | `m08_container_maze` | Harbor City (`مدينة المرسى`) | متاهة الحاويات / Container Maze | `RAIN` | **PASS** |
| 9 | `m09_harbor_siege` | Harbor City (`مدينة المرسى`) | حصار الميناء / Harbor Siege | `NIGHT` | **PASS** |
| 10 | `m10_ridge_ascent` | Mountain City (`مدينة الجبل`) | صعود القمة / Ridge Ascent | `FOG` | **PASS** |
| 11 | `m11_tunnel_breach` | Mountain City (`مدينة الجبل`) | اختراق النفق / Tunnel Breach | `SUNSET` | **PASS** |
| 12 | `m12_fortress_final` | Mountain City (`مدينة الجبل`) | الحصن الأخير / Final Fortress (Boss) | `NIGHT` | **PASS** |

---

## 2. Expanded v2.1.0 Game Modes Validation

| Game Mode | Arabic Name | Rules & Win/Loss Flow | Automated Validation |
| :--- | :--- | :--- | :---: |
| **Survival** | `البقاء` | Playable in all 4 cities + 7 weather presets. Endless escalating waves with between-wave ammo/health resupply and per-city best wave/score saved in Schema v4. | **PASS** (`workflowD`) |
| **Defense** | `الدفاع` | Playable in all 4 cities + 7 weather presets. Protect 3D Command Core (400 HP) across 5 escalating enemy waves; AI targets Core or player dynamically. Victory on Wave 5 clear, Defeat if Core HP or Player HP hits 0. | **PASS** (`workflowD`) |
| **Challenges** | `التحديات` | 4 timed tactical trials: `chal_river_marksman` (River, Daylight), `chal_mirage_blitz` (Mirage, Sandstorm), `chal_harbor_ironclad` (Harbor, Fog), `chal_mountain_gauntlet` (Mountain, Night). Live timer + Bronze/Silver/Gold medals persisted in Schema v4. | **PASS** (`workflowD`) |

---

## 3. Tactical Vehicle Spawn Validation

All 4 city builders (`CityBuilder.kt`) expose an unobstructed `vehicleSpawnPos` checked against building/cover AABBs, allowing the Tactical Armored Rover to spawn, drive, fire its roof autocannon, and eject the player safely in every city.
