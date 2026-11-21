package com.example.game.engine

import com.example.game.data.GraphicsQuality

/**
 * v2.1.0 PerformanceManager & Adaptive Budget Controller.
 * Monitors smoothed frame time (EMA), caps active particles/tracers/weather motes,
 * and dynamically trims non-essential LOD detail tiers if sustained frame times exceed budget.
 */
class PerformanceManager(initialQuality: GraphicsQuality = GraphicsQuality.MEDIUM) {
    var smoothedFrameMs: Float = 16.6f
        private set
    var peakFrameMs: Float = 16.6f
        private set
    var adaptiveLodOffset: Int = 0
        private set
    private var slowFrameStreak: Int = 0
    private var fastFrameStreak: Int = 0

    val maxActiveTracers: Int = 28
    var baseQuality: GraphicsQuality = initialQuality

    fun recordFrame(dtSeconds: Float, currentQuality: GraphicsQuality) {
        baseQuality = currentQuality
        val ms = (dtSeconds * 1000f).coerceIn(2f, 100f)
        smoothedFrameMs = smoothedFrameMs * 0.90f + ms * 0.10f
        if (ms > peakFrameMs) peakFrameMs = ms

        val budgetMs = 1000f / currentQuality.targetFps.coerceAtLeast(30)
        if (smoothedFrameMs > budgetMs * 1.35f) {
            slowFrameStreak++
            fastFrameStreak = 0
            if (slowFrameStreak >= 20 && adaptiveLodOffset < 1) {
                adaptiveLodOffset = 1
                slowFrameStreak = 0
            }
        } else if (smoothedFrameMs < budgetMs * 0.95f) {
            fastFrameStreak++
            slowFrameStreak = 0
            if (fastFrameStreak >= 45 && adaptiveLodOffset > 0) {
                adaptiveLodOffset = 0
                fastFrameStreak = 0
            }
        }
    }

    val effectiveDetailLevel: Int
        get() = (baseQuality.detailLevel - adaptiveLodOffset).coerceAtLeast(1)

    val effectiveMaxParticles: Int
        get() = if (adaptiveLodOffset > 0) (baseQuality.maxParticles * 0.65f).toInt().coerceAtLeast(16) else baseQuality.maxParticles

    val effectiveMaxWeatherParticles: Int
        get() = when (baseQuality) {
            GraphicsQuality.LOW -> 24
            GraphicsQuality.MEDIUM -> if (adaptiveLodOffset > 0) 28 else 46
            GraphicsQuality.HIGH -> if (adaptiveLodOffset > 0) 44 else 68
        }
}
