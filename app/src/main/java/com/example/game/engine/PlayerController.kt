package com.example.game.engine

import com.example.game.data.EffectiveWeaponStats
import com.example.game.data.LowerBodyAnimState
import com.example.game.data.PlayerAnimState
import com.example.game.data.UpperBodyCombatState
import com.example.game.data.WeaponId
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

data class PlayerInputState(
    val moveX: Float = 0f,       // -1..1 right
    val moveY: Float = 0f,       // -1..1 forward
    val lookDeltaX: Float = 0f,  // radians yaw
    val lookDeltaY: Float = 0f,  // radians pitch
    val fireHeld: Boolean = false,
    val aimHeld: Boolean = false,
    val sprintHeld: Boolean = false,
    val crouchToggled: Boolean = false,
    val jumpPressed: Boolean = false,
    val reloadPressed: Boolean = false,
    val coverPressed: Boolean = false,
    val switchWeaponPressed: Boolean = false,
    val interactPressed: Boolean = false,
    val vaultPressed: Boolean = false,
    val toggleShoulderPressed: Boolean = false
)

data class WeaponSlotRuntime(
    val stats: EffectiveWeaponStats,
    var ammoInMag: Int,
    var ammoReserve: Int
)

/**
 * v1.1.0 Upgraded PlayerController with:
 * - Layered Lower-Body (10 states) + Upper-Body (8 states) Animation Coordinator
 * - Weapon-weight movement speed, natural acceleration/deceleration
 * - Waist-high cover Vaulting, Cover Leaning while ADS, Cover Switching
 * - Airborne Fall state + Fall Damage calculation
 * - Reload cancellation on sprint/vault/weapon-switch
 * - ADS Weapon Sway, Hit Reaction/Stagger, and Respawn Protection
 */
class PlayerController(
    spawnPos: Vec3,
    loadoutStats: List<EffectiveWeaponStats>
) {
    var position: Vec3 = spawnPos
    var velocity: Vec3 = Vec3()
    var yaw: Float = 0f          // 0 faces -Z
    var pitch: Float = -0.06f
    var bodyYaw: Float = 0f

    var health: Float = 100f
    var maxHealth: Float = 100f
    var armor: Float = 100f
    var maxArmor: Float = 100f
    var stamina: Float = 100f

    var isGrounded: Boolean = true
    var isCrouching: Boolean = false
    var isSprinting: Boolean = false
    var isAiming: Boolean = false
    var isInCover: Boolean = false
    var isLeaningFromCover: Boolean = false
    var coverNormal: Vec3 = Vec3(0f, 0f, -1f)
    var canEnterCover: Boolean = false

    // v1.1.0 Vaulting
    var canVault: Boolean = false
    var isVaulting: Boolean = false
    private var vaultTimer: Float = 0f
    private val vaultDuration: Float = 0.42f
    private var vaultStartPos: Vec3 = Vec3()
    private var vaultEndPos: Vec3 = Vec3()

    // v1.1.0 Fall tracking & Respawn Protection
    private var peakAirY: Float = 0f
    var pendingFallDamage: Float = 0f
    var respawnProtectionTimer: Float = 0f

    // v1.1.0 Layered Animation States + Legacy Unified AnimState
    var animState: PlayerAnimState = PlayerAnimState.IDLE
    var lowerBodyState: LowerBodyAnimState = LowerBodyAnimState.IDLE
    var upperBodyState: UpperBodyCombatState = UpperBodyCombatState.HIP_FIRE
    var animTime: Float = 0f

    val weaponSlots: MutableList<WeaponSlotRuntime> = loadoutStats.map { s ->
        WeaponSlotRuntime(s, s.magCapacity, s.reserveMax)
    }.toMutableList()
    var activeSlotIndex: Int = 0
        private set

    val activeWeapon: WeaponSlotRuntime get() = weaponSlots[activeSlotIndex]
    val activeWeaponId: WeaponId get() = activeWeapon.stats.weapon.id

    var isReloading: Boolean = false
    var reloadTimer: Float = 0f
    var weaponSwitchTimer: Float = 0f
    var fireCooldown: Float = 0f
    var currentRecoilPitch: Float = 0f
    var currentSpread: Float = 0.02f
    var weaponSwayYaw: Float = 0f
    var weaponSwayPitch: Float = 0f
    var muzzleFlashTimer: Float = 0f
    var damageFlashTimer: Float = 0f
    var staggerTimer: Float = 0f
    var armorBreakFlashTimer: Float = 0f
    var lastDamageAngleRad: Float = 0f
    var timeSinceLastDamage: Float = 10f
    var landingTimer: Float = 0f
    var footstepAccum: Float = 0f
    var totalDistanceMoved: Float = 0f
    var cameraShake: Float = 0f

    // Track whether reload just finished this step (for EventBus publishing)
    var justCompletedReloadWeapon: WeaponId? = null
        private set

    val radius: Float = 0.42f
    val eyeHeight: Float
        get() = when {
            isVaulting -> 1.38f
            isInCover && isAiming -> 1.48f // peeking over cover
            isCrouching || isInCover -> 1.05f
            else -> 1.62f
        }
    val height: Float get() = if (isCrouching || isInCover) 1.25f else 1.82f
    val isAlive: Boolean get() = health > 0f

    fun resetToCheckpoint(pos: Vec3, savedHealth: Float, savedArmor: Float, savedMag: Int, savedReserve: Int) {
        position = pos
        velocity = Vec3()
        health = savedHealth.coerceIn(35f, maxHealth)
        armor = savedArmor.coerceIn(0f, maxArmor)
        stamina = 100f
        isReloading = false
        reloadTimer = 0f
        isVaulting = false
        vaultTimer = 0f
        isInCover = false
        isLeaningFromCover = false
        isCrouching = false
        damageFlashTimer = 0f
        staggerTimer = 0f
        pendingFallDamage = 0f
        peakAirY = pos.y
        respawnProtectionTimer = 2.6f // 2.6s respawn shield against spawn traps
        animState = PlayerAnimState.IDLE
        lowerBodyState = LowerBodyAnimState.IDLE
        upperBodyState = UpperBodyCombatState.HIP_FIRE
        activeWeapon.ammoInMag = savedMag.coerceIn(1, activeWeapon.stats.magCapacity)
        activeWeapon.ammoReserve = savedReserve.coerceIn(0, activeWeapon.stats.reserveMax)
    }

    fun switchWeapon(nextIndex: Int? = null): Boolean {
        if (!isAlive || weaponSlots.size <= 1) return false
        val target = nextIndex?.coerceIn(0, weaponSlots.lastIndex) ?: ((activeSlotIndex + 1) % weaponSlots.size)
        if (target == activeSlotIndex) return false
        activeSlotIndex = target
        cancelReload()
        weaponSwitchTimer = 0.28f
        fireCooldown = 0.26f
        return true
    }

    fun startReload(): Boolean {
        if (!isAlive || isReloading || isVaulting) return false
        val w = activeWeapon
        if (w.ammoInMag >= w.stats.magCapacity || w.ammoReserve <= 0) return false
        isReloading = true
        reloadTimer = w.stats.reloadDuration
        return true
    }

    fun cancelReload(): Boolean {
        if (!isReloading) return false
        isReloading = false
        reloadTimer = 0f
        return true
    }

    fun resupplyAll() {
        weaponSlots.forEach { slot ->
            slot.ammoReserve = slot.stats.reserveMax
            if (slot.ammoInMag == 0) {
                val fill = min(slot.stats.magCapacity, slot.ammoReserve)
                slot.ammoInMag = fill
                slot.ammoReserve -= fill
            }
        }
        health = min(maxHealth, health + 35f)
        armor = maxArmor
    }

    fun applyDamage(
        amount: Float,
        sourcePos: Vec3,
        reduceShake: Boolean,
        bypassArmor: Boolean = false
    ): Boolean {
        if (!isAlive || amount <= 0f) return false
        var remaining = amount
        if (!bypassArmor && armor > 0f) {
            val absorbed = min(armor, remaining * 0.68f)
            armor = (armor - absorbed).coerceAtLeast(0f)
            remaining -= absorbed
            if (armor <= 0.1f) armorBreakFlashTimer = 0.45f
        }
        health = (health - remaining).coerceAtLeast(0f)
        damageFlashTimer = 0.42f
        if (remaining >= 22f) staggerTimer = 0.26f
        timeSinceLastDamage = 0f
        if (!reduceShake) cameraShake = min(1f, cameraShake + 0.32f)

        val toSource = sourcePos - position
        val worldAngle = Math3D.yawFromDirection(toSource.x, toSource.z)
        lastDamageAngleRad = Math3D.wrapAngle(worldAngle - yaw)

        if (health <= 0f) {
            animState = PlayerAnimState.DEATH
            upperBodyState = UpperBodyCombatState.DEATH
            isReloading = false
            isVaulting = false
            return true
        }
        return false
    }

    /** Finds a waist-high vaultable obstacle directly ahead of the player and returns target landing point. */
    private fun findVaultDestination(city: CityMap): Vec3? {
        val fwd = Vec3(-sin(yaw), 0f, -cos(yaw))
        val probe = position + fwd * 1.15f
        for (i in city.blocks.indices) {
            val b = city.blocks[i]
            if (!b.solid || !b.isActive || !b.isVaultable) continue
            if (b.bounds.intersectsCylinder(probe.x, 0.1f, 1.4f, probe.z, radius + 0.35f)) {
                val clearDist = max(b.bounds.sizeX, b.bounds.sizeZ).coerceAtMost(2.2f) + 1.15f
                val dest = Vec3(
                    (position.x + fwd.x * clearDist).coerceIn(city.minBoundX + 0.6f, city.maxBoundX - 0.6f),
                    0f,
                    (position.z + fwd.z * clearDist).coerceIn(city.minBoundZ + 0.6f, city.maxBoundZ - 0.6f)
                )
                // Ensure landing zone is unobstructed
                val blocked = city.colliders.any { it.isActive && it.bounds.intersectsCylinder(dest.x, 0.1f, 1.8f, dest.z, radius) }
                if (!blocked) return dest
            }
        }
        return null
    }

    /**
     * Steps movement, vaulting, gravity, fall detection, AABB sliding, cover, reload, and layered animation FSM.
     * Returns true if a footstep sound should be emitted this tick.
     */
    fun step(
        dt: Float,
        input: PlayerInputState,
        city: CityMap,
        reduceShake: Boolean
    ): Boolean {
        animTime += dt
        timeSinceLastDamage += dt
        justCompletedReloadWeapon = null
        pendingFallDamage = 0f

        if (respawnProtectionTimer > 0f) respawnProtectionTimer = max(0f, respawnProtectionTimer - dt)
        if (fireCooldown > 0f) fireCooldown = max(0f, fireCooldown - dt)
        if (weaponSwitchTimer > 0f) weaponSwitchTimer = max(0f, weaponSwitchTimer - dt)
        if (muzzleFlashTimer > 0f) muzzleFlashTimer = max(0f, muzzleFlashTimer - dt)
        if (damageFlashTimer > 0f) damageFlashTimer = max(0f, damageFlashTimer - dt)
        if (staggerTimer > 0f) staggerTimer = max(0f, staggerTimer - dt)
        if (armorBreakFlashTimer > 0f) armorBreakFlashTimer = max(0f, armorBreakFlashTimer - dt)
        if (landingTimer > 0f) landingTimer = max(0f, landingTimer - dt)
        if (cameraShake > 0f) cameraShake = max(0f, cameraShake - dt * 3.2f)

        if (!isAlive) {
            animState = PlayerAnimState.DEATH
            upperBodyState = UpperBodyCombatState.DEATH
            return false
        }

        // Camera look
        yaw = Math3D.wrapAngle(yaw + input.lookDeltaX)
        pitch = (pitch + input.lookDeltaY).coerceIn(-0.82f, 0.72f)
        currentRecoilPitch = Math3D.lerp(currentRecoilPitch, 0f, dt * 8.5f)

        // Subtle organic ADS breathing sway
        val swayAmp = if (isCrouching || isInCover) 0.0022f else 0.0045f
        weaponSwayYaw = sin(animTime * 1.9f) * swayAmp
        weaponSwayPitch = cos(animTime * 1.35f) * swayAmp * 0.8f

        // Active vault interpolation
        if (isVaulting) {
            vaultTimer += dt
            val t = (vaultTimer / vaultDuration).coerceIn(0f, 1f)
            val arcY = sin(t * kotlin.math.PI.toFloat()) * 1.38f
            val nx = Math3D.lerp(vaultStartPos.x, vaultEndPos.x, t)
            val nz = Math3D.lerp(vaultStartPos.z, vaultEndPos.z, t)
            position = Vec3(nx, arcY, nz)
            velocity = Vec3(0f, 0f, 0f)
            if (t >= 1f) {
                isVaulting = false
                position = vaultEndPos.copy(y = 0f)
                isGrounded = true
                landingTimer = 0.18f
            }
            updateAnimationStates(0f)
            return false
        }

        // Weapon switch & reload
        if (input.switchWeaponPressed) switchWeapon()
        if (input.reloadPressed) startReload()
        if (isReloading) {
            reloadTimer -= dt
            if (reloadTimer <= 0f) {
                val w = activeWeapon
                val needed = w.stats.magCapacity - w.ammoInMag
                val take = min(needed, w.ammoReserve)
                w.ammoInMag += take
                w.ammoReserve -= take
                isReloading = false
                reloadTimer = 0f
                justCompletedReloadWeapon = w.stats.weapon.id
            }
        }

        // Cover availability, snapping, and cover switching
        val nearestCover = city.coverPoints.minByOrNull { it.position.distanceXZ(position) }
        val nearCoverDist = nearestCover?.position?.distanceXZ(position) ?: 999f
        canEnterCover = nearCoverDist < 2.25f
        if (input.coverPressed) {
            if (isInCover) {
                // Check if another cover point is nearby for smooth cover switching
                val altCover = city.coverPoints
                    .filter { it.position.distanceXZ(position) in 1.1f..3.0f }
                    .minByOrNull { it.position.distanceXZ(position) }
                if (altCover != null && input.moveX * input.moveX + input.moveY * input.moveY > 0.1f) {
                    coverNormal = altCover.normal
                    position = Vec3(altCover.position.x, position.y, altCover.position.z)
                } else {
                    isInCover = false
                }
            } else if (nearestCover != null && nearCoverDist < 2.25f) {
                isInCover = true
                isCrouching = true
                coverNormal = nearestCover.normal
                position = Vec3(nearestCover.position.x, position.y, nearestCover.position.z)
            }
        }

        // Vault availability & initiation over waist-high cover
        val vaultDest = if (isGrounded) findVaultDestination(city) else null
        canVault = vaultDest != null
        if ((input.vaultPressed || (input.jumpPressed && isInCover)) && vaultDest != null) {
            cancelReload()
            isInCover = false
            isCrouching = false
            isSprinting = false
            isAiming = false
            isVaulting = true
            vaultTimer = 0f
            vaultStartPos = position
            vaultEndPos = vaultDest
            updateAnimationStates(0f)
            return false
        }

        isAiming = input.aimHeld && !isReloading && weaponSwitchTimer <= 0f
        isLeaningFromCover = isInCover && isAiming
        isCrouching = if (isInCover) true else input.crouchToggled

        val moveMag = sqrt(input.moveX * input.moveX + input.moveY * input.moveY).coerceAtMost(1f)
        if (isInCover && moveMag > 0.65f) {
            isInCover = false
            isLeaningFromCover = false
        }

        // Sprint cancels reload if player actively pushes sprint forward
        val wantsSprint = input.sprintHeld && input.moveY > 0.35f && !isAiming && !isCrouching && !isInCover && stamina > 5f
        if (wantsSprint && isReloading) {
            cancelReload()
        }
        isSprinting = wantsSprint
        if (isSprinting) {
            stamina = max(0f, stamina - dt * 18f)
        } else {
            stamina = min(100f, stamina + dt * 14f)
        }

        // Passive tactical recovery out of combat
        if (timeSinceLastDamage > 6f) {
            if (armor < maxArmor) armor = min(maxArmor, armor + dt * 6.5f)
            if (health < maxHealth * 0.55f) health = min(maxHealth * 0.55f, health + dt * 4.5f)
        }

        // Camera-relative horizontal movement weighted by weapon mobility
        val forward = Vec3(-sin(yaw), 0f, -cos(yaw))
        val right = Vec3(cos(yaw), 0f, -sin(yaw))
        val wishDir = (forward * input.moveY + right * input.moveX)
        val wishLen = wishDir.lengthXZ()
        val normWish = if (wishLen > 1e-4f) wishDir * (1f / wishLen) else Vec3()

        val mobility = activeWeaponId.mobilityMultiplier
        val maxSpeed = when {
            isInCover -> 0f
            staggerTimer > 0f -> 2.1f
            isCrouching -> 2.7f * mobility
            isAiming -> 3.0f * mobility
            isSprinting -> 7.4f * mobility
            else -> 4.9f * mobility
        }

        val targetVx = normWish.x * maxSpeed * moveMag
        val targetVz = normWish.z * maxSpeed * moveMag
        val accel = if (isGrounded) (if (moveMag > 0.05f) 12.5f else 15.0f) else 4.5f
        val newVx = Math3D.lerp(velocity.x, targetVx, (dt * accel).coerceIn(0f, 1f))
        val newVz = Math3D.lerp(velocity.z, targetVz, (dt * accel).coerceIn(0f, 1f))

        var newVy = velocity.y
        if (input.jumpPressed && isGrounded) {
            newVy = 6.2f
            isGrounded = false
            peakAirY = position.y
            isCrouching = false
            isInCover = false
        }
        newVy -= 17.5f * dt

        val oldPos = position
        var nextX = (position.x + newVx * dt).coerceIn(city.minBoundX, city.maxBoundX)
        var nextZ = (position.z + newVz * dt).coerceIn(city.minBoundZ, city.maxBoundZ)
        var nextY = position.y + newVy * dt

        // Separate-axis AABB collision sliding (respecting dynamic active doors/gates)
        for (i in city.colliders.indices) {
            val block = city.colliders[i]
            if (!block.isActive) continue
            val box = block.bounds
            if (box.intersectsCylinder(nextX, position.y + 0.1f, position.y + height, position.z, radius)) {
                nextX = position.x
            }
            if (box.intersectsCylinder(nextX, position.y + 0.1f, position.y + height, nextZ, radius)) {
                nextZ = position.z
            }
        }

        // Ground, peak fall tracking, and landing check
        val wasGrounded = isGrounded
        if (nextY > peakAirY) peakAirY = nextY
        if (nextY <= 0f) {
            nextY = 0f
            newVy = 0f
            isGrounded = true
            if (!wasGrounded) {
                landingTimer = 0.22f
                val fallHeight = peakAirY - nextY
                if (fallHeight > 3.6f) {
                    pendingFallDamage = ((fallHeight - 3.6f) * 14f).coerceAtMost(85f)
                }
            }
            peakAirY = 0f
        } else {
            isGrounded = false
        }

        position = Vec3(nextX, nextY, nextZ)
        velocity = Vec3(newVx, newVy, newVz)

        val stepDist = position.distanceXZ(oldPos)
        totalDistanceMoved += stepDist

        bodyYaw = if (isAiming || muzzleFlashTimer > 0f) {
            Math3D.lerpAngle(bodyYaw, yaw, dt * 16f)
        } else if (stepDist > 0.005f) {
            Math3D.lerpAngle(bodyYaw, Math3D.yawFromDirection(position.x - oldPos.x, position.z - oldPos.z), dt * 10f)
        } else {
            Math3D.lerpAngle(bodyYaw, yaw, dt * 4f)
        }

        val speedFactor = (velocity.lengthXZ() / 7.4f).coerceIn(0f, 1f)
        val baseSpread = if (isAiming) activeWeapon.stats.adsSpread else activeWeapon.stats.hipSpread
        val targetSpread = baseSpread * (1f + speedFactor * 0.65f) * (if (isCrouching || isInCover) 0.72f else 1f)
        currentSpread = Math3D.lerp(currentSpread, targetSpread, dt * 9f)

        val horizSpeed = velocity.lengthXZ()
        updateAnimationStates(horizSpeed)

        // Footstep cadence
        var emitFootstep = false
        if (isGrounded && horizSpeed > 0.5f) {
            footstepAccum += stepDist
            val stride = if (isSprinting) 2.2f else if (isCrouching) 1.4f else 1.85f
            if (footstepAccum >= stride) {
                footstepAccum = 0f
                emitFootstep = true
            }
        }
        return emitFootstep
    }

    private fun updateAnimationStates(horizSpeed: Float) {
        // Lower-body locomotion layer (10 states)
        lowerBodyState = when {
            isVaulting -> LowerBodyAnimState.VAULT
            !isGrounded && velocity.y < -1.2f -> LowerBodyAnimState.FALL
            !isGrounded -> LowerBodyAnimState.JUMP
            landingTimer > 0f -> LowerBodyAnimState.LAND
            isInCover -> LowerBodyAnimState.COVER
            isCrouching -> LowerBodyAnimState.CROUCH
            isSprinting && horizSpeed > 5.0f -> LowerBodyAnimState.SPRINT
            horizSpeed > 3.2f -> LowerBodyAnimState.RUN
            horizSpeed > 0.35f -> LowerBodyAnimState.WALK
            else -> LowerBodyAnimState.IDLE
        }

        // Upper-body combat layer (8 states)
        upperBodyState = when {
            !isAlive -> UpperBodyCombatState.DEATH
            staggerTimer > 0f -> UpperBodyCombatState.STAGGER
            damageFlashTimer > 0.28f -> UpperBodyCombatState.HIT_REACTION
            weaponSwitchTimer > 0f -> UpperBodyCombatState.WEAPON_SWITCH
            isReloading -> UpperBodyCombatState.RELOAD
            muzzleFlashTimer > 0f -> UpperBodyCombatState.FIRE
            isAiming -> UpperBodyCombatState.AIM
            else -> UpperBodyCombatState.HIP_FIRE
        }

        // Unified summary state (preserves full v1.0.0 compatibility + adds VAULT/FALL)
        animState = when {
            !isAlive -> PlayerAnimState.DEATH
            isVaulting -> PlayerAnimState.VAULT
            damageFlashTimer > 0.28f -> PlayerAnimState.TAKE_DAMAGE
            !isGrounded && velocity.y < -1.2f -> PlayerAnimState.FALL
            !isGrounded -> PlayerAnimState.JUMP
            landingTimer > 0f -> PlayerAnimState.LAND
            isReloading -> PlayerAnimState.RELOAD
            muzzleFlashTimer > 0f -> PlayerAnimState.SHOOT
            isInCover -> PlayerAnimState.COVER
            isAiming -> PlayerAnimState.AIM
            isCrouching -> PlayerAnimState.CROUCH
            isSprinting && horizSpeed > 5.0f -> PlayerAnimState.SPRINT
            horizSpeed > 3.2f -> PlayerAnimState.RUN
            horizSpeed > 0.35f -> PlayerAnimState.WALK
            else -> PlayerAnimState.IDLE
        }
    }

    /** Backwards-compatible camera helper delegating to CameraDirector pose math. */
    fun computeCamera(city: CityMap, reduceShake: Boolean, cameraDirector: CameraDirector? = null): Pair<Vec3, Vec3> {
        if (cameraDirector != null) {
            val pose = cameraDirector.computePose(this, city, reduceShake)
            return pose.position to pose.lookTarget
        }
        val pose = CameraDirector().computePose(this, city, reduceShake)
        return pose.position to pose.lookTarget
    }

    fun getAimOriginAndDirection(city: CityMap, cameraDirector: CameraDirector? = null): Pair<Vec3, Vec3> {
        val (camPos, lookTarget) = computeCamera(city, reduceShake = true, cameraDirector = cameraDirector)
        val dir = (lookTarget - camPos).normalized()
        return camPos to dir
    }

    fun tryFire(reduceShake: Boolean): Boolean {
        if (!isAlive || isReloading || isVaulting || weaponSwitchTimer > 0f || fireCooldown > 0f) return false
        val w = activeWeapon
        if (w.ammoInMag <= 0) {
            startReload()
            return false
        }
        w.ammoInMag -= 1
        fireCooldown = 1f / w.stats.fireRate
        muzzleFlashTimer = 0.075f
        val kick = w.stats.recoilKick * (if (isAiming) 0.72f else 1f) * (if (isCrouching || isInCover) 0.75f else 1f)
        currentRecoilPitch = min(0.24f, currentRecoilPitch + kick)
        currentSpread = min(0.14f, currentSpread + kick * 0.65f)
        if (!reduceShake) cameraShake = min(0.8f, cameraShake + kick * 6f)
        return true
    }
}
