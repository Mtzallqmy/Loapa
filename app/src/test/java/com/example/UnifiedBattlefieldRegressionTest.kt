package com.example

import com.example.game.data.CameraShoulder
import com.example.game.data.CombatPhase
import com.example.game.data.DamageSource
import com.example.game.data.Difficulty
import com.example.game.data.EnemyAiState
import com.example.game.data.EnemyArchetype
import com.example.game.data.InteractiveKind
import com.example.game.data.LowerBodyAnimState
import com.example.game.data.MissionCatalog
import com.example.game.data.UpperBodyCombatState
import com.example.game.engine.CityBuilder
import com.example.game.engine.CombatDirector
import com.example.game.engine.CombatSimulation
import com.example.game.engine.EnemyUnit
import com.example.game.engine.GameplayEvent
import com.example.game.engine.MissionRunStatus
import com.example.game.engine.MissionValidator
import com.example.game.engine.PlayerInputState
import com.example.game.engine.SquadCoordinator
import com.example.game.engine.Vec3
import com.example.game.save.GameSettings
import com.example.game.save.PlayerSaveData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UnifiedBattlefieldRegressionTest {

    @Test
    fun `mission validator verifies all 4 cities and 12 missions with zero errors`() {
        val report = MissionValidator.validateAll()
        assertEquals(12, report.missionsChecked)
        assertEquals(4, report.citiesChecked)
        assertTrue("MissionValidator issues: ${report.issues}", report.isValid)
    }

    @Test
    fun `cross-system player combat combos - sprint+aim, fire+reload cancel, empty reserve, cover lean, vault, shoulder switch`() {
        val sim = CombatSimulation(MissionCatalog.missionByNumber(1), PlayerSaveData())
        sim.enemies.clear()
        sim.skipIntro()

        // 1. Sprinting + Aiming: aiming overrides sprint and transitions upper-body layer to AIM
        repeat(10) { sim.step(0.03f, PlayerInputState(moveY = 1f, sprintHeld = true)) }
        assertTrue(sim.player.isSprinting)
        assertEquals(LowerBodyAnimState.SPRINT, sim.player.lowerBodyState)

        sim.step(0.03f, PlayerInputState(moveY = 1f, sprintHeld = true, aimHeld = true))
        assertFalse("Aiming must cancel sprint", sim.player.isSprinting)
        assertTrue(sim.player.isAiming)
        assertEquals(UpperBodyCombatState.AIM, sim.player.upperBodyState)

        // 2. Firing + Reloading + Reload Cancellation on weapon switch
        sim.step(0.03f, PlayerInputState(aimHeld = true, fireHeld = true))
        assertTrue(sim.eventBus.recentEvents().any { it is GameplayEvent.WeaponFired })
        sim.step(0.03f, PlayerInputState(reloadPressed = true))
        assertTrue(sim.player.isReloading)
        assertEquals(UpperBodyCombatState.RELOAD, sim.player.upperBodyState)

        sim.step(0.03f, PlayerInputState(switchWeaponPressed = true))
        assertFalse("Weapon switch must cancel active reload", sim.player.isReloading)
        assertEquals(UpperBodyCombatState.WEAPON_SWITCH, sim.player.upperBodyState)

        // 3. Reload blocked when reserve ammo is 0
        sim.player.activeWeapon.ammoInMag = 2
        sim.player.activeWeapon.ammoReserve = 0
        assertFalse("Reload must fail with zero reserve", sim.player.startReload())

        // 4. Vaulting over designated waist-high cover barrier
        val vaultableBlock = sim.city.blocks.first { it.isVaultable && it.isActive }
        sim.player.position = Vec3(vaultableBlock.bounds.centerX, 0f, vaultableBlock.bounds.maxZ + 0.9f)
        sim.player.yaw = 0f // facing -Z toward barrier
        sim.step(0.03f, PlayerInputState(vaultPressed = true))
        assertTrue("Player should enter vault state", sim.player.isVaulting)
        assertEquals(LowerBodyAnimState.VAULT, sim.player.lowerBodyState)
        repeat(20) { sim.step(0.03f, PlayerInputState()) }
        assertFalse("Player should finish vault cleanly", sim.player.isVaulting)
        assertTrue("Player should land across the barrier", sim.player.position.z < vaultableBlock.bounds.minZ)

        // 5. Camera shoulder switching Left <-> Right
        assertEquals(CameraShoulder.RIGHT, sim.cameraDirector.shoulder)
        sim.step(0.03f, PlayerInputState(toggleShoulderPressed = true))
        assertEquals(CameraShoulder.LEFT, sim.cameraDirector.shoulder)
        val hudSnap = sim.buildHudSnapshot()
        assertEquals(CameraShoulder.LEFT, hudSnap.cameraShoulder)
    }

    @Test
    fun `authoritative damage system enforces armor mitigation, respawn protection shield, and fall damage`() {
        val sim = CombatSimulation(MissionCatalog.missionByNumber(1), PlayerSaveData())
        sim.enemies.clear()
        sim.skipIntro()

        // Checkpoint restoration grants respawn protection shield
        sim.restoreFromCheckpoint()
        assertTrue("Respawn protection should be active after checkpoint restore", sim.player.respawnProtectionTimer > 0f)
        val blocked = sim.damageSystem.applyDamageToPlayer(
            sim.player, 30f, DamageSource.ENEMY_WEAPON, Vec3(0f, 0f, 0f), reduceShake = true
        )
        assertTrue("Respawn shield should block incoming enemy damage", blocked.blockedByProtection)
        assertEquals(100f, sim.player.health, 0.01f)

        // Expire shield and verify armor + health damage and EventBus notification
        sim.player.respawnProtectionTimer = 0f
        val hit = sim.damageSystem.applyDamageToPlayer(
            sim.player, 40f, DamageSource.ENEMY_WEAPON, Vec3(0f, 0f, 0f), reduceShake = true
        )
        assertTrue(hit.applied)
        assertTrue("Armor should absorb majority of hit", sim.player.armor < 100f)
        assertTrue("Health should take remaining penetration", sim.player.health < 100f)
        assertTrue(sim.eventBus.recentEvents().any { it is GameplayEvent.PlayerDamaged })

        // Fall damage bypasses armor
        val armorBeforeFall = sim.player.armor
        val hpBeforeFall = sim.player.health
        sim.damageSystem.applyDamageToPlayer(
            sim.player, 20f, DamageSource.FALL_DAMAGE, sim.player.position, reduceShake = true
        )
        assertEquals("Fall damage must bypass armor", armorBeforeFall, sim.player.armor, 0.01f)
        assertEquals(hpBeforeFall - 20f, sim.player.health, 0.01f)
    }

    @Test
    fun `enemy AI 2_0 respects wall LoS, reserves distinct cover points, and enforces staggered attack slots`() {
        val sim = CombatSimulation(MissionCatalog.missionByNumber(1), PlayerSaveData(settings = GameSettings(difficulty = Difficulty.EASY)))
        sim.enemies.clear()
        sim.skipIntro()

        // 1. Enemy behind solid building wall never gains visual awareness
        val blindEnemy = EnemyUnit(10, EnemyArchetype.STANDARD_INFANTRY, Vec3(-23f, 0f, 31f), Vec3(-23f, 0f, 31f), Vec3(-23f, 0f, 31f), Difficulty.MEDIUM)
        sim.player.position = Vec3(-23f, 0f, 12f) // solid building between z=15.5..28.5
        sim.enemies.add(blindEnemy)
        repeat(15) { sim.step(0.04f, PlayerInputState()) }
        assertEquals("Wall must prevent enemy visual awareness", 0f, blindEnemy.awareness, 0.01f)

        // 2. SquadCoordinator cover reservation guarantees no two enemies stack on the same cover point
        val coord = SquadCoordinator(Difficulty.EASY)
        val cp1 = coord.reserveCoverFor(1, Vec3(0f, 0f, 18f), sim.city)
        val cp2 = coord.reserveCoverFor(2, Vec3(0f, 0f, 18f), sim.city)
        assertNotNull(cp1)
        assertNotNull(cp2)
        assertNotEquals("SquadCoordinator must prevent cover stacking", cp1, cp2)

        // 3. Staggered attack tokens cap simultaneous attackers (Easy = max 2)
        val units = (1..4).map { id ->
            EnemyUnit(id, EnemyArchetype.STANDARD_INFANTRY, Vec3(id * 2f, 0f, 0f), Vec3(0f, 0f, 0f), Vec3(0f, 0f, 0f), Difficulty.EASY)
        }
        val granted = units.count { coord.requestAttackSlot(it) }
        assertEquals("Easy difficulty should cap simultaneous active attackers at 2", 2, granted)
    }

    @Test
    fun `interactive world objects - doors, locked gates with switches, supply boxes, and explosive barrels work end-to-end`() {
        val sim = CombatSimulation(MissionCatalog.missionByNumber(1), PlayerSaveData())
        sim.enemies.clear()
        sim.skipIntro()

        // 1. Tactical door opens and disables its solid collider
        val door = sim.interactionManager.objects.first { it.kind == InteractiveKind.DOOR }
        val doorBlock = sim.city.blocks.first { it.id == door.linkedBlockId }
        assertTrue("Door block starts active/solid", doorBlock.isActive)
        sim.player.position = door.position + Vec3(0f, 0f, 1.4f)
        assertNotNull("HUD prompt should appear near door", sim.buildHudSnapshot().interactionPrompt)
        sim.step(0.03f, PlayerInputState(interactPressed = true))
        assertTrue("Door should be marked activated/open", door.isActivated)
        assertFalse("Opening door must deactivate its solid collider", doorBlock.isActive)

        // Wait past debounce window
        repeat(15) { sim.step(0.03f, PlayerInputState()) }

        // 2. Locked security gate refuses to open until nearby control switch is activated
        val gate = sim.interactionManager.objects.first { it.kind == InteractiveKind.GATE }
        val gateSwitch = sim.interactionManager.objects.first { it.kind == InteractiveKind.SWITCH }
        val gateBlock = sim.city.blocks.first { it.id == gate.linkedBlockId }
        assertTrue(gate.isLocked)
        assertTrue(gateBlock.isActive)

        sim.player.position = gate.position + Vec3(0f, 0f, 1.5f)
        sim.step(0.03f, PlayerInputState(interactPressed = true))
        assertTrue("Locked gate must stay closed", gateBlock.isActive)

        repeat(15) { sim.step(0.03f, PlayerInputState()) }
        sim.player.position = gateSwitch.position + Vec3(0f, 0f, 1.2f)
        sim.step(0.03f, PlayerInputState(interactPressed = true))
        assertTrue("Switch activated", gateSwitch.isActivated)
        assertFalse("Linked gate unlocked", gate.isLocked)
        assertFalse("Linked gate collider deactivated", gateBlock.isActive)

        // 3. Destructible fuel barrel detonates via radial DamageSystem and eliminates nearby enemy
        val barrel = sim.interactionManager.objects.first { it.kind == InteractiveKind.DESTRUCTIBLE_BARREL }
        val nearbyEnemy = EnemyUnit(99, EnemyArchetype.STANDARD_INFANTRY, barrel.position + Vec3(1.8f, 0f, 0f), barrel.position, barrel.position, Difficulty.EASY)
        sim.enemies.add(nearbyEnemy)
        repeat(15) { sim.step(0.03f, PlayerInputState()) }
        sim.player.position = barrel.position + Vec3(0f, 0f, 2.2f)
        sim.step(0.03f, PlayerInputState(interactPressed = true))
        assertTrue("Barrel should be destroyed", barrel.isDestroyed)
        assertFalse("Nearby enemy should be eliminated by barrel blast", nearbyEnemy.isAlive)
    }

    @Test
    fun `combat director enforces safe spawn distance and pause-resume preserves simulation integrity`() {
        val sim = CombatSimulation(MissionCatalog.missionByNumber(1), PlayerSaveData())
        sim.skipIntro()

        val tooClose = sim.player.position + Vec3(1f, 0f, -1f)
        val resolved = sim.combatDirector.resolveSafeSpawnPosition(tooClose, sim.player.position, sim.city, enforceSafeDistance = true)
        assertTrue(
            "Safe wave spawn must be at least ${CombatDirector.MIN_SAFE_SPAWN_DISTANCE}m away from player",
            resolved.distanceXZ(sim.player.position) >= CombatDirector.MIN_SAFE_SPAWN_DISTANCE - 0.5f
        )

        // Pause and Resume
        val posBeforePause = sim.player.position
        val elapsedBefore = sim.elapsedSeconds
        sim.status = MissionRunStatus.PAUSED
        repeat(10) { sim.step(0.03f, PlayerInputState(moveY = 1f, fireHeld = true)) }
        assertEquals("Paused simulation must freeze elapsed time", elapsedBefore, sim.elapsedSeconds, 0.0001f)
        assertEquals("Paused simulation must freeze player position", posBeforePause, sim.player.position)

        sim.status = MissionRunStatus.ACTIVE
        sim.step(0.03f, PlayerInputState(moveY = 1f))
        assertTrue("Resumed simulation must advance cleanly", sim.elapsedSeconds > elapsedBefore)
        assertTrue(sim.combatDirector.currentPhase in CombatPhase.entries)
    }
}
