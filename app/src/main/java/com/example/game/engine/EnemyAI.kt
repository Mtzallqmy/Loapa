package com.example.game.engine

import com.example.game.data.Difficulty
import com.example.game.data.EnemyAiState
import com.example.game.data.EnemyArchetype
import com.example.game.data.HitZone
import com.example.game.data.SquadRole
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * v1.1.0 Enemy AI 2.0 Unit.
 * Implements 14-state tactical FSM, progressive PerceptionController + AwarenessMemory
 * (never sees through walls), SquadCoordinator cover reservation & staggered attack slots.
 */
class EnemyUnit(
    val id: Int,
    val archetype: EnemyArchetype,
    spawnPos: Vec3,
    val patrolA: Vec3,
    val patrolB: Vec3,
    difficulty: Difficulty
) {
    var position: Vec3 = spawnPos
    var yaw: Float = 0f
    var health: Float = archetype.maxHealth * difficulty.enemyHealthMult
    val maxHealth: Float = health
    var armor: Float = archetype.maxArmor * difficulty.enemyHealthMult
    val maxArmor: Float = armor

    var state: EnemyAiState = EnemyAiState.PATROL
    val squadRole: SquadRole = archetype.squadRole
    var ammoInMag: Int = archetype.magSize
    var fireCooldown: Float = 0.4f
    var stateTimer: Float = 0f
    var reloadTimer: Float = 0f
    var damagedTimer: Float = 0f
    var muzzleFlashTimer: Float = 0f
    var animTime: Float = Random.nextFloat() * 6f

    // v1.1.0 Perception & Awareness Memory (0.0 = unaware, 0.4 = suspicious, 1.0 = full alert)
    var awareness: Float = 0f
    var lastKnownPlayerPos: Vec3? = null
    var timeSincePlayerSeen: Float = 99f
    var assignedCover: CoverPoint? = null
    var patrolTargetB: Boolean = true
    var flankSide: Float = if (id % 2 == 0) 1f else -1f
    var hasRequestedSupport: Boolean = false
    var aiTickAccum: Float = (id % 4) * 0.03f
    var lastShotTargetedCore: Boolean = false

    val isAlive: Boolean get() = state != EnemyAiState.DEFEATED && health > 0f
    val height: Float get() = if (archetype.isBoss) 2.15f else if (archetype == EnemyArchetype.ARMORED_HEAVY) 1.98f else 1.82f
    val radius: Float get() = if (archetype.isBoss) 0.62f else 0.46f

    /** Ray-tests hitboxes (Head, Torso, Limb, Frontal Shield) and returns nearest HitZone + distance. */
    fun intersectRay(origin: Vec3, dir: Vec3, maxDist: Float): Pair<HitZone, Float>? {
        if (!isAlive) return null

        if (archetype.hasFrontalShield) {
            val facing = Vec3(-sin(yaw), 0f, -cos(yaw))
            val incoming = (origin - position).normalizedXZ()
            if (facing.dot(incoming) > 0.35f) {
                val shieldCenter = position + facing * 0.52f + Vec3(0f, 1.0f, 0f)
                val shieldBox = AABB(
                    shieldCenter.x - 0.45f, shieldCenter.y - 0.65f, shieldCenter.z - 0.45f,
                    shieldCenter.x + 0.45f, shieldCenter.y + 0.65f, shieldCenter.z + 0.45f
                )
                val tShield = shieldBox.intersectRay(origin, dir, maxDist)
                if (tShield != null) return HitZone.SHIELD to tShield
            }
        }

        val headBox = AABB(
            position.x - 0.24f, position.y + height - 0.42f, position.z - 0.24f,
            position.x + 0.24f, position.y + height + 0.05f, position.z + 0.24f
        )
        val torsoBox = AABB(
            position.x - radius * 0.85f, position.y + 0.78f, position.z - radius * 0.85f,
            position.x + radius * 0.85f, position.y + height - 0.40f, position.z + radius * 0.85f
        )
        val limbBox = AABB(
            position.x - radius * 0.72f, position.y, position.z - radius * 0.72f,
            position.x + radius * 0.72f, position.y + 0.80f, position.z + radius * 0.72f
        )

        var bestZone: HitZone? = null
        var bestDist = maxDist
        headBox.intersectRay(origin, dir, bestDist)?.let { bestDist = it; bestZone = HitZone.HEAD }
        torsoBox.intersectRay(origin, dir, bestDist)?.let { bestDist = it; bestZone = HitZone.TORSO }
        limbBox.intersectRay(origin, dir, bestDist)?.let { bestDist = it; bestZone = HitZone.LIMB }
        return bestZone?.let { it to bestDist }
    }

    /** Applies hit damage with armor absorption; returns true if this hit eliminated the enemy. */
    fun applyDamage(baseDamage: Float, zone: HitZone, attackerPos: Vec3): Boolean {
        if (!isAlive) return false
        var dmg = baseDamage * zone.damageMultiplier
        if (armor > 0f && zone != HitZone.HEAD) {
            val absorbed = min(armor, dmg * 0.65f)
            armor = max(0f, armor - absorbed)
            dmg -= absorbed * 0.8f
        }
        health = max(0f, health - dmg)
        lastKnownPlayerPos = attackerPos
        awareness = 1.0f
        if (health <= 0f) {
            state = EnemyAiState.DEFEATED
            return true
        }
        damagedTimer = 0.24f
        if (state == EnemyAiState.PATROL || state == EnemyAiState.IDLE || state == EnemyAiState.SUSPICIOUS || state == EnemyAiState.SEARCH) {
            state = EnemyAiState.ALERT
            stateTimer = 0.35f
        } else if (health < maxHealth * 0.32f && !archetype.isBoss && state != EnemyAiState.RETREAT) {
            state = EnemyAiState.RETREAT
            stateTimer = 2.2f
        }
        return false
    }

    fun alertTo(pos: Vec3, highPriority: Boolean) {
        if (!isAlive) return
        lastKnownPlayerPos = pos
        awareness = max(awareness, if (highPriority) 1.0f else 0.65f)
        if (state == EnemyAiState.PATROL || state == EnemyAiState.IDLE || state == EnemyAiState.SUSPICIOUS) {
            state = if (highPriority) EnemyAiState.ALERT else EnemyAiState.INVESTIGATE
            stateTimer = if (highPriority) 0.55f else 2.8f
        }
    }

    fun receiveSquadAlert(pos: Vec3, preferFlank: Boolean) {
        if (!isAlive) return
        lastKnownPlayerPos = pos
        awareness = 1.0f
        if (state in setOf(EnemyAiState.IDLE, EnemyAiState.PATROL, EnemyAiState.SUSPICIOUS, EnemyAiState.SEARCH)) {
            state = if (preferFlank) EnemyAiState.FLANK else EnemyAiState.ALERT
            stateTimer = if (preferFlank) 2.5f else 0.6f
        }
    }

    fun onNearbyAllyDefeated(threatPos: Vec3) {
        if (!isAlive) return
        lastKnownPlayerPos = threatPos
        awareness = 1.0f
        if (health < maxHealth * 0.45f && !archetype.isBoss) {
            state = EnemyAiState.RETREAT
            stateTimer = 2.4f
        } else if (archetype.canCoordinateSquad && !hasRequestedSupport) {
            state = EnemyAiState.REQUEST_SUPPORT
            stateTimer = 0.85f
            hasRequestedSupport = true
        } else if (state != EnemyAiState.ENGAGE && state != EnemyAiState.RELOAD) {
            state = EnemyAiState.TAKE_COVER
            stateTimer = 2.0f
        }
    }

    /**
     * Executes Enemy AI 2.0 14-state FSM with progressive perception and SquadCoordinator integration.
     * Returns true ONLY if this enemy fires a shot with verified line-of-sight and an attack slot.
     */
    fun step(
        dt: Float,
        playerPos: Vec3,
        playerEyePos: Vec3,
        playerAlive: Boolean,
        city: CityMap,
        difficulty: Difficulty,
        allies: List<EnemyUnit>,
        squadCoordinator: SquadCoordinator,
        eventBus: GameplayEventBus,
        simTime: Float,
        rng: Random,
        weatherVisionMult: Float = 1.0f,
        defenseCorePos: Vec3? = null
    ): Boolean {
        animTime += dt
        lastShotTargetedCore = false
        if (muzzleFlashTimer > 0f) muzzleFlashTimer = max(0f, muzzleFlashTimer - dt)
        if (fireCooldown > 0f) fireCooldown = max(0f, fireCooldown - dt)
        if (damagedTimer > 0f) damagedTimer = max(0f, damagedTimer - dt)

        if (!isAlive) {
            state = EnemyAiState.DEFEATED
            squadCoordinator.releaseUnit(id)
            return false
        }

        val distToPlayer = position.distanceXZ(playerPos)
        // Adaptive AI tick budget for distant units
        if (distToPlayer > 58f && defenseCorePos == null) {
            aiTickAccum += dt
            if (aiTickAccum < 0.16f) return false
            aiTickAccum = 0f
        }

        // Progressive PerceptionController (strictly respects solid wall LoS + dynamic weather visibility)
        val eyePos = Vec3(position.x, position.y + height - 0.25f, position.z)
        val baseVisionRange = if (archetype == EnemyArchetype.PRECISION_MARKSMAN) 56f else 42f
        val visionRange = (baseVisionRange * weatherVisionMult.coerceIn(0.4f, 1.0f))
        val canSeePlayer = playerAlive && distToPlayer <= visionRange && city.hasLineOfSight(eyePos, playerEyePos)

        // Defense Mode: enemies can also spot and assault the Command Core
        val coreEye = defenseCorePos?.let { Vec3(it.x, 1.4f, it.z) }
        val distToCore = defenseCorePos?.let { position.distanceXZ(it) } ?: Float.MAX_VALUE
        val canSeeCore = coreEye != null && distToCore <= visionRange * 1.05f && city.hasLineOfSight(eyePos, coreEye)
        if (defenseCorePos != null && !canSeePlayer && state == EnemyAiState.PATROL) {
            lastKnownPlayerPos = defenseCorePos
            awareness = max(awareness, 0.9f)
            state = EnemyAiState.INVESTIGATE
            stateTimer = 3.5f
        }

        if (canSeePlayer) {
            timeSincePlayerSeen = 0f
            lastKnownPlayerPos = playerPos
            val distGain = (1.6f - (distToPlayer / visionRange)).coerceIn(0.6f, 2.2f)
            val prevAware = awareness
            awareness = (awareness + dt * distGain * (1.25f / difficulty.enemyReactionTime)).coerceAtMost(1f)
            if (prevAware < 1f && awareness >= 1f) {
                eventBus.publish(GameplayEvent.EnemyAlerted(id, playerPos))
                if (archetype.canCoordinateSquad) {
                    squadCoordinator.broadcastAlert(this, playerPos, allies, simTime)
                }
            }
        } else {
            timeSincePlayerSeen += dt
            if (timeSincePlayerSeen > 4.5f) {
                awareness = (awareness - dt * 0.18f).coerceAtLeast(0f)
            }
        }

        // Reload state handling
        if (state == EnemyAiState.RELOAD) {
            squadCoordinator.releaseAttackSlot(id)
            reloadTimer -= dt
            if (reloadTimer <= 0f) {
                ammoInMag = archetype.magSize
                state = if (canSeePlayer) EnemyAiState.ENGAGE else EnemyAiState.SEARCH
                stateTimer = 2.2f
            }
            return false
        }

        if (ammoInMag <= 0) {
            squadCoordinator.releaseAttackSlot(id)
            state = EnemyAiState.RELOAD
            reloadTimer = if (archetype == EnemyArchetype.ARMORED_HEAVY || archetype.isBoss) 2.4f else 1.7f
            return false
        }

        if (state != EnemyAiState.ENGAGE) {
            squadCoordinator.releaseAttackSlot(id)
        }

        stateTimer -= dt
        var firedShot = false

        when (state) {
            EnemyAiState.IDLE -> {
                if (canSeePlayer && awareness >= 0.45f) {
                    state = if (awareness >= 0.95f) EnemyAiState.ALERT else EnemyAiState.SUSPICIOUS
                    stateTimer = difficulty.enemyReactionTime
                } else if (stateTimer <= 0f) {
                    state = EnemyAiState.PATROL
                }
            }
            EnemyAiState.PATROL -> {
                val target = if (patrolTargetB) patrolB else patrolA
                moveToward(target, dt, archetype.moveSpeed * 0.58f, city)
                if (position.distanceXZ(target) < 1.3f) {
                    patrolTargetB = !patrolTargetB
                    state = EnemyAiState.IDLE
                    stateTimer = 1.2f
                }
                if (canSeePlayer && awareness >= 0.35f) {
                    state = EnemyAiState.SUSPICIOUS
                    stateTimer = difficulty.enemyReactionTime * 1.15f
                }
            }
            EnemyAiState.SUSPICIOUS -> {
                faceTarget(lastKnownPlayerPos ?: playerPos, dt)
                if (canSeePlayer && awareness >= 0.95f) {
                    state = EnemyAiState.ALERT
                    stateTimer = difficulty.enemyReactionTime * 0.8f
                } else if (!canSeePlayer && stateTimer <= 0f) {
                    state = if (lastKnownPlayerPos != null) EnemyAiState.INVESTIGATE else EnemyAiState.PATROL
                    stateTimer = 2.6f
                }
            }
            EnemyAiState.INVESTIGATE -> {
                val dest = lastKnownPlayerPos ?: squadCoordinator.sharedLastKnownPlayerPos ?: defenseCorePos ?: patrolA
                moveToward(dest, dt, archetype.moveSpeed * 0.78f, city)
                if ((canSeePlayer && awareness >= 0.85f) || (canSeeCore && distToCore <= archetype.preferredRange + 4f)) {
                    state = EnemyAiState.ALERT
                    stateTimer = difficulty.enemyReactionTime * 0.7f
                } else if (position.distanceXZ(dest) < 2.2f || stateTimer <= 0f) {
                    state = if (defenseCorePos != null) EnemyAiState.ENGAGE else EnemyAiState.SEARCH
                    stateTimer = 3.0f
                }
            }
            EnemyAiState.ALERT -> {
                faceTarget(lastKnownPlayerPos ?: defenseCorePos ?: playerPos, dt)
                if (archetype.canCoordinateSquad && !hasRequestedSupport && allies.count { it.isAlive } > 1) {
                    state = EnemyAiState.REQUEST_SUPPORT
                    stateTimer = 0.65f
                    hasRequestedSupport = true
                } else if (stateTimer <= 0f) {
                    state = when {
                        !canSeePlayer && !canSeeCore -> EnemyAiState.INVESTIGATE
                        squadRole == SquadRole.MOBILE_FLANKER -> EnemyAiState.FLANK
                        squadRole == SquadRole.DEFENSIVE_UNIT || squadRole == SquadRole.PRECISION_SUPPORT -> EnemyAiState.TAKE_COVER
                        else -> EnemyAiState.ENGAGE
                    }
                    stateTimer = 2.5f
                }
            }
            EnemyAiState.REQUEST_SUPPORT -> {
                faceTarget(lastKnownPlayerPos ?: playerPos, dt)
                if (stateTimer <= 0f) {
                    squadCoordinator.broadcastAlert(this, lastKnownPlayerPos ?: playerPos, allies, simTime, radius = 32f)
                    state = if (canSeePlayer || canSeeCore) EnemyAiState.ENGAGE else EnemyAiState.TAKE_COVER
                    stateTimer = 2.4f
                }
            }
            EnemyAiState.SEARCH -> {
                yaw = Math3D.wrapAngle(yaw + dt * 1.1f)
                if (canSeePlayer || canSeeCore) {
                    state = EnemyAiState.ALERT
                    stateTimer = difficulty.enemyReactionTime * 0.75f
                } else if (stateTimer <= 0f) {
                    state = if (defenseCorePos != null) EnemyAiState.INVESTIGATE else EnemyAiState.PATROL
                }
            }
            EnemyAiState.TAKE_COVER -> {
                val cp = assignedCover ?: squadCoordinator.reserveCoverFor(id, position, city).also { assignedCover = it }
                if (cp != null && position.distanceXZ(cp.position) > 1.2f) {
                    moveToward(cp.position, dt, archetype.moveSpeed, city)
                } else {
                    faceTarget(lastKnownPlayerPos ?: defenseCorePos ?: playerPos, dt)
                    if (stateTimer <= 0f) {
                        state = if (canSeePlayer || canSeeCore) EnemyAiState.ENGAGE else EnemyAiState.REPOSITION
                        stateTimer = 2.4f
                    }
                }
            }
            EnemyAiState.ENGAGE -> {
                val targetCoreInstead = defenseCorePos != null && canSeeCore && (!canSeePlayer || distToCore + 3.5f < distToPlayer)
                val coreTarget = if (targetCoreInstead) defenseCorePos else null
                val engageTarget = coreTarget ?: playerPos
                val engageDist = if (targetCoreInstead) distToCore else distToPlayer
                faceTarget(engageTarget, dt)
                if (!canSeePlayer && !canSeeCore) {
                    squadCoordinator.releaseAttackSlot(id)
                    state = EnemyAiState.INVESTIGATE
                    stateTimer = 2.8f
                } else {
                    if (engageDist > archetype.preferredRange + 4f) {
                        moveToward(engageTarget, dt, archetype.moveSpeed * 0.76f, city)
                    } else if (!targetCoreInstead && engageDist < archetype.preferredRange * 0.45f && archetype == EnemyArchetype.PRECISION_MARKSMAN) {
                        state = EnemyAiState.RETREAT
                        stateTimer = 2.0f
                    }
                    // Staggered attack token prevents synchronized unfair instant-kill bursts
                    val hasSlot = squadCoordinator.requestAttackSlot(this)
                    if (hasSlot && fireCooldown <= 0f && ammoInMag > 0) {
                        ammoInMag -= 1
                        fireCooldown = 1f / archetype.fireRate
                        muzzleFlashTimer = 0.08f
                        lastShotTargetedCore = targetCoreInstead
                        firedShot = true
                    }
                    if (stateTimer <= 0f) {
                        squadCoordinator.releaseAttackSlot(id)
                        state = when {
                            squadRole == SquadRole.MOBILE_FLANKER -> EnemyAiState.FLANK
                            rng.nextFloat() < 0.48f -> EnemyAiState.TAKE_COVER
                            else -> EnemyAiState.REPOSITION
                        }
                        stateTimer = 2.2f
                    }
                }
            }
            EnemyAiState.FLANK -> {
                val target = lastKnownPlayerPos ?: playerPos
                val toPlayer = (target - position).normalizedXZ()
                val side = Vec3(-toPlayer.z * flankSide, 0f, toPlayer.x * flankSide)
                val flankDest = position + (toPlayer * 0.55f + side * 0.85f).normalizedXZ() * 4f
                moveToward(flankDest, dt, archetype.moveSpeed, city)
                if (canSeePlayer && (distToPlayer <= archetype.preferredRange || stateTimer <= 0f)) {
                    state = EnemyAiState.ENGAGE
                    stateTimer = 2.2f
                } else if (stateTimer <= 0f) {
                    state = EnemyAiState.INVESTIGATE
                    stateTimer = 2.5f
                }
            }
            EnemyAiState.REPOSITION -> {
                val target = lastKnownPlayerPos ?: playerPos
                val toTarget = (target - position).normalizedXZ()
                val strafe = Vec3(-toTarget.z * flankSide, 0f, toTarget.x * flankSide)
                moveToward(position + (toTarget * 0.7f + strafe * 0.6f) * 3.5f, dt, archetype.moveSpeed * 0.85f, city)
                if (canSeePlayer && stateTimer <= 0.8f) {
                    state = EnemyAiState.ENGAGE
                    stateTimer = 2.2f
                } else if (stateTimer <= 0f) {
                    state = if (lastKnownPlayerPos != null) EnemyAiState.INVESTIGATE else EnemyAiState.PATROL
                    stateTimer = 2.5f
                }
            }
            EnemyAiState.RETREAT -> {
                val away = (position - (lastKnownPlayerPos ?: playerPos)).normalizedXZ()
                moveToward(position + away * 5f, dt, archetype.moveSpeed * 0.9f, city)
                if (stateTimer <= 0f) {
                    state = EnemyAiState.TAKE_COVER
                    stateTimer = 2.2f
                }
            }
            EnemyAiState.RELOAD, EnemyAiState.DEFEATED -> Unit
        }
        return firedShot
    }

    private fun faceTarget(target: Vec3, dt: Float) {
        val desired = Math3D.yawFromDirection(target.x - position.x, target.z - position.z)
        yaw = Math3D.lerpAngle(yaw, desired, dt * 10f)
    }

    private fun moveToward(target: Vec3, dt: Float, speed: Float, city: CityMap) {
        val delta = Vec3(target.x - position.x, 0f, target.z - position.z)
        val dist = delta.lengthXZ()
        if (dist < 0.25f) return
        val dir = delta * (1f / dist)
        yaw = Math3D.lerpAngle(yaw, Math3D.yawFromDirection(dir.x, dir.z), dt * 9f)

        var nx = (position.x + dir.x * speed * dt).coerceIn(city.minBoundX, city.maxBoundX)
        var nz = (position.z + dir.z * speed * dt).coerceIn(city.minBoundZ, city.maxBoundZ)

        var blockedX = false
        var blockedZ = false
        for (i in city.colliders.indices) {
            val block = city.colliders[i]
            if (!block.isActive) continue
            val b = block.bounds
            if (b.intersectsCylinder(nx, 0.1f, height, position.z, radius)) blockedX = true
            if (b.intersectsCylinder(position.x, 0.1f, height, nz, radius)) blockedZ = true
        }
        if (blockedX) nx = position.x
        if (blockedZ) nz = position.z
        if (blockedX && blockedZ) {
            val sideX = (position.x - dir.z * flankSide * speed * dt).coerceIn(city.minBoundX, city.maxBoundX)
            val sideZ = (position.z + dir.x * flankSide * speed * dt).coerceIn(city.minBoundZ, city.maxBoundZ)
            var sideBlocked = false
            for (i in city.colliders.indices) {
                val block = city.colliders[i]
                if (!block.isActive) continue
                if (block.bounds.intersectsCylinder(sideX, 0.1f, height, sideZ, radius)) {
                    sideBlocked = true
                    break
                }
            }
            if (!sideBlocked) {
                nx = sideX; nz = sideZ
            } else {
                flankSide = -flankSide
            }
        }
        position = Vec3(nx, 0f, nz)
    }
}
