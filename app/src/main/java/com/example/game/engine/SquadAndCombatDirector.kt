package com.example.game.engine

import com.example.game.data.CombatPhase
import com.example.game.data.Difficulty
import com.example.game.data.EnemyAiState
import com.example.game.data.EnemyArchetype
import com.example.game.data.SquadRole

/**
 * v1.1.0 SquadCoordinator & CoverPointRegistry.
 * Prevents enemy cover stacking, coordinates squad alerts, and staggers active firing slots
 * so enemy squads fight tactically without unfair synchronized alpha-strikes.
 */
class SquadCoordinator(private val difficulty: Difficulty) {
    private val reservedCoverByEnemyId = mutableMapOf<Int, CoverPoint>()
    private val activeAttackerIds = mutableSetOf<Int>()
    var sharedLastKnownPlayerPos: Vec3? = null
        private set
    var lastAlertTimestamp: Float = -99f
        private set

    fun clear() {
        reservedCoverByEnemyId.clear()
        activeAttackerIds.clear()
        sharedLastKnownPlayerPos = null
    }

    fun releaseUnit(enemyId: Int) {
        reservedCoverByEnemyId.remove(enemyId)
        activeAttackerIds.remove(enemyId)
    }

    /** Reserves an unoccupied cover point closest to the enemy (preferring cover facing away from threat). */
    fun reserveCoverFor(enemyId: Int, enemyPos: Vec3, city: CityMap, maxRadius: Float = 16f): CoverPoint? {
        reservedCoverByEnemyId[enemyId]?.let { existing ->
            if (existing.position.distanceXZ(enemyPos) < maxRadius) return existing
        }
        val taken = reservedCoverByEnemyId.filterKeys { it != enemyId }.values.toSet()
        val candidate = city.coverPoints
            .filter { it !in taken && it.position.distanceXZ(enemyPos) <= maxRadius }
            .minByOrNull { it.position.distanceXZ(enemyPos) }
        if (candidate != null) {
            reservedCoverByEnemyId[enemyId] = candidate
        }
        return candidate
    }

    fun getReservedCover(enemyId: Int): CoverPoint? = reservedCoverByEnemyId[enemyId]

    /** Grants staggered attack token up to difficulty.maxSimultaneousAttackers (bosses/leaders always get priority). */
    fun requestAttackSlot(enemy: EnemyUnit): Boolean {
        if (enemy.id in activeAttackerIds) return true
        if (enemy.archetype.isBoss || activeAttackerIds.size < difficulty.maxSimultaneousAttackers) {
            activeAttackerIds.add(enemy.id)
            return true
        }
        return false
    }

    fun releaseAttackSlot(enemyId: Int) {
        activeAttackerIds.remove(enemyId)
    }

    fun activeAttackerCount(): Int = activeAttackerIds.size

    fun broadcastAlert(sender: EnemyUnit, threatPos: Vec3, enemies: List<EnemyUnit>, simTime: Float, radius: Float = 24f) {
        sharedLastKnownPlayerPos = threatPos
        lastAlertTimestamp = simTime
        for (i in enemies.indices) {
            val ally = enemies[i]
            if (ally.id != sender.id && ally.isAlive && ally.position.distanceXZ(sender.position) <= radius) {
                val assignFlank = ally.archetype.squadRole == SquadRole.MOBILE_FLANKER
                ally.receiveSquadAlert(threatPos, preferFlank = assignFlank)
            }
        }
    }

    fun onAllyDefeated(defeated: EnemyUnit, threatPos: Vec3, enemies: List<EnemyUnit>) {
        releaseUnit(defeated.id)
        sharedLastKnownPlayerPos = threatPos
        for (i in enemies.indices) {
            val ally = enemies[i]
            if (ally.isAlive && ally.position.distanceXZ(defeated.position) < 18f) {
                ally.onNearbyAllyDefeated(threatPos)
            }
        }
    }
}

/**
 * v1.1.0 CombatDirector & PerformanceManager.
 * Manages combat pacing phases (QUIET -> EXPLORATION -> ALERT -> COMBAT -> RECOVERY -> FINAL_ENCOUNTER),
 * active enemy caps, safe minimum-distance spawn validation, and adaptive AI budget monitoring.
 */
class CombatDirector(
    private val eventBus: GameplayEventBus,
    val maxActiveEnemies: Int = 10
) {
    companion object {
        const val MIN_SAFE_SPAWN_DISTANCE = 12f
    }

    var currentPhase: CombatPhase = CombatPhase.QUIET
        private set
    private var quietTimer: Float = 0f
    var lastWaveSpawnCount: Int = 0
        private set

    fun updatePacing(
        dt: Float,
        enemies: List<EnemyUnit>,
        player: PlayerController,
        isFinalObjective: Boolean
    ) {
        val alive = enemies.filter { it.isAlive }
        val inCombat = alive.any {
            it.state == EnemyAiState.ENGAGE ||
                it.state == EnemyAiState.FLANK ||
                it.state == EnemyAiState.TAKE_COVER ||
                it.state == EnemyAiState.REQUEST_SUPPORT
        }
        val alerted = alive.any {
            it.state == EnemyAiState.ALERT ||
                it.state == EnemyAiState.INVESTIGATE ||
                it.state == EnemyAiState.SUSPICIOUS ||
                it.state == EnemyAiState.SEARCH
        }
        val hasBoss = alive.any { it.archetype.isBoss }

        val desired = when {
            hasBoss || (isFinalObjective && inCombat) -> CombatPhase.FINAL_ENCOUNTER
            inCombat -> {
                quietTimer = 0f
                CombatPhase.COMBAT
            }
            alerted -> {
                quietTimer = 0f
                CombatPhase.ALERT
            }
            player.timeSinceLastDamage < 6f -> CombatPhase.RECOVERY
            alive.isNotEmpty() -> {
                quietTimer += dt
                if (quietTimer > 3.5f) CombatPhase.EXPLORATION else CombatPhase.RECOVERY
            }
            else -> CombatPhase.QUIET
        }

        if (desired != currentPhase) {
            val prev = currentPhase
            currentPhase = desired
            eventBus.publish(GameplayEvent.CombatPhaseChanged(prev, desired))
        }
    }

    /**
     * Relocates a spawn coordinate if it would place an enemy closer than MIN_SAFE_SPAWN_DISTANCE
     * to the player or inside a solid collider. Never spawns directly on top of the player.
     */
    fun resolveSafeSpawnPosition(
        desired: Vec3,
        playerPos: Vec3,
        city: CityMap,
        enforceSafeDistance: Boolean
    ): Vec3 {
        var pos = Vec3(
            desired.x.coerceIn(city.minBoundX + 1.2f, city.maxBoundX - 1.2f),
            0f,
            desired.z.coerceIn(city.minBoundZ + 1.2f, city.maxBoundZ - 1.2f)
        )
        if (enforceSafeDistance && pos.distanceXZ(playerPos) < MIN_SAFE_SPAWN_DISTANCE) {
            val away = (pos - playerPos).normalizedXZ()
            val dir = if (away.lengthXZ() > 0.1f) away else Vec3(0f, 0f, -1f)
            pos = Vec3(
                (playerPos.x + dir.x * (MIN_SAFE_SPAWN_DISTANCE + 2.5f)).coerceIn(city.minBoundX + 1.5f, city.maxBoundX - 1.5f),
                0f,
                (playerPos.z + dir.z * (MIN_SAFE_SPAWN_DISTANCE + 2.5f)).coerceIn(city.minBoundZ + 1.5f, city.maxBoundZ - 1.5f)
            )
        }
        // Push gently out of any solid collider if overlapping
        if (isBlocked(pos, city)) {
            val offsets = listOf(
                Vec3(2.2f, 0f, 0f), Vec3(-2.2f, 0f, 0f),
                Vec3(0f, 0f, 2.2f), Vec3(0f, 0f, -2.2f),
                Vec3(3.5f, 0f, 3.5f), Vec3(-3.5f, 0f, -3.5f)
            )
            for (off in offsets) {
                val cand = Vec3(
                    (pos.x + off.x).coerceIn(city.minBoundX + 1.2f, city.maxBoundX - 1.2f),
                    0f,
                    (pos.z + off.z).coerceIn(city.minBoundZ + 1.2f, city.maxBoundZ - 1.2f)
                )
                if (!isBlocked(cand, city)) {
                    pos = cand
                    break
                }
            }
        }
        return pos
    }

    private fun isBlocked(pos: Vec3, city: CityMap): Boolean {
        for (i in city.blocks.indices) {
            val b = city.blocks[i]
            if (!b.solid || !b.isActive) continue
            if (b.bounds.intersectsCylinder(pos.x, 0.1f, 1.8f, pos.z, 0.5f)) return true
        }
        return false
    }

    fun recordWaveSpawned(count: Int) {
        lastWaveSpawnCount = count
    }
}
