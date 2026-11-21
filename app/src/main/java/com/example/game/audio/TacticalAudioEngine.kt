package com.example.game.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.example.game.data.SurfaceMaterial
import com.example.game.data.WeaponId
import com.example.game.engine.SimAudioEvent
import com.example.game.save.GameSettings
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * 100% Offline Procedural Military Audio Engine.
 * Uses thread-safe ConcurrentHashMap caching, a bounded polyphonic voice pool, and
 * per-key debouncing so intense firefights never queue-lag or exhaust AudioTracks.
 */
class TacticalAudioEngine {
    private val sampleRate = 22050
    private val maxConcurrentVoices = 4
    private val executor = Executors.newFixedThreadPool(maxConcurrentVoices)
    private val cache = ConcurrentHashMap<String, ShortArray>()
    private val lastPlayedMs = ConcurrentHashMap<String, Long>()
    private val activeVoices = AtomicInteger(0)

    @Volatile
    private var settings: GameSettings = GameSettings()
    @Volatile
    private var released = false
    @Volatile
    private var musicPaused = false
    @Volatile
    private var combatMusicActive = false

    private var musicThread: Thread? = null

    init {
        executor.execute { prebuildSounds() }
        startMusicLoop()
    }

    fun updateSettings(newSettings: GameSettings) {
        settings = newSettings
    }

    fun setCombatMusicMode(inCombat: Boolean) {
        combatMusicActive = inCombat
    }

    fun setPaused(paused: Boolean) {
        musicPaused = paused
    }

    fun playUiClick() {
        playCached("ui_click", volumeScale = 0.45f, minIntervalMs = 45L)
    }

    fun handleEvent(event: SimAudioEvent) {
        when (event) {
            is SimAudioEvent.WeaponFired -> {
                val key = if (event.suppressed) "shot_suppressed" else "shot_${event.weaponId.id}"
                playCached(key, volumeScale = if (event.suppressed) 0.62f else 0.95f, minIntervalMs = 55L)
            }
            is SimAudioEvent.Reload -> playCached("reload", volumeScale = 0.72f, minIntervalMs = 200L)
            is SimAudioEvent.Footstep -> {
                val key = if (event.material == SurfaceMaterial.SAND) "step_sand" else "step_hard"
                playCached(key, volumeScale = 0.40f, minIntervalMs = 180L)
            }
            is SimAudioEvent.HitConfirm -> playCached(if (event.headshot) "hit_head" else "hit_body", volumeScale = 0.75f, minIntervalMs = 45L)
            is SimAudioEvent.Impact -> playCached("impact", volumeScale = 0.50f, minIntervalMs = 70L)
            is SimAudioEvent.Explosion -> playCached("explosion", volumeScale = 1.0f, minIntervalMs = 180L)
            is SimAudioEvent.GrenadeThrown -> playCached("grenade_throw", volumeScale = 0.65f, minIntervalMs = 200L)
            is SimAudioEvent.MedkitUsed -> playCached("medkit_use", volumeScale = 0.78f, minIntervalMs = 250L)
            is SimAudioEvent.RoverEnginePulse -> playCached("rover_engine", volumeScale = 0.42f, minIntervalMs = 280L)
            is SimAudioEvent.PlayerDamaged -> playCached("player_hit", volumeScale = 0.85f, minIntervalMs = 120L)
            is SimAudioEvent.Checkpoint -> playCached("checkpoint", volumeScale = 0.70f, minIntervalMs = 250L)
            is SimAudioEvent.ObjectiveComplete -> playCached("checkpoint", volumeScale = 0.80f, minIntervalMs = 250L)
            is SimAudioEvent.ReinforcementsAlert -> playCached("alert", volumeScale = 0.85f, minIntervalMs = 400L)
            is SimAudioEvent.MissionVictory -> playCached("victory", volumeScale = 0.90f, minIntervalMs = 500L)
            is SimAudioEvent.MissionDefeat -> playCached("defeat", volumeScale = 0.85f, minIntervalMs = 500L)
            is SimAudioEvent.VehicleTurretFired -> playCached("turret_shot", volumeScale = 0.92f, minIntervalMs = 95L)
            is SimAudioEvent.WeatherPulse -> playCached(
                if (event.isRain) "weather_rain" else "weather_wind",
                volumeScale = settings.ambientVolume * 0.45f,
                minIntervalMs = 1500L
            )
            is SimAudioEvent.CoreDamaged -> playCached("core_alert", volumeScale = 0.80f, minIntervalMs = 400L)
        }
    }

    private fun prebuildSounds() {
        cache["shot_${WeaponId.VX74_ASSAULT.id}"] = synthGunshot(0.16f, 155f, 0.85f, 18f)
        cache["shot_${WeaponId.KSR9_SMG.id}"] = synthGunshot(0.10f, 240f, 0.72f, 26f)
        cache["shot_${WeaponId.SR308_PRECISION.id}"] = synthGunshot(0.32f, 92f, 1.0f, 10f)
        cache["shot_${WeaponId.DMR14_MARKSMAN.id}"] = synthGunshot(0.22f, 125f, 0.92f, 13f)
        cache["shot_${WeaponId.BR12_SHOTGUN.id}"] = synthGunshot(0.28f, 82f, 1.0f, 11f)
        cache["shot_${WeaponId.MG48_LMG.id}"] = synthGunshot(0.18f, 130f, 0.90f, 15f)
        cache["shot_${WeaponId.M9S_SIDEARM.id}"] = synthGunshot(0.12f, 270f, 0.68f, 24f)
        cache["shot_${WeaponId.RAIL_X1_HEAVY.id}"] = synthPulseShot(0.26f)
        cache["shot_suppressed"] = synthSuppressedShot()

        cache["explosion"] = synthExplosion()
        cache["grenade_throw"] = synthTone(0.16f, 480f, 180f, 0.50f)
        cache["medkit_use"] = synthChord(0.36f, floatArrayOf(587.33f, 739.99f, 880.0f))
        cache["rover_engine"] = synthRoverEngine()
        cache["reload"] = synthReload()
        cache["step_hard"] = synthFootstep(isSand = false)
        cache["step_sand"] = synthFootstep(isSand = true)
        cache["hit_body"] = synthTone(0.06f, 1350f, 900f, 0.55f)
        cache["hit_head"] = synthTone(0.11f, 2100f, 2650f, 0.75f)
        cache["impact"] = synthGunshot(0.08f, 420f, 0.45f, 34f)
        cache["player_hit"] = synthGunshot(0.18f, 75f, 0.88f, 14f)
        cache["checkpoint"] = synthChord(0.35f, floatArrayOf(523.25f, 659.25f, 783.99f))
        cache["alert"] = synthTone(0.38f, 640f, 440f, 0.72f)
        cache["victory"] = synthChord(0.65f, floatArrayOf(440f, 554.37f, 659.25f, 880f))
        cache["defeat"] = synthTone(0.55f, 260f, 130f, 0.72f)
        cache["ui_click"] = synthTone(0.04f, 920f, 640f, 0.45f)
        cache["turret_shot"] = synthGunshot(0.19f, 95f, 0.96f, 13f)
        cache["core_alert"] = synthTone(0.28f, 880f, 520f, 0.75f)
        cache["weather_rain"] = synthWeatherNoise(0.55f, isRain = true)
        cache["weather_wind"] = synthWeatherNoise(0.60f, isRain = false)
    }

    private fun playCached(key: String, volumeScale: Float, minIntervalMs: Long) {
        if (released) return
        val s = settings
        val gain = (s.masterVolume * s.sfxVolume * volumeScale).coerceIn(0f, 1f)
        if (gain < 0.02f) return
        val pcm = cache[key] ?: return

        val now = System.currentTimeMillis()
        val prev = lastPlayedMs[key] ?: 0L
        if (now - prev < minIntervalMs) return
        if (activeVoices.get() >= maxConcurrentVoices) return
        lastPlayedMs[key] = now

        if (activeVoices.incrementAndGet() > maxConcurrentVoices) {
            activeVoices.decrementAndGet()
            return
        }

        try {
            executor.execute {
                var track: AudioTrack? = null
                try {
                    if (released) return@execute
                    track = AudioTrack.Builder()
                        .setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_GAME)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build()
                        )
                        .setAudioFormat(
                            AudioFormat.Builder()
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .setSampleRate(sampleRate)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                .build()
                        )
                        .setBufferSizeInBytes(pcm.size * 2)
                        .setTransferMode(AudioTrack.MODE_STATIC)
                        .build()
                    if (track.state == AudioTrack.STATE_INITIALIZED) {
                        track.write(pcm, 0, pcm.size)
                        track.setVolume(gain)
                        track.play()
                        val durationMs = ((pcm.size * 1000L) / sampleRate).coerceAtLeast(35L)
                        Thread.sleep(durationMs + 20L)
                        if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                            track.stop()
                        }
                    }
                } catch (_: Throwable) {
                    // Safe no-op in headless/JVM test environments or interrupted shutdown
                } finally {
                    runCatching { track?.release() }
                    activeVoices.decrementAndGet()
                }
            }
        } catch (_: Throwable) {
            activeVoices.decrementAndGet()
        }
    }

    private fun synthGunshot(duration: Float, punchFreq: Float, amp: Float, decay: Float): ShortArray {
        val n = (sampleRate * duration).toInt()
        val out = ShortArray(n)
        val rng = Random(punchFreq.toInt())
        for (i in 0 until n) {
            val t = i.toFloat() / sampleRate
            val env = exp(-t * decay)
            val noise = (rng.nextFloat() * 2f - 1f) * env
            val punch = sin(2.0 * PI * punchFreq * exp(-t * 14.0) * t).toFloat() * exp(-t * (decay * 1.4f))
            val sample = ((noise * 0.65f + punch * 0.65f) * amp).coerceIn(-1f, 1f)
            out[i] = (sample * 32000f).toInt().toShort()
        }
        return out
    }

    private fun synthPulseShot(duration: Float): ShortArray {
        val n = (sampleRate * duration).toInt()
        val out = ShortArray(n)
        for (i in 0 until n) {
            val t = i.toFloat() / sampleRate
            val env = exp(-t * 11f)
            val freq = 980f * exp(-t * 9f) + 120f
            val wave = sin(2.0 * PI * freq * t).toFloat() * 0.7f + sin(4.0 * PI * freq * t).toFloat() * 0.3f
            out[i] = ((wave * env).coerceIn(-1f, 1f) * 30000f).toInt().toShort()
        }
        return out
    }

    private fun synthReload(): ShortArray {
        val n = (sampleRate * 0.28f).toInt()
        val out = ShortArray(n)
        val rng = Random(42)
        for (i in 0 until n) {
            val t = i.toFloat() / sampleRate
            val c1 = if (t < 0.08f) exp(-t * 42f) else 0f
            val c2 = if (t > 0.14f) exp(-(t - 0.14f) * 35f) else 0f
            val metal = sin(2.0 * PI * 1450.0 * t).toFloat() * 0.4f + (rng.nextFloat() * 2f - 1f) * 0.5f
            out[i] = ((metal * (c1 + c2)).coerceIn(-1f, 1f) * 26000f).toInt().toShort()
        }
        return out
    }

    private fun synthFootstep(isSand: Boolean): ShortArray {
        val n = (sampleRate * 0.09f).toInt()
        val out = ShortArray(n)
        val rng = Random(if (isSand) 7 else 13)
        for (i in 0 until n) {
            val t = i.toFloat() / sampleRate
            val env = exp(-t * (if (isSand) 24f else 45f))
            val thud = sin(2.0 * PI * (if (isSand) 95.0 else 135.0) * t).toFloat()
            val grit = (rng.nextFloat() * 2f - 1f) * (if (isSand) 0.75f else 0.35f)
            out[i] = (((thud * 0.5f + grit) * env).coerceIn(-1f, 1f) * 18000f).toInt().toShort()
        }
        return out
    }

    private fun synthTone(duration: Float, fStart: Float, fEnd: Float, amp: Float): ShortArray {
        val n = (sampleRate * duration).toInt()
        val out = ShortArray(n)
        for (i in 0 until n) {
            val p = i.toFloat() / n
            val t = i.toFloat() / sampleRate
            val freq = fStart + (fEnd - fStart) * p
            val env = (1f - p) * amp
            out[i] = ((sin(2.0 * PI * freq * t).toFloat() * env).coerceIn(-1f, 1f) * 28000f).toInt().toShort()
        }
        return out
    }

    private fun synthChord(duration: Float, freqs: FloatArray): ShortArray {
        val n = (sampleRate * duration).toInt()
        val out = ShortArray(n)
        for (i in 0 until n) {
            val t = i.toFloat() / sampleRate
            val env = exp(-t * 3.8f)
            var sum = 0f
            for (f in freqs) sum += sin(2.0 * PI * f * t).toFloat()
            val s = ((sum / freqs.size) * env * 0.85f).coerceIn(-1f, 1f)
            out[i] = (s * 28000f).toInt().toShort()
        }
        return out
    }

    private fun synthWeatherNoise(duration: Float, isRain: Boolean): ShortArray {
        val n = (sampleRate * duration).toInt()
        val out = ShortArray(n)
        val rng = Random(if (isRain) 303 else 505)
        var lp = 0f
        for (i in 0 until n) {
            val p = i.toFloat() / n
            val env = sin(PI * p).toFloat()
            val white = rng.nextFloat() * 2f - 1f
            lp = lp * (if (isRain) 0.55f else 0.88f) + white * (if (isRain) 0.45f else 0.12f)
            val s = (lp * env * 0.42f).coerceIn(-1f, 1f)
            out[i] = (s * 24000f).toInt().toShort()
        }
        return out
    }

    private fun synthSuppressedShot(): ShortArray {
        val n = (sampleRate * 0.11f).toInt()
        val out = ShortArray(n)
        val rng = Random(88)
        var lp = 0f
        for (i in 0 until n) {
            val t = i.toFloat() / sampleRate
            val env = exp(-t * 36f)
            val white = rng.nextFloat() * 2f - 1f
            lp = lp * 0.72f + white * 0.28f
            val click = sin(2.0 * PI * 310.0 * exp(-t * 24.0) * t).toFloat() * exp(-t * 48f)
            val s = ((lp * 0.55f + click * 0.45f) * env * 0.75f).coerceIn(-1f, 1f)
            out[i] = (s * 28000f).toInt().toShort()
        }
        return out
    }

    private fun synthExplosion(): ShortArray {
        val n = (sampleRate * 0.65f).toInt()
        val out = ShortArray(n)
        val rng = Random(911)
        var lp = 0f
        for (i in 0 until n) {
            val t = i.toFloat() / sampleRate
            val env = exp(-t * 4.4f)
            val white = rng.nextFloat() * 2f - 1f
            lp = lp * 0.84f + white * 0.16f
            val subBoom = sin(2.0 * PI * (68.0 * exp(-t * 3.5) + 28.0) * t).toFloat() * exp(-t * 5.2f)
            val crackle = white * exp(-t * 14f) * 0.35f
            val s = ((subBoom * 0.75f + lp * 0.65f + crackle) * env).coerceIn(-1f, 1f)
            out[i] = (s * 32000f).toInt().toShort()
        }
        return out
    }

    private fun synthRoverEngine(): ShortArray {
        val n = (sampleRate * 0.28f).toInt()
        val out = ShortArray(n)
        for (i in 0 until n) {
            val p = i.toFloat() / n
            val t = i.toFloat() / sampleRate
            val env = sin(PI * p).toFloat()
            val hum = sin(2.0 * PI * 62.0 * t).toFloat() * 0.55f +
                sin(2.0 * PI * 124.0 * t).toFloat() * 0.28f +
                sin(2.0 * PI * 93.0 * t).toFloat() * 0.17f
            out[i] = ((hum * env * 0.55f).coerceIn(-1f, 1f) * 24000f).toInt().toShort()
        }
        return out
    }

    private fun synthMusicLoop(combat: Boolean): ShortArray {
        val seconds = 2.0f
        val n = (sampleRate * seconds).toInt()
        val out = ShortArray(n)
        val rng = Random(if (combat) 777 else 333)
        val beatDur = if (combat) 0.25f else 0.5f
        for (i in 0 until n) {
            val t = i.toFloat() / sampleRate
            val beatPos = t % beatDur
            // Dark D-minor military drone pad (D2 + A2 + F3)
            val pad = sin(2.0 * PI * 73.42 * t).toFloat() * 0.24f +
                sin(2.0 * PI * 110.0 * t).toFloat() * 0.16f +
                sin(2.0 * PI * 174.61 * t).toFloat() * (0.08f + 0.04f * sin(2.0 * PI * 0.5 * t).toFloat())
            var pulse = 0f
            if (combat) {
                val kick = sin(2.0 * PI * (95.0 * exp(-beatPos * 18.0) + 42.0) * beatPos).toFloat() * exp(-beatPos * 15f)
                val stepIdx = ((t / beatDur).toInt()) % 8
                val bassFreq = when (stepIdx) {
                    0, 1, 4 -> 73.42
                    2, 5 -> 82.41
                    3, 6 -> 87.31
                    else -> 65.41
                }
                val bass = sin(2.0 * PI * bassFreq * t).toFloat() * exp(-beatPos * 6f) * 0.26f
                val hat = if (stepIdx % 2 == 1) (rng.nextFloat() * 2f - 1f) * exp(-beatPos * 45f) * 0.09f else 0f
                pulse = kick * 0.34f + bass + hat
            } else {
                val heartbeat = sin(2.0 * PI * 55.0 * beatPos).toFloat() * exp(-beatPos * 11f) * 0.16f
                pulse = heartbeat
            }
            val s = ((pad + pulse) * 0.65f).coerceIn(-1f, 1f)
            out[i] = (s * 24000f).toInt().toShort()
        }
        return out
    }

    private fun startMusicLoop() {
        val thread = Thread({
            var track: AudioTrack? = null
            try {
                val menuLoop = synthMusicLoop(combat = false)
                val combatLoop = synthMusicLoop(combat = true)
                val minBuf = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                ).coerceAtLeast(4096)

                track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(minBuf * 2)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                if (track.state != AudioTrack.STATE_INITIALIZED) return@Thread
                var playing = false
                val chunkSamples = 2205 // 100ms chunks for responsive pause/volume changes
                var cursor = 0

                while (!released) {
                    val s = settings
                    val vol = (s.masterVolume * s.musicVolume * 0.42f).coerceIn(0f, 1f)
                    if (musicPaused || vol < 0.015f) {
                        if (playing) {
                            runCatching { track.pause(); track.flush() }
                            playing = false
                        }
                        Thread.sleep(160L)
                        continue
                    }
                    if (!playing) {
                        track.play()
                        playing = true
                    }
                    track.setVolume(vol)
                    val buf = if (combatMusicActive) combatLoop else menuLoop
                    val len = minOf(chunkSamples, buf.size - cursor)
                    track.write(buf, cursor, len)
                    cursor = (cursor + len) % buf.size
                }
            } catch (_: Throwable) {
                // Safe exit in headless JVM/Robolectric tests or interrupted release
            } finally {
                runCatching { track?.stop() }
                runCatching { track?.release() }
            }
        }, "CombatFronts-Music")
        thread.isDaemon = true
        musicThread = thread
        thread.start()
    }

    fun release() {
        released = true
        musicThread?.interrupt()
        executor.shutdownNow()
    }
}
