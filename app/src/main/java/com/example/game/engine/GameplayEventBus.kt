package com.example.game.engine

import com.example.game.data.CombatPhase
import com.example.game.data.DamageSource
import com.example.game.data.HitZone
import com.example.game.data.InteractiveKind
import com.example.game.data.WeaponId

/**
 * v1.1.0 Unified Gameplay Event Bus.
 * Single authoritative communication channel connecting Player, Weapons, DamageSystem,
 * Enemy AI, SquadCoordinator, WorldInteractionManager, MissionDirector, Audio, and HUD.
 */
sealed class GameplayEvent {
    data class PlayerDamaged(
        val amount: Float,
        val source: DamageSource,
        val sourcePos: Vec3,
        val remainingHealth: Float,
        val remainingArmor: Float
    ) : GameplayEvent()

    data class PlayerHealed(val healthDelta: Float, val armorDelta: Float) : GameplayEvent()
    data class WeaponFired(val weaponId: WeaponId, val origin: Vec3, val noiseRadius: Float) : GameplayEvent()
    data class WeaponReloaded(val weaponId: WeaponId, val magCount: Int, val reserveCount: Int) : GameplayEvent()
    data class AmmoDepleted(val weaponId: WeaponId) : GameplayEvent()
    data class EnemyAlerted(val enemyId: Int, val threatPos: Vec3) : GameplayEvent()
    data class EnemyDefeated(
        val enemyId: Int,
        val weaponId: WeaponId?,
        val hitZone: HitZone,
        val position: Vec3,
        val source: DamageSource
    ) : GameplayEvent()

    data class ObjectiveStarted(val missionNumber: Int, val objectiveIndex: Int) : GameplayEvent()
    data class ObjectiveCompleted(val missionNumber: Int, val objectiveIndex: Int) : GameplayEvent()
    data class CheckpointReached(val missionNumber: Int, val objectiveIndex: Int) : GameplayEvent()
    data class MissionFailed(val missionNumber: Int, val reason: String) : GameplayEvent()
    data class MissionCompleted(val missionNumber: Int) : GameplayEvent()
    data class EquipmentUnlocked(val itemId: String) : GameplayEvent()
    data class WorldInteracted(val objectId: String, val kind: InteractiveKind, val position: Vec3) : GameplayEvent()
    data class CombatPhaseChanged(val previous: CombatPhase, val current: CombatPhase) : GameplayEvent()
}

class GameplayEventBus(private val maxHistory: Int = 128) {
    private val listeners = mutableListOf<(GameplayEvent) -> Unit>()
    private val history = ArrayDeque<GameplayEvent>(maxHistory)

    fun subscribe(listener: (GameplayEvent) -> Unit) {
        if (listener !in listeners) listeners.add(listener)
    }

    fun unsubscribe(listener: (GameplayEvent) -> Unit) {
        listeners.remove(listener)
    }

    fun publish(event: GameplayEvent) {
        if (history.size >= maxHistory) history.removeFirst()
        history.addLast(event)
        // Iterate snapshot so listeners can safely unsubscribe or trigger chained events
        val snapshot = listeners.toList()
        for (i in snapshot.indices) {
            snapshot[i].invoke(event)
        }
    }

    fun recentEvents(): List<GameplayEvent> = history.toList()

    fun clearHistory() = history.clear()
}
