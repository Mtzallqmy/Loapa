package com.example

import com.example.game.data.AttachmentSlot
import com.example.game.data.CityId
import com.example.game.data.Difficulty
import com.example.game.data.EnemyArchetype
import com.example.game.data.HitZone
import com.example.game.data.MissionCatalog
import com.example.game.data.ObjectiveKind
import com.example.game.data.PlayerAnimState
import com.example.game.data.WeaponCatalog
import com.example.game.data.WeaponId
import com.example.game.engine.CityBuilder
import com.example.game.engine.CombatSimulation
import com.example.game.engine.EnemyUnit
import com.example.game.engine.MissionRunStatus
import com.example.game.engine.PlayerInputState
import com.example.game.engine.Vec3
import com.example.game.save.GameSettings
import com.example.game.save.PlayerSaveData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CombatFrontsEngineTest {

    @Test
    fun `all four cities build with colliders, cover points, and resupply stations`() {
        CityId.entries.forEach { cityId ->
            val city = CityBuilder.buildCity(cityId)
            assertTrue("${cityId.id} must have blocks", city.blocks.size >= 15)
            assertTrue("${cityId.id} must have solid colliders", city.colliders.size >= 10)
            assertTrue("${cityId.id} must have cover points", city.coverPoints.size >= 8)
            assertTrue("${cityId.id} must have resupply stations", city.resupplyStations.isNotEmpty())
        }
    }

    @Test
    fun `player spawns, moves, sprints, crouches, jumps, lands, and enters tactical cover`() {
        val save = PlayerSaveData()
        val sim = CombatSimulation(MissionCatalog.missionByNumber(1), save)
        sim.enemies.clear() // Isolate player movement & cover physics from enemy gunfire
        sim.skipIntro()

        val startPos = sim.player.position
        // Walk forward
        repeat(20) { sim.step(0.03f, PlayerInputState(moveY = 1f)) }
        assertTrue("Player should move forward", sim.player.position.distanceXZ(startPos) > 1.5f)
        assertTrue(sim.player.animState in setOf(PlayerAnimState.WALK, PlayerAnimState.RUN))

        // Sprint
        repeat(15) { sim.step(0.03f, PlayerInputState(moveY = 1f, sprintHeld = true)) }
        assertEquals(PlayerAnimState.SPRINT, sim.player.animState)

        // Crouch
        sim.step(0.03f, PlayerInputState(crouchToggled = true))
        assertTrue(sim.player.isCrouching)
        assertEquals(PlayerAnimState.CROUCH, sim.player.animState)

        // Jump and Land
        sim.step(0.03f, PlayerInputState(jumpPressed = true))
        assertFalse("Player should be airborne after jump", sim.player.isGrounded)
        assertEquals(PlayerAnimState.JUMP, sim.player.animState)
        repeat(45) { sim.step(0.03f, PlayerInputState()) }
        assertTrue("Player should land back on ground", sim.player.isGrounded)

        // Snap into nearest tactical cover point
        val cover = sim.city.coverPoints.first()
        sim.player.position = Vec3(cover.position.x, 0f, cover.position.z + 0.5f)
        sim.step(0.03f, PlayerInputState(coverPressed = true))
        assertTrue("Player should snap into cover", sim.player.isInCover)
        assertEquals(PlayerAnimState.COVER, sim.player.animState)
    }

    @Test
    fun `all 8 weapons shoot, reload, switch, and apply attachment upgrades`() {
        val attachments = mapOf(
            AttachmentSlot.OPTIC to "optic_acog",
            AttachmentSlot.GRIP to "grip_vertical",
            AttachmentSlot.MAGAZINE to "mag_ext",
            AttachmentSlot.BARREL to "barrel_tac",
            AttachmentSlot.STABILIZER to "stab_pro",
            AttachmentSlot.SKIN to "skin_gold"
        )
        assertEquals(8, WeaponCatalog.weapons.size)
        WeaponId.entries.forEach { wId ->
            val base = WeaponCatalog.computeEffectiveStats(wId, WeaponCatalog.defaultAttachments())
            val upgraded = WeaponCatalog.computeEffectiveStats(wId, attachments)
            assertTrue("Extended mag should increase mag capacity for $wId", upgraded.magCapacity > base.magCapacity)
            assertTrue("Optic & grip should tighten ADS spread for $wId", upgraded.adsSpread <= base.adsSpread)
            assertTrue("Stabilizer should lower recoil for $wId", upgraded.recoilKick < base.recoilKick)
            assertTrue("Long barrel should extend range for $wId", upgraded.effectiveRange > base.effectiveRange)
        }

        val sim = CombatSimulation(MissionCatalog.missionByNumber(1), PlayerSaveData())
        sim.skipIntro()
        val initialWeapon = sim.player.activeWeaponId
        val initialMag = sim.player.activeWeapon.ammoInMag

        // Fire shot
        sim.step(0.03f, PlayerInputState(fireHeld = true, aimHeld = true))
        assertEquals(initialMag - 1, sim.player.activeWeapon.ammoInMag)
        assertTrue(sim.missionShotsFired >= 1)

        // Reload
        sim.step(0.03f, PlayerInputState(reloadPressed = true))
        assertTrue(sim.player.isReloading)
        repeat(90) { sim.step(0.03f, PlayerInputState()) }
        assertFalse(sim.player.isReloading)
        assertEquals(initialMag, sim.player.activeWeapon.ammoInMag)

        // Switch weapon
        sim.step(0.03f, PlayerInputState(switchWeaponPressed = true))
        assertNotEquals(initialWeapon, sim.player.activeWeaponId)
    }

    @Test
    fun `enemy hitbox multipliers, shield deflection, wall line-of-sight, and death work accurately`() {
        val city = CityBuilder.buildCity(CityId.RIVER_CITY)
        val e1 = EnemyUnit(1, EnemyArchetype.STANDARD_INFANTRY, Vec3(0f, 0f, -10f), Vec3(0f, 0f, -10f), Vec3(5f, 0f, -10f), Difficulty.MEDIUM)
        val e2 = EnemyUnit(2, EnemyArchetype.STANDARD_INFANTRY, Vec3(0f, 0f, -10f), Vec3(0f, 0f, -10f), Vec3(5f, 0f, -10f), Difficulty.MEDIUM)

        e1.applyDamage(30f, HitZone.HEAD, Vec3(0f, 0f, 0f))
        e2.applyDamage(30f, HitZone.LIMB, Vec3(0f, 0f, 0f))
        assertTrue("Headshot must deal significantly more damage than limb shot", e1.health < e2.health)

        // Fatal damage transitions enemy to DEAD
        val killed = e1.applyDamage(500f, HitZone.HEAD, Vec3(0f, 0f, 0f))
        assertTrue(killed)
        assertFalse(e1.isAlive)

        // Shield defender frontal shield absorbs frontal raycast
        val shieldEnemy = EnemyUnit(3, EnemyArchetype.SHIELD_DEFENDER, Vec3(0f, 0f, -10f), Vec3(0f, 0f, -10f), Vec3(0f, 0f, -10f), Difficulty.MEDIUM)
        shieldEnemy.yaw = Math.PI.toFloat() // facing +Z toward origin
        val hit = shieldEnemy.intersectRay(Vec3(0f, 1.0f, 0f), Vec3(0f, 0f, -1f), 25f)
        assertEquals(HitZone.SHIELD, hit?.first)

        // Line of sight is blocked by solid city building wall
        val blocked = city.hasLineOfSight(Vec3(-23f, 2f, 30f), Vec3(-23f, 2f, 12f))
        assertFalse("Line of sight must be blocked through solid building", blocked)
    }

    @Test
    fun `all twelve campaign missions load, progress through objectives, restore checkpoints, and complete`() {
        assertEquals(12, MissionCatalog.missions.size)
        val difficulties = listOf(Difficulty.EASY, Difficulty.MEDIUM, Difficulty.HARD)

        MissionCatalog.missions.forEachIndexed { idx, mission ->
            val diff = difficulties[idx % difficulties.size]
            val save = PlayerSaveData(settings = GameSettings(difficulty = diff))
            val sim = CombatSimulation(mission, save)
            assertEquals(MissionRunStatus.INTRO, sim.status)
            sim.skipIntro()
            assertEquals(MissionRunStatus.ACTIVE, sim.status)

            // Play through every objective phase of the mission
            var guard = 0
            while (sim.status == MissionRunStatus.ACTIVE && guard < 200) {
                guard++
                val obj = sim.currentObjective ?: break
                when (obj.kind) {
                    ObjectiveKind.MOVE_TUTORIAL,
                    ObjectiveKind.REACH_CHECKPOINT,
                    ObjectiveKind.ACQUIRE_AMMO,
                    ObjectiveKind.COLLECT_INTEL,
                    ObjectiveKind.EXTRACT -> {
                        sim.player.position = Vec3(obj.targetX, 0f, obj.targetZ)
                        sim.player.totalDistanceMoved = 10f
                        sim.step(0.04f, PlayerInputState())
                    }
                    ObjectiveKind.SECURE_ZONE -> {
                        sim.player.position = Vec3(obj.targetX, 0f, obj.targetZ)
                        repeat(65) { sim.step(0.04f, PlayerInputState()) }
                    }
                    ObjectiveKind.ELIMINATE_HOSTILES,
                    ObjectiveKind.SURVIVE_REINFORCEMENTS,
                    ObjectiveKind.DEFEND_CORE,
                    ObjectiveKind.DEFEAT_BOSS -> {
                        sim.enemies.filter { it.isAlive }.forEach { enemy ->
                            if (enemy.applyDamage(1500f, HitZone.HEAD, sim.player.position)) {
                                sim.missionKills += 1
                            }
                        }
                        sim.step(0.04f, PlayerInputState())
                    }
                }
            }
            assertEquals("Mission ${mission.number} (${mission.id}) should complete cleanly", MissionRunStatus.VICTORY, sim.status)
            val summary = sim.buildSummary()
            assertTrue("Mission ${mission.number} should award XP", summary.xpEarned > 0)
            assertTrue("Mission ${mission.number} should award 1-3 stars", summary.stars in 1..3)

            // Verify checkpoint restoration works without error
            sim.restoreFromCheckpoint(sim.latestCheckpoint)
            assertEquals(MissionRunStatus.ACTIVE, sim.status)
            assertTrue(sim.player.isAlive)
        }
    }
}
