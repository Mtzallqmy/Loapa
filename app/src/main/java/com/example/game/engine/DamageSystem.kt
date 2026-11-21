package com.example.game.engine

import com.example.game.data.DamageSource
import com.example.game.data.HitZone
import com.example.game.data.WeaponId
import kotlin.math.max

data class DamageResult(
    val applied: Boolean,
    val effectiveDamage: Float,
    val eliminated: Boolean,
    val blockedByProtection: Boolean = false
)

/**
 * v1.1.0 Authoritative DamageSystem.
 * Centralizes player, enemy, fall, and environmental hazard damage rules,
 * armor absorption, respawn protection, stagger thresholds, and event publishing.
 */
class DamageSystem(private val eventBus: GameplayEventBus) {

    fun applyDamageToPlayer(
        player: PlayerController,
        rawAmount: Float,
        source: DamageSource,
        sourcePos: Vec3,
        reduceShake: Boolean
    ): DamageResult {
        if (!player.isAlive || rawAmount <= 0f) {
            return DamageResult(applied = false, effectiveDamage = 0f, eliminated = false)
        }
        if (player.respawnProtectionTimer > 0f && source != DamageSource.MISSION_HAZARD) {
            return DamageResult(applied = false, effectiveDamage = 0f, eliminated = false, blockedByProtection = true)
        }

        val healthBefore = player.health
        val armorBefore = player.armor
        val killed = player.applyDamage(
            amount = rawAmount,
            sourcePos = sourcePos,
            reduceShake = reduceShake,
            bypassArmor = (source == DamageSource.FALL_DAMAGE)
        )
        val totalDelta = (healthBefore - player.health) + (armorBefore - player.armor)
        eventBus.publish(
            GameplayEvent.PlayerDamaged(
                amount = totalDelta,
                source = source,
                sourcePos = sourcePos,
                remainingHealth = player.health,
                remainingArmor = player.armor
            )
        )
        return DamageResult(applied = true, effectiveDamage = totalDelta, eliminated = killed)
    }

    fun applyDamageToEnemy(
        enemy: EnemyUnit,
        rawDamage: Float,
        hitZone: HitZone,
        attackerPos: Vec3,
        weaponId: WeaponId?,
        source: DamageSource
    ): DamageResult {
        if (!enemy.isAlive || rawDamage <= 0f) {
            return DamageResult(applied = false, effectiveDamage = 0f, eliminated = false)
        }
        val hpBefore = enemy.health + enemy.armor
        val killed = enemy.applyDamage(rawDamage, hitZone, attackerPos)
        val hpAfter = enemy.health + enemy.armor
        val dealt = max(0f, hpBefore - hpAfter)
        if (killed) {
            eventBus.publish(
                GameplayEvent.EnemyDefeated(
                    enemyId = enemy.id,
                    weaponId = weaponId,
                    hitZone = hitZone,
                    position = enemy.position,
                    source = source
                )
            )
        }
        return DamageResult(applied = true, effectiveDamage = dealt, eliminated = killed)
    }

    /**
     * Resolves an environmental hazard blast (e.g., detonated fuel barrel) affecting
     * both nearby enemies and the player with linear distance falloff.
     */
    fun applyRadialHazardBlast(
        center: Vec3,
        radius: Float,
        maxDamage: Float,
        player: PlayerController,
        enemies: List<EnemyUnit>,
        reduceShake: Boolean
    ): Int {
        var enemiesEliminated = 0
        for (i in enemies.indices) {
            val e = enemies[i]
            if (!e.isAlive) continue
            val dist = e.position.distanceXZ(center)
            if (dist <= radius) {
                val falloff = (1f - (dist / radius) * 0.72f).coerceIn(0.25f, 1f)
                val res = applyDamageToEnemy(
                    enemy = e,
                    rawDamage = maxDamage * falloff,
                    hitZone = HitZone.TORSO,
                    attackerPos = center,
                    weaponId = null,
                    source = DamageSource.ENVIRONMENTAL_HAZARD
                )
                if (res.eliminated) enemiesEliminated++
            }
        }
        val pDist = player.position.distanceXZ(center)
        if (pDist <= radius) {
            val falloff = (1f - (pDist / radius) * 0.75f).coerceIn(0.22f, 1f)
            applyDamageToPlayer(
                player = player,
                rawAmount = maxDamage * 0.55f * falloff,
                source = DamageSource.ENVIRONMENTAL_HAZARD,
                sourcePos = center,
                reduceShake = reduceShake
            )
        }
        return enemiesEliminated
    }
}
