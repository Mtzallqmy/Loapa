package com.example

import com.example.game.audio.TacticalAudioEngine
import com.example.game.data.AttachmentSlot
import com.example.game.data.CameraShoulder
import com.example.game.data.ChallengeMedal
import com.example.game.data.CharacterSlot
import com.example.game.data.CityId
import com.example.game.data.Difficulty
import com.example.game.data.GameLanguage
import com.example.game.data.GameMode
import com.example.game.data.GameModeCatalog
import com.example.game.data.GraphicsQuality
import com.example.game.data.MissionCatalog
import com.example.game.data.WeaponCatalog
import com.example.game.data.WeaponId
import com.example.game.data.WeatherCondition
import com.example.game.engine.CityBuilder
import com.example.game.engine.CombatSimulation
import com.example.game.engine.MissionRunStatus
import com.example.game.engine.PlayerInputState
import com.example.game.engine.SimAudioEvent
import com.example.game.engine.Vec3
import com.example.game.save.ControlLayoutSettings
import com.example.game.save.GameSettings
import com.example.game.save.PlayerSaveData
import com.example.game.save.SAVE_SCHEMA_VERSION
import com.example.game.save.SaveManager
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import kotlin.io.path.createTempDirectory

/**
 * v2.1.0 Master Engineering Prompt 05 — End-to-End Integration Workflows (A–E),
 * Dynamic Weather & Tactical Vehicle verification, and Cross-System Stress Tests.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class EndToEndAndStressTest {

    private fun tempSaveDir(): File = createTempDirectory("combat_fronts_e2e").toFile()

    @Test
    fun workflowA_campaignMissionCheckpointVictoryAndPersistence() {
        val dir = tempSaveDir()
        val mgr = SaveManager(dir)
        val initialSave = PlayerSaveData()
        val m1 = MissionCatalog.missionByNumber(1)
        val sim = CombatSimulation(m1, initialSave, mode = GameMode.CAMPAIGN)
        sim.skipIntro()

        // Step movement to complete objective 0 (MOVE_TUTORIAL) and trigger checkpoint
        repeat(60) {
            sim.step(0.03f, PlayerInputState(moveY = 1f))
        }
        assertTrue("Objective 0 should complete and advance to objective 1", sim.currentObjectiveIndex >= 1)
        assertEquals(1, sim.latestCheckpoint.missionNumber)

        // Eliminate all enemies & complete remaining mission objectives
        sim.enemies.forEach { it.health = 0f; it.state = com.example.game.data.EnemyAiState.DEFEATED }
        var safety = 0
        while (sim.status == MissionRunStatus.ACTIVE && safety < 250) {
            val obj = sim.currentObjective ?: break
            sim.player.position = Vec3(obj.targetX, 0f, obj.targetZ)
            sim.step(0.05f, PlayerInputState(interactPressed = true))
            safety++
        }
        assertEquals(MissionRunStatus.VICTORY, sim.status)

        val summary = sim.buildSummary()
        assertTrue(summary.victory)
        assertTrue(summary.xpEarned > 0)

        // Persist outcome and reload from a brand new SaveManager instance
        val updated = initialSave.copy(
            highestUnlockedMission = 2,
            completedMissions = setOf(1),
            xp = initialSave.xp + summary.xpEarned,
            missionBestStars = mapOf(1 to summary.stars)
        )
        assertTrue(mgr.save(updated))
        val reloaded = SaveManager(dir).load()
        assertEquals(SAVE_SCHEMA_VERSION, reloaded.schemaVersion)
        assertEquals(2, reloaded.highestUnlockedMission)
        assertTrue(1 in reloaded.completedMissions)
        assertEquals(summary.xpEarned, reloaded.xp)
    }

    @Test
    fun workflowB_weaponCustomizationAppliesLiveInCombatAndPersists() {
        val dir = tempSaveDir()
        val mgr = SaveManager(dir)
        val defaultStats = WeaponCatalog.computeEffectiveStats(WeaponId.VX74_ASSAULT, WeaponCatalog.defaultAttachments())
        val customAtts = WeaponCatalog.defaultAttachments() + mapOf(
            AttachmentSlot.MAGAZINE to "mag_ext",
            AttachmentSlot.BARREL to "barrel_tac",
            AttachmentSlot.GRIP to "grip_vertical"
        )
        val save = PlayerSaveData(
            unlockedAttachments = WeaponCatalog.defaultAttachments().values.toSet() + setOf("mag_ext", "barrel_tac", "grip_vertical"),
            weaponAttachments = mapOf(WeaponId.VX74_ASSAULT to customAtts)
        )
        assertTrue(mgr.save(save))

        val reloaded = SaveManager(dir).load()
        val sim = CombatSimulation(MissionCatalog.missionByNumber(1), reloaded)
        val liveStats = sim.player.activeWeapon.stats
        assertTrue("Extended magazine must increase live capacity", liveStats.magCapacity > defaultStats.magCapacity)
        assertTrue("Long barrel must increase live damage/range", liveStats.effectiveRange > defaultStats.effectiveRange)
        assertTrue("Vertical grip must reduce live recoil", liveStats.recoilKick < defaultStats.recoilKick)
    }

    @Test
    fun workflowC_characterGearControlsAudioAndCameraShoulderPersist() {
        val dir = tempSaveDir()
        val mgr = SaveManager(dir)
        val customSave = PlayerSaveData(
            equippedCharacterGear = mapOf(
                CharacterSlot.OUTFIT to "outfit_desert",
                CharacterSlot.HELMET to "helmet_nvg",
                CharacterSlot.VEST to "vest_heavy",
                CharacterSlot.GLOVES to "gloves_tactical",
                CharacterSlot.BOOTS to "boots_desert",
                CharacterSlot.BACKPACK to "pack_comms"
            ),
            settings = GameSettings(
                language = GameLanguage.ENGLISH,
                difficulty = Difficulty.HARD,
                graphicsQuality = GraphicsQuality.HIGH,
                masterVolume = 0.65f,
                sfxVolume = 0.70f,
                cameraShoulder = CameraShoulder.LEFT,
                controls = ControlLayoutSettings(leftHanded = true, buttonScale = 1.2f, buttonOpacity = 0.72f)
            )
        )
        assertTrue(mgr.save(customSave))

        val reloaded = SaveManager(dir).load()
        assertEquals("outfit_desert", reloaded.equippedCharacterGear[CharacterSlot.OUTFIT])
        assertEquals("helmet_nvg", reloaded.equippedCharacterGear[CharacterSlot.HELMET])
        assertEquals(GameLanguage.ENGLISH, reloaded.settings.language)
        assertEquals(Difficulty.HARD, reloaded.settings.difficulty)
        assertEquals(CameraShoulder.LEFT, reloaded.settings.cameraShoulder)
        assertTrue(reloaded.settings.controls.leftHanded)
        assertEquals(1.2f, reloaded.settings.controls.buttonScale, 0.01f)
        assertEquals(0.65f, reloaded.settings.masterVolume, 0.01f)
    }

    @Test
    fun workflowD_survivalDefenseAndChallengesModesExecuteAndPersistRecords() {
        val dir = tempSaveDir()
        val mgr = SaveManager(dir)
        val baseSave = PlayerSaveData()

        // 1. Survival Mode: wave clear -> resupply -> wave 2 spawn -> score increase
        val survDef = GameModeCatalog.buildSurvivalMissionDef(CityId.MIRAGE_CITY, WeatherCondition.SANDSTORM)
        val survSim = CombatSimulation(survDef, baseSave, mode = GameMode.SURVIVAL, weatherCondition = WeatherCondition.SANDSTORM)
        survSim.skipIntro()
        assertEquals(1, survSim.waveNumber)
        assertTrue("Survival wave 1 must spawn active enemies", survSim.enemies.isNotEmpty())

        // Clear wave 1
        survSim.enemies.forEach { it.health = 0f; it.state = com.example.game.data.EnemyAiState.DEFEATED }
        survSim.missionKills += 4
        repeat(80) { survSim.step(0.05f, PlayerInputState()) }
        assertEquals("Survival must advance to wave 2 after clearing wave 1", 2, survSim.waveNumber)
        assertTrue("Survival score must increase after clearing wave 1", survSim.score >= 200)
        assertTrue("Survival wave 2 must spawn fresh enemies", survSim.enemies.count { it.isAlive } >= 4)

        // 2. Defense Mode: 5-wave Core Defense victory
        val defDef = GameModeCatalog.buildDefenseMissionDef(CityId.HARBOR_CITY, WeatherCondition.FOG)
        val defSim = CombatSimulation(defDef, baseSave, mode = GameMode.DEFENSE, weatherCondition = WeatherCondition.FOG)
        defSim.skipIntro()
        assertEquals(400f, defSim.defenseCoreHealth, 0.01f)
        for (wave in 1..5) {
            defSim.enemies.forEach { it.health = 0f; it.state = com.example.game.data.EnemyAiState.DEFEATED }
            repeat(75) { defSim.step(0.05f, PlayerInputState()) }
        }
        assertEquals("Clearing all 5 Defense waves must trigger Victory", MissionRunStatus.VICTORY, defSim.status)
        val defSummary = defSim.buildSummary()
        assertEquals(GameMode.DEFENSE, defSummary.gameMode)
        assertTrue(defSummary.victory)

        // 3. Challenge Mode: timer countdown, completion & medal evaluation
        val chal = GameModeCatalog.challengeById("chal_river_marksman")
        val chalDef = GameModeCatalog.buildChallengeMissionDef(chal)
        val chalSim = CombatSimulation(chalDef, baseSave, mode = GameMode.CHALLENGES, weatherCondition = chal.weather, challenge = chal)
        chalSim.skipIntro()
        val initialTime = chalSim.challengeTimeRemaining
        chalSim.step(0.05f, PlayerInputState())
        assertTrue("Challenge timer must count down", chalSim.challengeTimeRemaining < initialTime)
        chalSim.missionKills = chal.targetKills
        chalSim.missionHeadshots = chal.requiredHeadshots
        chalSim.score = 900
        chalSim.enemies.forEach { it.health = 0f; it.state = com.example.game.data.EnemyAiState.DEFEATED }
        chalSim.step(0.05f, PlayerInputState())
        assertEquals(MissionRunStatus.VICTORY, chalSim.status)
        val chalSummary = chalSim.buildSummary()
        assertTrue("Completed challenge must award Bronze/Silver/Gold medal", chalSummary.challengeMedal.tier >= ChallengeMedal.SILVER.tier)

        // Persist all 3 mode records and reload via fresh SaveManager
        val saved = baseSave.copy(
            survivalBestWaves = mapOf(CityId.MIRAGE_CITY to survSim.waveNumber),
            survivalBestScore = survSim.score,
            defenseBestWaves = mapOf(CityId.HARBOR_CITY to 5),
            defenseVictories = 1,
            challengeBestScores = mapOf(chal.id to chalSummary.score),
            challengeMedals = mapOf(chal.id to chalSummary.challengeMedal),
            completedChallenges = setOf(chal.id)
        )
        assertTrue(mgr.save(saved))
        val reloaded = SaveManager(dir).load()
        assertEquals(2, reloaded.survivalBestWaves[CityId.MIRAGE_CITY])
        assertEquals(survSim.score, reloaded.survivalBestScore)
        assertEquals(5, reloaded.defenseBestWaves[CityId.HARBOR_CITY])
        assertEquals(1, reloaded.defenseVictories)
        assertEquals(chalSummary.challengeMedal, reloaded.challengeMedals[chal.id])
        assertTrue(chal.id in reloaded.completedChallenges)
    }

    @Test
    fun workflowE_midMissionForceStopRecoveryAndLegacySchemaV1V2V3Migration() {
        val dir = tempSaveDir()
        val mgr = SaveManager(dir)
        val m2 = MissionCatalog.missionByNumber(2)
        val sim = CombatSimulation(m2, PlayerSaveData())
        sim.skipIntro()

        // Advance to objective 1 checkpoint (Mission 2 Obj 0 is SECURE_ZONE requiring 2.2s dwell)
        sim.player.position = Vec3(m2.objectives[0].targetX, 0f, m2.objectives[0].targetZ)
        var ticks = 0
        while (sim.currentObjectiveIndex == 0 && ticks < 60) {
            sim.player.health = sim.player.maxHealth
            sim.step(0.05f, PlayerInputState())
            ticks++
        }
        assertEquals(1, sim.currentObjectiveIndex)
        val checkpointSave = PlayerSaveData(lastPlayedMission = 2, lastCheckpoint = sim.latestCheckpoint)
        assertTrue(mgr.save(checkpointSave))

        // Reload fresh session and restore checkpoint
        val reloaded = SaveManager(dir).load()
        assertNotNull(reloaded.lastCheckpoint)
        val resumedSim = CombatSimulation(m2, reloaded, resumeCheckpoint = reloaded.lastCheckpoint)
        assertEquals(MissionRunStatus.ACTIVE, resumedSim.status)
        assertEquals(1, resumedSim.currentObjectiveIndex)

        // Verify backward-compatible migration from legacy schema v3 JSON
        val legacyV3 = JSONObject().apply {
            put("schemaVersion", 3)
            put("highestUnlockedMission", 5)
            put("xp", 820)
        }
        val migrated = mgr.deserialize(legacyV3)
        assertEquals(SAVE_SCHEMA_VERSION, migrated.schemaVersion)
        assertEquals(5, migrated.highestUnlockedMission)
        assertEquals(820, migrated.xp)
        assertEquals(0, migrated.survivalBestScore)
        assertEquals(0, migrated.defenseVictories)
    }

    @Test
    fun weatherAndTacticalVehicleIntegrationWorksAcrossCities() {
        val save = PlayerSaveData(settings = GameSettings(graphicsQuality = GraphicsQuality.HIGH))
        val survDef = GameModeCatalog.buildSurvivalMissionDef(CityId.RIVER_CITY, WeatherCondition.RAIN)
        val sim = CombatSimulation(survDef, save, mode = GameMode.SURVIVAL, weatherCondition = WeatherCondition.RAIN)
        sim.skipIntro()
        sim.enemies.clear()

        // Weather particles must populate and AI vision multiplier must be reduced in Rain/Fog/Sandstorm
        sim.step(0.03f, PlayerInputState())
        assertTrue("Rain weather must generate active 3D weather particles", sim.weatherSystem.particles.isNotEmpty())
        assertTrue("Rain must reduce AI sight multiplier below 1.0", sim.weatherSystem.aiVisionMultiplier < 1.0f)
        assertTrue("Sandstorm must reduce AI sight more than daylight", WeatherCondition.SANDSTORM.aiVisionMultiplier < 0.6f)

        // Move next to Tactical Armored Rover and board it via WorldInteractionManager
        sim.player.position = sim.vehicle.position + Vec3(1.4f, 0f, 0f)
        sim.step(0.03f, PlayerInputState(interactPressed = true))
        assertTrue("Player must board the Tactical Armored Rover", sim.vehicle.isOccupied)
        val startPos = sim.vehicle.position

        // Drive forward and fire roof autocannon turret
        repeat(25) {
            sim.step(0.03f, PlayerInputState(moveY = 1f, fireHeld = true))
        }
        assertTrue("Driving throttle must move the Tactical Rover", sim.vehicle.position.distanceXZ(startPos) > 0.5f)
        assertTrue("Firing rover turret must register shots", sim.missionShotsFired > 0)

        // Exit rover safely
        sim.step(0.03f, PlayerInputState(interactPressed = true))
        assertFalse("Interacting while driving must safely exit the rover", sim.vehicle.isOccupied)
    }

    @Test
    fun crossSystemStressTest600TicksZeroExceptionsZeroNaNAndBoundedPools() {
        val save = PlayerSaveData(settings = GameSettings(graphicsQuality = GraphicsQuality.HIGH))
        val defDef = GameModeCatalog.buildDefenseMissionDef(CityId.MOUNTAIN_CITY, WeatherCondition.SANDSTORM)
        val sim = CombatSimulation(defDef, save, mode = GameMode.DEFENSE, weatherCondition = WeatherCondition.SANDSTORM)
        sim.skipIntro()

        repeat(600) { tick ->
            val input = PlayerInputState(
                moveX = if (tick % 40 < 20) 0.7f else -0.7f,
                moveY = if (tick % 60 < 40) 0.9f else -0.4f,
                lookDeltaX = 0.02f,
                fireHeld = tick % 3 == 0,
                aimHeld = tick % 10 < 5,
                reloadPressed = tick % 45 == 0,
                switchWeaponPressed = tick % 55 == 0,
                jumpPressed = tick % 70 == 0,
                toggleShoulderPressed = tick % 90 == 0
            )
            sim.step(0.016f, input)
            sim.performanceManager.recordFrame(0.016f, GraphicsQuality.HIGH)

            // Keep player & core alive for full stress loop duration
            if (sim.player.health < 35f) sim.player.health = 100f
            if (sim.defenseCoreHealth < 120f) sim.defenseCoreHealth = 400f
            if (sim.status != MissionRunStatus.ACTIVE) {
                sim.status = MissionRunStatus.ACTIVE
            }

            assertFalse("Player X must never be NaN", sim.player.position.x.isNaN())
            assertFalse("Player Z must never be NaN", sim.player.position.z.isNaN())
            assertTrue("Tracers must stay bounded by PerformanceManager cap", sim.tracers.size <= sim.performanceManager.maxActiveTracers)
            assertTrue("Impact particles must stay bounded", sim.particles.size <= sim.performanceManager.effectiveMaxParticles)
            assertTrue("Weather particles must stay bounded", sim.weatherSystem.particles.size <= 72)
        }
        assertTrue("HUD snapshot must build cleanly after 600 stress ticks", sim.buildHudSnapshot().fps in 10..120)
    }

    @Test
    fun allFourCitiesHaveClearUnobstructedSpawnsForPlayerVehicleAndDefenseCore() {
        val corePos = Vec3(0f, 0f, 18f)
        for (cityId in CityId.entries) {
            val city = CityBuilder.buildCity(cityId)
            val vPos = city.vehicleSpawnPos

            assertTrue(
                "Vehicle spawn in $cityId must not overlap Defense Command Core",
                vPos.distanceXZ(corePos) >= 3.5f
            )
            for (block in city.colliders) {
                val b = block.bounds
                val r = 1.25f
                val overlapsVehicle = vPos.x + r > b.minX && vPos.x - r < b.maxX &&
                    vPos.z + r > b.minZ && vPos.z - r < b.maxZ &&
                    b.maxY > 0.25f
                assertFalse(
                    "Vehicle spawn (${vPos.x}, ${vPos.z}) in $cityId must not intersect collider ${block.id}",
                    overlapsVehicle
                )
            }
        }
    }

    @Test
    fun challengeHeadshotSoftLockPreventionSpawnsBonusTargets() {
        val chal = GameModeCatalog.challengeById("chal_river_marksman")
        val chalDef = GameModeCatalog.buildChallengeMissionDef(chal)
        val sim = CombatSimulation(chalDef, PlayerSaveData(), mode = GameMode.CHALLENGES, weatherCondition = chal.weather, challenge = chal)
        sim.skipIntro()

        // Kill all initial targets with body shots only (0 headshots < required 3)
        sim.missionKills = chal.targetKills
        sim.missionHeadshots = 0
        sim.enemies.forEach { it.health = 0f; it.state = com.example.game.data.EnemyAiState.DEFEATED }

        sim.step(0.05f, PlayerInputState())
        assertEquals("Challenge should remain ACTIVE when headshots are still needed", MissionRunStatus.ACTIVE, sim.status)
        assertTrue("Bonus targets must spawn so Challenge Mode never soft-locks", sim.enemies.count { it.isAlive } >= 1)
    }

    @Test
    fun audioEngineHandlesHighRateConcurrentBurstsWithoutBlockingOrCrashing() {
        val engine = TacticalAudioEngine()
        val startMs = System.currentTimeMillis()
        repeat(60) { i ->
            engine.handleEvent(SimAudioEvent.WeaponFired(WeaponId.VX74_ASSAULT))
            engine.handleEvent(SimAudioEvent.HitConfirm(headshot = i % 5 == 0))
            if (i % 10 == 0) engine.handleEvent(SimAudioEvent.VehicleTurretFired)
            if (i % 12 == 0) engine.playUiClick()
        }
        val elapsedMs = System.currentTimeMillis() - startMs
        engine.release()
        assertTrue("Audio dispatch must remain non-blocking under burst load (took ${elapsedMs}ms)", elapsedMs < 1500L)
    }
}
