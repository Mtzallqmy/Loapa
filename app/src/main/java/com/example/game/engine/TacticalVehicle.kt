package com.example.game.engine

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * v2.1.0 Scoped Playable Ground Vehicle — Tactical Armored Rover.
 * Parked in open city avenues across all 4 cities. Supports boarding/exiting,
 * mobile joystick driving with AABB world collision sliding, roof turret firing,
 * hull armor absorption, and safe player ejection when disabled.
 */
class TacticalVehicle(
    val id: String,
    initialPosition: Vec3,
    initialYaw: Float = 0f
) {
    var position: Vec3 = initialPosition
    var yaw: Float = initialYaw
    var turretYaw: Float = initialYaw
    var speed: Float = 0f
    var wheelSpinRad: Float = 0f

    val maxHealth: Float = 280f
    var health: Float = maxHealth
    var isOccupied: Boolean = false
    var isDestroyed: Boolean = false
    var fireCooldown: Float = 0f
    var muzzleFlashTimer: Float = 0f

    val radius: Float = 1.45f
    val height: Float = 2.05f

    val canBoard: Boolean get() = !isDestroyed && !isOccupied

    fun board(player: PlayerController) {
        if (!canBoard) return
        isOccupied = true
        turretYaw = player.yaw
        player.isInCover = false
        player.isCrouching = false
        player.isVaulting = false
        player.position = Vec3(position.x, 0.35f, position.z)
    }

    /** Safely ejects the player onto an unobstructed side exit spot near the rover. */
    fun exit(player: PlayerController, city: CityMap) {
        if (!isOccupied) return
        isOccupied = false
        speed = 0f
        val right = Math3D.rightFromYaw(yaw)
        val candidates = listOf(
            position + right * 2.4f,
            position - right * 2.4f,
            position + Vec3(0f, 0f, 2.6f),
            position - Vec3(0f, 0f, 2.6f)
        )
        var exitPos = position + right * 2.4f
        for (c in candidates) {
            val clamped = Vec3(
                c.x.coerceIn(city.minBoundX + 0.8f, city.maxBoundX - 0.8f),
                0f,
                c.z.coerceIn(city.minBoundZ + 0.8f, city.maxBoundZ - 0.8f)
            )
            val blocked = city.colliders.any { blk ->
                blk.isActive && blk.bounds.intersectsCylinder(clamped.x, 0.1f, 1.8f, clamped.z, player.radius)
            }
            if (!blocked) {
                exitPos = clamped
                break
            }
        }
        player.position = exitPos
        player.velocity = Vec3(0f, 0f, 0f)
    }

    /** Steps driving physics, steering, turret tracking, and syncs player position when occupied. */
    fun step(dt: Float, input: PlayerInputState, player: PlayerController, city: CityMap) {
        if (fireCooldown > 0f) fireCooldown = max(0f, fireCooldown - dt)
        if (muzzleFlashTimer > 0f) muzzleFlashTimer = max(0f, muzzleFlashTimer - dt)

        if (!isOccupied || isDestroyed) {
            speed *= (1f - min(1f, dt * 6f))
            return
        }

        // Camera look rotates roof turret + player aim
        player.yaw = Math3D.wrapAngle(player.yaw + input.lookDeltaX)
        player.pitch = (player.pitch + input.lookDeltaY).coerceIn(-0.42f, 0.35f)
        turretYaw = player.yaw

        // Joystick Y = throttle/reverse, Joystick X = steering
        val throttle = input.moveY.coerceIn(-1f, 1f)
        val steer = input.moveX.coerceIn(-1f, 1f)

        val maxFwdSpeed = 12.2f
        val maxRevSpeed = -5.5f
        val accel = if (throttle >= 0f) 14.5f else 11.0f

        if (abs(throttle) > 0.08f) {
            speed = (speed + throttle * accel * dt).coerceIn(maxRevSpeed, maxFwdSpeed)
        } else {
            val drag = 10.5f * dt
            speed = when {
                speed > drag -> speed - drag
                speed < -drag -> speed + drag
                else -> 0f
            }
        }

        if (abs(speed) > 0.2f && abs(steer) > 0.06f) {
            val dirSign = if (speed >= 0f) 1f else -1f
            val turnRate = 1.95f * (abs(speed) / maxFwdSpeed).coerceIn(0.35f, 1f)
            yaw = Math3D.wrapAngle(yaw - steer * turnRate * dirSign * dt)
        }

        val fwd = Math3D.forwardFromYawPitch(yaw, 0f)
        val dx = fwd.x * speed * dt
        val dz = fwd.z * speed * dt

        var nx = (position.x + dx).coerceIn(city.minBoundX + 1.2f, city.maxBoundX - 1.2f)
        var nz = (position.z + dz).coerceIn(city.minBoundZ + 1.2f, city.maxBoundZ - 1.2f)

        var blockedX = false
        var blockedZ = false
        for (i in city.colliders.indices) {
            val blk = city.colliders[i]
            if (!blk.isActive) continue
            if (blk.bounds.intersectsCylinder(nx, 0.15f, height, position.z, radius)) blockedX = true
            if (blk.bounds.intersectsCylinder(position.x, 0.15f, height, nz, radius)) blockedZ = true
        }
        if (blockedX) {
            nx = position.x
            speed *= 0.45f
        }
        if (blockedZ) {
            nz = position.z
            speed *= 0.45f
        }

        position = Vec3(nx, 0f, nz)
        wheelSpinRad += speed * dt * 2.2f

        // Sync driver transform
        player.position = Vec3(position.x, 0.35f, position.z)
        player.bodyYaw = yaw
        player.totalDistanceMoved += abs(speed * dt)
    }

    fun tryFireTurret(): Boolean {
        if (!isOccupied || isDestroyed || fireCooldown > 0f) return false
        fireCooldown = 0.17f // ~350 RPM heavy mounted autocannon
        muzzleFlashTimer = 0.07f
        return true
    }

    fun turretMuzzleWorldPos(): Vec3 {
        val fwd = Math3D.forwardFromYawPitch(turretYaw, 0f)
        return Vec3(position.x + fwd.x * 1.15f, position.y + 2.15f, position.z + fwd.z * 1.15f)
    }

    /** Applies incoming enemy damage to vehicle hull. Returns true if hull was just disabled. */
    fun applyHullDamage(amount: Float): Boolean {
        if (isDestroyed) return false
        health = max(0f, health - amount)
        if (health <= 0f) {
            isDestroyed = true
            speed = 0f
            return true
        }
        return false
    }
}
