package com.example.game.engine

import com.example.game.data.CameraShoulder
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

data class CameraPose(
    val position: Vec3,
    val lookTarget: Vec3,
    val fov: Float,
    val shoulderSign: Float
)

/**
 * v1.1.0 Unified CameraDirector.
 * Smoothly blends third-person follow, ADS over-the-shoulder zoom, Left/Right shoulder switching,
 * cover-peek framing, vault/sprint FOV shift, collision-safe raycast boom, and reduced-motion shake.
 */
class CameraDirector(initialShoulder: CameraShoulder = CameraShoulder.RIGHT) {
    var shoulder: CameraShoulder = initialShoulder
    private var currentShoulderSign: Float = initialShoulder.sign
    private var currentBackDist: Float = 3.35f
    private var currentFov: Float = 65f
    private var currentHeightOffset: Float = 0.28f

    fun toggleShoulder(): CameraShoulder {
        shoulder = if (shoulder == CameraShoulder.RIGHT) CameraShoulder.LEFT else CameraShoulder.RIGHT
        return shoulder
    }

    fun step(dt: Float, player: PlayerController, inVehicle: Boolean = false) {
        val targetSign = if (inVehicle) 0f else shoulder.sign
        currentShoulderSign = Math3D.lerp(currentShoulderSign, targetSign, (dt * 10f).coerceIn(0f, 1f))

        val targetBack = when {
            inVehicle -> 5.45f
            player.isAiming -> 1.72f
            player.isVaulting -> 2.75f
            player.isSprinting -> 3.65f
            player.isInCover -> 2.85f
            else -> 3.35f
        }
        currentBackDist = Math3D.lerp(currentBackDist, targetBack, (dt * 9.5f).coerceIn(0f, 1f))

        val targetFov = when {
            inVehicle -> 72f
            player.isAiming -> player.activeWeapon.stats.adsZoomFov
            player.isSprinting -> 70f
            player.isVaulting -> 68f
            else -> 65f
        }
        currentFov = Math3D.lerp(currentFov, targetFov, (dt * 10.5f).coerceIn(0f, 1f))

        val targetHeight = when {
            inVehicle -> 1.45f
            player.isInCover && player.isAiming -> 0.42f // cover peek elevation
            player.isCrouching || player.isInCover -> 0.16f
            else -> 0.28f
        }
        currentHeightOffset = Math3D.lerp(currentHeightOffset, targetHeight, (dt * 8f).coerceIn(0f, 1f))
    }

    fun computePose(player: PlayerController, city: CityMap, reduceShake: Boolean, inVehicle: Boolean = false): CameraPose {
        val shoulderMag = if (inVehicle) 0.0f else if (player.isAiming) 0.62f else 0.76f
        val coverLeanBoost = if (!inVehicle && player.isInCover && player.isAiming) 0.28f else 0f
        val shoulderOffset = (shoulderMag + coverLeanBoost) * currentShoulderSign

        val pivot = Vec3(player.position.x, player.position.y + (if (inVehicle) 1.55f else player.eyeHeight), player.position.z)
        val right = Math3D.rightFromYaw(player.yaw)
        val swayPitch = if (!inVehicle && player.isAiming) player.weaponSwayPitch else 0f
        val swayYaw = if (!inVehicle && player.isAiming) player.weaponSwayYaw else 0f
        val lookDir = Math3D.forwardFromYawPitch(
            player.yaw + swayYaw,
            player.pitch + player.currentRecoilPitch + swayPitch
        )

        val desiredCam = pivot + right * shoulderOffset - lookDir * currentBackDist + Vec3(0f, currentHeightOffset, 0f)
        val toCam = desiredCam - pivot
        val dist = toCam.length().coerceAtLeast(0.01f)
        val dir = toCam * (1f / dist)

        var safeDist = dist
        val hit = city.raycastWorld(pivot, dir, dist)
        if (hit != null) {
            safeDist = max(0.55f, hit.first - 0.28f)
        }
        var camPos = pivot + dir * safeDist
        if (camPos.y < 0.45f) camPos = camPos.copy(y = 0.45f)

        if (!reduceShake && player.cameraShake > 0.01f) {
            val sx = sin(player.animTime * 47f) * 0.05f * player.cameraShake
            val sy = cos(player.animTime * 53f) * 0.04f * player.cameraShake
            camPos += Vec3(sx, sy, 0f)
        }

        val lookTarget = pivot + right * ((if (player.isAiming) 0.22f else 0.32f) * currentShoulderSign) + lookDir * 35f
        return CameraPose(camPos, lookTarget, currentFov, currentShoulderSign)
    }
}
