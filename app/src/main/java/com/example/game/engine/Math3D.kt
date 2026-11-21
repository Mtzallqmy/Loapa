package com.example.game.engine

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

data class Vec3(val x: Float = 0f, val y: Float = 0f, val z: Float = 0f) {
    operator fun plus(o: Vec3) = Vec3(x + o.x, y + o.y, z + o.z)
    operator fun minus(o: Vec3) = Vec3(x - o.x, y - o.y, z - o.z)
    operator fun times(s: Float) = Vec3(x * s, y * s, z * s)
    fun lengthSq(): Float = x * x + y * y + z * z
    fun length(): Float = sqrt(lengthSq())
    fun lengthXZ(): Float = sqrt(x * x + z * z)
    fun dot(o: Vec3): Float = x * o.x + y * o.y + z * o.z
    fun normalized(): Vec3 {
        val len = length()
        return if (len > 1e-5f) Vec3(x / len, y / len, z / len) else Vec3(0f, 0f, -1f)
    }
    fun normalizedXZ(): Vec3 {
        val len = lengthXZ()
        return if (len > 1e-5f) Vec3(x / len, 0f, z / len) else Vec3(0f, 0f, -1f)
    }
    fun distanceTo(o: Vec3): Float = (this - o).length()
    fun distanceXZ(o: Vec3): Float {
        val dx = x - o.x
        val dz = z - o.z
        return sqrt(dx * dx + dz * dz)
    }
}

data class AABB(
    val minX: Float, val minY: Float, val minZ: Float,
    val maxX: Float, val maxY: Float, val maxZ: Float
) {
    val centerX: Float get() = (minX + maxX) * 0.5f
    val centerY: Float get() = (minY + maxY) * 0.5f
    val centerZ: Float get() = (minZ + maxZ) * 0.5f
    val sizeX: Float get() = maxX - minX
    val sizeY: Float get() = maxY - minY
    val sizeZ: Float get() = maxZ - minZ

    fun intersectsXZ(cx: Float, cz: Float, radius: Float): Boolean {
        val closestX = cx.coerceIn(minX, maxX)
        val closestZ = cz.coerceIn(minZ, maxZ)
        val dx = cx - closestX
        val dz = cz - closestZ
        return dx * dx + dz * dz < radius * radius
    }

    fun intersectsCylinder(cx: Float, baseY: Float, topY: Float, cz: Float, radius: Float): Boolean {
        if (topY <= minY + 0.05f || baseY >= maxY - 0.05f) return false
        return intersectsXZ(cx, cz, radius)
    }

    /** Ray-AABB slab intersection returning distance t >= 0, or null if missed. */
    fun intersectRay(origin: Vec3, dir: Vec3, maxDist: Float): Float? {
        var tMin = 0f
        var tMax = maxDist

        fun slab(o: Float, d: Float, lo: Float, hi: Float): Boolean {
            if (kotlin.math.abs(d) < 1e-6f) {
                return o >= lo && o <= hi
            }
            var t1 = (lo - o) / d
            var t2 = (hi - o) / d
            if (t1 > t2) {
                val tmp = t1; t1 = t2; t2 = tmp
            }
            tMin = max(tMin, t1)
            tMax = min(tMax, t2)
            return tMin <= tMax
        }

        if (!slab(origin.x, dir.x, minX, maxX)) return null
        if (!slab(origin.y, dir.y, minY, maxY)) return null
        if (!slab(origin.z, dir.z, minZ, maxZ)) return null
        return if (tMin >= 0f && tMin <= maxDist) tMin else null
    }
}

object Math3D {
    fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t.coerceIn(0f, 1f)

    fun wrapAngle(rad: Float): Float {
        var a = rad
        val twoPi = (2.0 * PI).toFloat()
        while (a > PI) a -= twoPi
        while (a < -PI) a += twoPi
        return a
    }

    fun lerpAngle(from: Float, to: Float, t: Float): Float {
        val diff = wrapAngle(to - from)
        return from + diff * t.coerceIn(0f, 1f)
    }

    fun forwardFromYawPitch(yaw: Float, pitch: Float): Vec3 {
        val cp = cos(pitch)
        return Vec3(
            x = -sin(yaw) * cp,
            y = sin(pitch),
            z = -cos(yaw) * cp
        ).normalized()
    }

    fun rightFromYaw(yaw: Float): Vec3 = Vec3(cos(yaw), 0f, -sin(yaw))

    fun yawFromDirection(dx: Float, dz: Float): Float = atan2(-dx, -dz)
}
