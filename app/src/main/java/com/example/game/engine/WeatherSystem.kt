package com.example.game.engine

import com.example.game.data.CityDefinition
import com.example.game.data.WeatherCondition
import kotlin.random.Random

data class WeatherLightingProfile(
    val zenithRgb: FloatArray,
    val horizonRgb: FloatArray,
    val fogRgb: FloatArray,
    val sunColorRgb: FloatArray,
    val ambientRgb: FloatArray,
    val fogDensity: Float,
    val nightFactor: Float
)

data class WeatherParticle(
    var x: Float,
    var y: Float,
    var z: Float,
    var vx: Float,
    var vy: Float,
    var vz: Float,
    val sx: Float,
    val sy: Float,
    val sz: Float,
    val colorRgb: FloatArray
)

/**
 * v2.1.0 Dynamic Weather & Environmental System.
 * Drives lighting/fog mood, 3D animated rain/dust/sandstorm particles (pre-allocated pool),
 * AI enemy vision range scaling, and periodic weather ambient audio cues.
 */
class WeatherSystem(
    val condition: WeatherCondition,
    private val reduceEffects: Boolean,
    seed: Long = 202605L
) {
    private val rng = Random(seed)
    val particles: MutableList<WeatherParticle> = mutableListOf()
    private var audioPulseTimer: Float = 2.5f

    val aiVisionMultiplier: Float get() = condition.aiVisionMultiplier

    fun lightingProfile(cityDef: CityDefinition): WeatherLightingProfile {
        return when (condition) {
            WeatherCondition.CLEAR_DAY -> WeatherLightingProfile(
                zenithRgb = cityDef.skyZenithRgb,
                horizonRgb = cityDef.skyHorizonRgb,
                fogRgb = cityDef.fogRgb,
                sunColorRgb = cityDef.sunColorRgb,
                ambientRgb = cityDef.ambientRgb,
                fogDensity = condition.fogDensity,
                nightFactor = 0f
            )
            WeatherCondition.SUNSET -> WeatherLightingProfile(
                zenithRgb = floatArrayOf(0.24f, 0.16f, 0.28f),
                horizonRgb = floatArrayOf(0.92f, 0.48f, 0.24f),
                fogRgb = floatArrayOf(0.76f, 0.46f, 0.30f),
                sunColorRgb = floatArrayOf(1.0f, 0.62f, 0.32f),
                ambientRgb = floatArrayOf(0.38f, 0.28f, 0.24f),
                fogDensity = condition.fogDensity,
                nightFactor = 0.25f
            )
            WeatherCondition.NIGHT -> WeatherLightingProfile(
                zenithRgb = floatArrayOf(0.03f, 0.05f, 0.11f),
                horizonRgb = floatArrayOf(0.10f, 0.14f, 0.22f),
                fogRgb = floatArrayOf(0.08f, 0.11f, 0.18f),
                sunColorRgb = floatArrayOf(0.30f, 0.40f, 0.58f),
                ambientRgb = floatArrayOf(0.14f, 0.17f, 0.25f),
                fogDensity = condition.fogDensity,
                nightFactor = 1.0f
            )
            WeatherCondition.RAIN -> WeatherLightingProfile(
                zenithRgb = floatArrayOf(0.16f, 0.21f, 0.26f),
                horizonRgb = floatArrayOf(0.36f, 0.42f, 0.47f),
                fogRgb = floatArrayOf(0.34f, 0.40f, 0.45f),
                sunColorRgb = floatArrayOf(0.55f, 0.62f, 0.70f),
                ambientRgb = floatArrayOf(0.26f, 0.30f, 0.35f),
                fogDensity = condition.fogDensity,
                nightFactor = 0.35f
            )
            WeatherCondition.FOG -> WeatherLightingProfile(
                zenithRgb = floatArrayOf(0.50f, 0.54f, 0.58f),
                horizonRgb = floatArrayOf(0.70f, 0.74f, 0.77f),
                fogRgb = floatArrayOf(0.68f, 0.72f, 0.75f),
                sunColorRgb = floatArrayOf(0.68f, 0.70f, 0.72f),
                ambientRgb = floatArrayOf(0.42f, 0.45f, 0.48f),
                fogDensity = condition.fogDensity,
                nightFactor = 0.15f
            )
            WeatherCondition.DUST -> WeatherLightingProfile(
                zenithRgb = floatArrayOf(0.58f, 0.48f, 0.34f),
                horizonRgb = floatArrayOf(0.80f, 0.66f, 0.46f),
                fogRgb = floatArrayOf(0.76f, 0.63f, 0.44f),
                sunColorRgb = floatArrayOf(0.95f, 0.80f, 0.55f),
                ambientRgb = floatArrayOf(0.45f, 0.38f, 0.28f),
                fogDensity = condition.fogDensity,
                nightFactor = 0.10f
            )
            WeatherCondition.SANDSTORM -> WeatherLightingProfile(
                zenithRgb = floatArrayOf(0.60f, 0.46f, 0.28f),
                horizonRgb = floatArrayOf(0.85f, 0.65f, 0.38f),
                fogRgb = floatArrayOf(0.80f, 0.62f, 0.38f),
                sunColorRgb = floatArrayOf(0.88f, 0.70f, 0.42f),
                ambientRgb = floatArrayOf(0.46f, 0.37f, 0.26f),
                fogDensity = condition.fogDensity,
                nightFactor = 0.22f
            )
        }
    }

    /**
     * Updates pooled weather particles around anchor position and returns true when
     * an ambient weather audio pulse should trigger.
     */
    fun step(dt: Float, anchorPos: Vec3, maxWeatherParticles: Int): Boolean {
        val targetCount = if (reduceEffects || condition.particleDensity <= 0.01f) {
            0
        } else {
            (maxWeatherParticles * condition.particleDensity).toInt().coerceIn(0, 72)
        }

        // Resize particle pool safely
        while (particles.size > targetCount) {
            particles.removeAt(particles.lastIndex)
        }
        while (particles.size < targetCount) {
            particles.add(spawnParticleNear(anchorPos))
        }

        val radius = 22f
        for (i in particles.indices) {
            val p = particles[i]
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.z += p.vz * dt

            val dx = p.x - anchorPos.x
            val dz = p.z - anchorPos.z
            if (p.y < 0.1f || p.y > 14f || dx * dx + dz * dz > radius * radius) {
                respawnParticle(p, anchorPos)
            }
        }

        if (condition.isPrecipitation || condition.isSandstorm || condition == WeatherCondition.DUST) {
            audioPulseTimer -= dt
            if (audioPulseTimer <= 0f) {
                audioPulseTimer = if (condition.isSandstorm) 3.4f else 4.2f
                return true
            }
        }
        return false
    }

    private fun spawnParticleNear(anchor: Vec3): WeatherParticle {
        val p = when {
            condition.isPrecipitation -> WeatherParticle(
                x = 0f, y = 0f, z = 0f,
                vx = -1.2f, vy = -16.5f, vz = 0.8f,
                sx = 0.025f, sy = 0.55f, sz = 0.025f,
                colorRgb = floatArrayOf(0.72f, 0.84f, 0.96f)
            )
            condition.isSandstorm -> WeatherParticle(
                x = 0f, y = 0f, z = 0f,
                vx = 11.5f, vy = -0.8f, vz = 4.2f,
                sx = 0.24f, sy = 0.07f, sz = 0.09f,
                colorRgb = floatArrayOf(0.86f, 0.72f, 0.46f)
            )
            else -> WeatherParticle(
                x = 0f, y = 0f, z = 0f,
                vx = 2.8f, vy = -0.35f, vz = 1.4f,
                sx = 0.14f, sy = 0.06f, sz = 0.14f,
                colorRgb = if (condition == WeatherCondition.FOG)
                    floatArrayOf(0.76f, 0.80f, 0.82f) else floatArrayOf(0.82f, 0.68f, 0.46f)
            )
        }
        respawnParticle(p, anchor, randomHeight = true)
        return p
    }

    private fun respawnParticle(p: WeatherParticle, anchor: Vec3, randomHeight: Boolean = false) {
        p.x = anchor.x + (rng.nextFloat() * 2f - 1f) * 19f
        p.z = anchor.z + (rng.nextFloat() * 2f - 1f) * 19f
        p.y = if (randomHeight) 0.8f + rng.nextFloat() * 11f
        else if (condition.isPrecipitation) 9.5f + rng.nextFloat() * 3.5f
        else 0.6f + rng.nextFloat() * 5.2f
    }
}
