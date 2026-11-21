package com.example.game.render

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.example.game.data.CharacterAppearance
import com.example.game.data.EnemyAiState
import com.example.game.data.GameMode
import com.example.game.data.GraphicsQuality
import com.example.game.data.InteractiveKind
import com.example.game.data.MissionCatalog
import com.example.game.data.SurfaceMaterial
import com.example.game.engine.BlockShape
import com.example.game.engine.CombatSimulation
import com.example.game.engine.HudSnapshot
import com.example.game.engine.PlayerInputState
import com.example.game.engine.SimAudioEvent
import java.util.concurrent.atomic.AtomicReference
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Hardware-accelerated universal OpenGL ES 2.0 3D Battlefield Renderer.
 * Thread-safely steps simulation under sim.simLock and renders city geometry,
 * articulated characters, weapons, Tactical Armored Rover, Defense Command Core,
 * dynamic weather lighting & particles, tracers, and impacts.
 */
class CombatGLRenderer(
    private val simRef: AtomicReference<CombatSimulation?>,
    private val inputProvider: () -> PlayerInputState,
    private val qualityProvider: () -> GraphicsQuality,
    private val onHudUpdate: (HudSnapshot) -> Unit,
    private val onAudioEvents: (List<SimAudioEvent>) -> Unit
) : GLSurfaceView.Renderer {

    private var worldProgram = 0
    private var skyProgram = 0
    private var meshes: MeshFactory? = null

    // World shader uniform locations
    private var uMVP = 0
    private var uModel = 0
    private var uColor = 0
    private var uRough = 0
    private var uMetal = 0
    private var uEmissive = 0
    private var uPattern = 0
    private var uCamPos = 0
    private var uSunDir = 0
    private var uSunColor = 0
    private var uAmbient = 0
    private var uFogColor = 0
    private var uFogDensity = 0
    private var uNightFactor = 0

    private var uSkyZenith = 0
    private var uSkyHorizon = 0

    private val projMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val vpMatrix = FloatArray(16)

    private var viewportAspect = 16f / 9f
    private var currentFov = 64f
    private var lastFrameNanos = 0L
    private var hudPushAccum = 0f

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_CULL_FACE)
        GLES20.glCullFace(GLES20.GL_BACK)
        worldProgram = ShaderLibrary.createWorldProgram()
        skyProgram = ShaderLibrary.createSkyProgram()
        meshes = MeshFactory()

        if (worldProgram != 0) {
            uMVP = GLES20.glGetUniformLocation(worldProgram, "uMVP")
            uModel = GLES20.glGetUniformLocation(worldProgram, "uModel")
            uColor = GLES20.glGetUniformLocation(worldProgram, "uColor")
            uRough = GLES20.glGetUniformLocation(worldProgram, "uRoughness")
            uMetal = GLES20.glGetUniformLocation(worldProgram, "uMetallic")
            uEmissive = GLES20.glGetUniformLocation(worldProgram, "uEmissive")
            uPattern = GLES20.glGetUniformLocation(worldProgram, "uPatternType")
            uCamPos = GLES20.glGetUniformLocation(worldProgram, "uCamPos")
            uSunDir = GLES20.glGetUniformLocation(worldProgram, "uSunDir")
            uSunColor = GLES20.glGetUniformLocation(worldProgram, "uSunColor")
            uAmbient = GLES20.glGetUniformLocation(worldProgram, "uAmbientColor")
            uFogColor = GLES20.glGetUniformLocation(worldProgram, "uFogColor")
            uFogDensity = GLES20.glGetUniformLocation(worldProgram, "uFogDensity")
            uNightFactor = GLES20.glGetUniformLocation(worldProgram, "uNightFactor")
        }
        if (skyProgram != 0) {
            uSkyZenith = GLES20.glGetUniformLocation(skyProgram, "uZenith")
            uSkyHorizon = GLES20.glGetUniformLocation(skyProgram, "uHorizon")
        }
        lastFrameNanos = System.nanoTime()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        viewportAspect = if (height > 0) width.toFloat() / height.toFloat() else 1.77f
    }

    override fun onDrawFrame(gl: GL10?) {
        val now = System.nanoTime()
        val dt = ((now - lastFrameNanos) / 1_000_000_000.0f).coerceIn(0.004f, 0.05f)
        lastFrameNanos = now

        val sim = simRef.get()
        val m = meshes
        if (sim == null || m == null || worldProgram == 0) {
            GLES20.glClearColor(0.05f, 0.07f, 0.09f, 1f)
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
            return
        }

        val input = inputProvider()
        val quality = qualityProvider()
        var audioEvents: List<SimAudioEvent> = emptyList()
        var hudSnapToPush: HudSnapshot? = null

        synchronized(sim.simLock) {
            // Step simulation atomically with render read
            sim.step(dt, input)
            audioEvents = sim.drainAudioEvents()

            hudPushAccum += dt
            if (hudPushAccum >= 0.045f) {
                hudPushAccum = 0f
                hudSnapToPush = sim.buildHudSnapshot()
            }

            val cityDef = MissionCatalog.cityById(sim.mission.cityId)
            sim.performanceManager.recordFrame(dt, quality)
            val effectiveDetail = sim.performanceManager.effectiveDetailLevel
            val profile = sim.weatherSystem.lightingProfile(cityDef)

            val zenith = profile.zenithRgb
            val horizon = profile.horizonRgb
            val fogCol = profile.fogRgb
            val sunCol = profile.sunColorRgb
            val ambCol = profile.ambientRgb

            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

            // 1. Draw atmospheric sky dome
            if (skyProgram != 0) {
                GLES20.glDisable(GLES20.GL_DEPTH_TEST)
                GLES20.glDisableVertexAttribArray(1)
                GLES20.glUseProgram(skyProgram)
                GLES20.glUniform3f(uSkyZenith, zenith[0], zenith[1], zenith[2])
                GLES20.glUniform3f(uSkyHorizon, horizon[0], horizon[1], horizon[2])
                GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, m.skyQuad.vboId)
                GLES20.glEnableVertexAttribArray(0)
                GLES20.glVertexAttribPointer(0, 2, GLES20.GL_FLOAT, false, 8, 0)
                GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6)
                GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
                GLES20.glEnable(GLES20.GL_DEPTH_TEST)
            }

            // 2. Unified CameraDirector Projection & View (with vehicle driving & shoulder framing)
            val camPose = sim.cameraDirector.computePose(sim.player, sim.city, sim.reduceShake, inVehicle = sim.vehicle.isOccupied)
            currentFov = camPose.fov
            Matrix.perspectiveM(projMatrix, 0, currentFov, viewportAspect, 0.18f, quality.drawDistance)

            val camPos = camPose.position
            val lookTarget = camPose.lookTarget
            Matrix.setLookAtM(
                viewMatrix, 0,
                camPos.x, camPos.y, camPos.z,
                lookTarget.x, lookTarget.y, lookTarget.z,
                0f, 1f, 0f
            )
            Matrix.multiplyMM(vpMatrix, 0, projMatrix, 0, viewMatrix, 0)

            // 3. Configure World Shader Uniforms (driven by WeatherSystem)
            GLES20.glUseProgram(worldProgram)
            GLES20.glUniform3f(uCamPos, camPos.x, camPos.y, camPos.z)
            GLES20.glUniform3f(uSunDir, 0.45f, 0.78f, 0.42f)
            GLES20.glUniform3f(uSunColor, sunCol[0], sunCol[1], sunCol[2])
            GLES20.glUniform3f(uAmbient, ambCol[0], ambCol[1], ambCol[2])
            GLES20.glUniform3f(uFogColor, fogCol[0], fogCol[1], fogCol[2])
            GLES20.glUniform1f(uFogDensity, profile.fogDensity)
            GLES20.glUniform1f(uNightFactor, profile.nightFactor)

            // 4. Draw Ground Plane
            val groundPattern = when (sim.city.groundMaterial) {
                SurfaceMaterial.SAND -> 3
                SurfaceMaterial.ROCK -> 5
                else -> 2
            }
            m.drawPart(
                m.cube, vpMatrix, null,
                0f, -0.25f, 0f, 160f, 0.5f, 160f,
                uMVP = uMVP, uModel = uModel, uColor = uColor, uRough = uRough, uMetal = uMetal, uEmissive = uEmissive, uPattern = uPattern,
                rgb = sim.city.groundColorRgb, rough = 0.88f, metal = 0.04f, pattern = groundPattern
            )

            // 5. Draw City Architecture & Props (Adaptive LOD + Distance Culled)
            val maxDistSq = quality.drawDistance * quality.drawDistance
            for (i in sim.city.blocks.indices) {
                val b = sim.city.blocks[i]
                if (!b.isActive) continue
                if (b.detailTier > effectiveDetail) continue
                val dx = b.bounds.centerX - camPos.x
                val dz = b.bounds.centerZ - camPos.z
                if (dx * dx + dz * dz > maxDistSq) continue
                val mesh = if (b.shape == BlockShape.CYLINDER) m.cylinder else m.cube
                m.drawPart(
                    mesh, vpMatrix, null,
                    b.bounds.centerX, b.bounds.centerY, b.bounds.centerZ,
                    b.bounds.sizeX, b.bounds.sizeY, b.bounds.sizeZ,
                    uMVP = uMVP, uModel = uModel, uColor = uColor, uRough = uRough, uMetal = uMetal, uEmissive = uEmissive, uPattern = uPattern,
                    rgb = b.colorRgb, rough = b.roughness, metal = b.metallic, emissive = b.emissive, pattern = b.patternType
                )
            }

            // 6. Draw Resupply Stations (pulsing tactical emerald crates)
            val pulse = 0.55f + 0.35f * sin(sim.elapsedSeconds * 4f)
            for (st in sim.city.resupplyStations) {
                if (st.used) continue
                m.drawPart(
                    m.cube, vpMatrix, null,
                    st.position.x, 0.38f, st.position.z, 1.1f, 0.72f, 0.85f,
                    uMVP = uMVP, uModel = uModel, uColor = uColor, uRough = uRough, uMetal = uMetal, uEmissive = uEmissive, uPattern = uPattern,
                    rgb = floatArrayOf(0.20f, 0.45f, 0.28f), rough = 0.55f, metal = 0.35f
                )
                m.drawPart(
                    m.cube, vpMatrix, null,
                    st.position.x, 0.78f, st.position.z, 0.45f, 0.10f, 0.45f,
                    uMVP = uMVP, uModel = uModel, uColor = uColor, uRough = uRough, uMetal = uMetal, uEmissive = uEmissive, uPattern = uPattern,
                    rgb = floatArrayOf(0.25f, 0.92f, 0.52f), emissive = pulse
                )
            }

            // 6b. Draw Interactive World Objects
            for (obj in sim.interactionManager.objects) {
                when (obj.kind) {
                    InteractiveKind.DESTRUCTIBLE_BARREL -> if (!obj.isDestroyed) {
                        m.drawPart(
                            m.cylinder, vpMatrix, null,
                            obj.position.x, 0.62f, obj.position.z, 0.78f, 1.24f, 0.78f,
                            uMVP = uMVP, uModel = uModel, uColor = uColor, uRough = uRough, uMetal = uMetal, uEmissive = uEmissive, uPattern = uPattern,
                            rgb = floatArrayOf(0.82f, 0.18f, 0.14f), rough = 0.42f, metal = 0.55f
                        )
                        m.drawPart(
                            m.cylinder, vpMatrix, null,
                            obj.position.x, 0.64f, obj.position.z, 0.81f, 0.22f, 0.81f,
                            uMVP = uMVP, uModel = uModel, uColor = uColor, uRough = uRough, uMetal = uMetal, uEmissive = uEmissive, uPattern = uPattern,
                            rgb = floatArrayOf(0.95f, 0.74f, 0.16f), emissive = 0.35f
                        )
                    }
                    InteractiveKind.SWITCH -> {
                        m.drawPart(
                            m.cube, vpMatrix, null,
                            obj.position.x, 0.75f, obj.position.z, 0.45f, 1.5f, 0.45f,
                            uMVP = uMVP, uModel = uModel, uColor = uColor, uRough = uRough, uMetal = uMetal, uEmissive = uEmissive, uPattern = uPattern,
                            rgb = floatArrayOf(0.24f, 0.27f, 0.31f), metal = 0.6f
                        )
                        m.drawPart(
                            m.cube, vpMatrix, null,
                            obj.position.x, 1.38f, obj.position.z, 0.26f, 0.20f, 0.48f,
                            uMVP = uMVP, uModel = uModel, uColor = uColor, uRough = uRough, uMetal = uMetal, uEmissive = uEmissive, uPattern = uPattern,
                            rgb = if (obj.isActivated) floatArrayOf(0.22f, 0.92f, 0.50f) else floatArrayOf(0.95f, 0.68f, 0.16f),
                            emissive = 0.85f
                        )
                    }
                    InteractiveKind.AMMO_CONTAINER, InteractiveKind.SUPPLY_BOX -> if (!obj.isActivated) {
                        m.drawPart(
                            m.cube, vpMatrix, null,
                            obj.position.x, 0.36f, obj.position.z, 1.05f, 0.70f, 0.80f,
                            uMVP = uMVP, uModel = uModel, uColor = uColor, uRough = uRough, uMetal = uMetal, uEmissive = uEmissive, uPattern = uPattern,
                            rgb = floatArrayOf(0.22f, 0.42f, 0.56f), rough = 0.50f, metal = 0.40f
                        )
                        m.drawPart(
                            m.cube, vpMatrix, null,
                            obj.position.x, 0.74f, obj.position.z, 0.40f, 0.10f, 0.40f,
                            uMVP = uMVP, uModel = uModel, uColor = uColor, uRough = uRough, uMetal = uMetal, uEmissive = uEmissive, uPattern = uPattern,
                            rgb = floatArrayOf(0.32f, 0.82f, 0.96f), emissive = pulse
                        )
                    }
                    InteractiveKind.OBJECTIVE_DEVICE -> {
                        m.drawPart(
                            m.cube, vpMatrix, null,
                            obj.position.x, 0.65f, obj.position.z, 0.85f, 1.30f, 0.65f,
                            uMVP = uMVP, uModel = uModel, uColor = uColor, uRough = uRough, uMetal = uMetal, uEmissive = uEmissive, uPattern = uPattern,
                            rgb = floatArrayOf(0.18f, 0.22f, 0.28f), metal = 0.70f
                        )
                        m.drawPart(
                            m.cube, vpMatrix, null,
                            obj.position.x, 1.15f, obj.position.z, 0.62f, 0.36f, 0.68f,
                            uMVP = uMVP, uModel = uModel, uColor = uColor, uRough = uRough, uMetal = uMetal, uEmissive = uEmissive, uPattern = uPattern,
                            rgb = if (obj.isActivated) floatArrayOf(0.24f, 0.95f, 0.54f) else floatArrayOf(0.95f, 0.72f, 0.20f),
                            emissive = 0.9f
                        )
                    }
                    else -> Unit
                }
            }

            // 7. Draw Active Objective Tactical Beacon
            sim.currentObjective?.let { obj ->
                m.drawPart(
                    m.cylinder, vpMatrix, null,
                    obj.targetX, 0.06f, obj.targetZ, obj.radius * 1.15f, 0.08f, obj.radius * 1.15f,
                    uMVP = uMVP, uModel = uModel, uColor = uColor, uRough = uRough, uMetal = uMetal, uEmissive = uEmissive, uPattern = uPattern,
                    rgb = floatArrayOf(0.95f, 0.72f, 0.22f), emissive = 0.65f + pulse * 0.4f
                )
                m.drawPart(
                    m.cylinder, vpMatrix, null,
                    obj.targetX, 3.8f, obj.targetZ, 0.22f, 7.5f, 0.22f,
                    uMVP = uMVP, uModel = uModel, uColor = uColor, uRough = uRough, uMetal = uMetal, uEmissive = uEmissive, uPattern = uPattern,
                    rgb = floatArrayOf(0.95f, 0.74f, 0.24f), emissive = 0.95f
                )
            }

            // 7b. Draw Defense Mode 3D Command Relay Core
            if (sim.mode == GameMode.DEFENSE) {
                val core = sim.defenseCorePosition
                val hpFrac = (sim.defenseCoreHealth / sim.defenseCoreMaxHealth).coerceIn(0f, 1f)
                val coreRgb = if (hpFrac > 0.35f) floatArrayOf(0.22f, 0.85f, 0.98f) else floatArrayOf(0.95f, 0.24f, 0.20f)
                m.drawPart(
                    m.cube, vpMatrix, null,
                    core.x, 0.45f, core.z, 2.2f, 0.90f, 2.2f,
                    uMVP = uMVP, uModel = uModel, uColor = uColor, uRough = uRough, uMetal = uMetal, uEmissive = uEmissive, uPattern = uPattern,
                    rgb = floatArrayOf(0.24f, 0.28f, 0.34f), rough = 0.42f, metal = 0.72f
                )
                m.drawPart(
                    m.cylinder, vpMatrix, null,
                    core.x, 1.65f, core.z, 0.85f, 1.65f + hpFrac * 0.55f, 0.85f,
                    uMVP = uMVP, uModel = uModel, uColor = uColor, uRough = uRough, uMetal = uMetal, uEmissive = uEmissive, uPattern = uPattern,
                    rgb = coreRgb, emissive = 0.75f + pulse * 0.45f
                )
            }

            // 7c. Draw Playable 3D Tactical Armored Rover
            run {
                val v = sim.vehicle
                val vYawDeg = Math.toDegrees(v.yaw.toDouble()).toFloat()
                val tYawDeg = Math.toDegrees(v.turretYaw.toDouble()).toFloat()
                val hullCol = if (v.isDestroyed) floatArrayOf(0.18f, 0.16f, 0.16f) else floatArrayOf(0.27f, 0.34f, 0.26f)
                val cabinCol = if (v.isDestroyed) floatArrayOf(0.14f, 0.13f, 0.13f) else floatArrayOf(0.20f, 0.26f, 0.22f)
                m.drawPart(
                    m.cube, vpMatrix, null,
                    v.position.x, 0.72f, v.position.z, 2.25f, 0.85f, 4.10f,
                    rotYDeg = vYawDeg,
                    uMVP = uMVP, uModel = uModel, uColor = uColor, uRough = uRough, uMetal = uMetal, uEmissive = uEmissive, uPattern = uPattern,
                    rgb = hullCol, rough = 0.45f, metal = 0.58f
                )
                m.drawPart(
                    m.cube, vpMatrix, null,
                    v.position.x, 1.42f, v.position.z, 1.85f, 0.65f, 2.05f,
                    rotYDeg = vYawDeg,
                    uMVP = uMVP, uModel = uModel, uColor = uColor, uRough = uRough, uMetal = uMetal, uEmissive = uEmissive, uPattern = uPattern,
                    rgb = cabinCol, rough = 0.38f, metal = 0.62f
                )
                val cosV = cos(v.yaw)
                val sinV = sin(v.yaw)
                val wheelOffsets = listOf(-1.12f to -1.25f, 1.12f to -1.25f, -1.12f to 1.25f, 1.12f to 1.25f)
                for ((wx, wz) in wheelOffsets) {
                    val worldWx = v.position.x + wx * cosV + wz * sinV
                    val worldWz = v.position.z - wx * sinV + wz * cosV
                    m.drawPart(
                        m.cylinder, vpMatrix, null,
                        worldWx, 0.38f, worldWz, 0.42f, 0.74f, 0.42f,
                        rotYDeg = vYawDeg,
                        uMVP = uMVP, uModel = uModel, uColor = uColor, uRough = uRough, uMetal = uMetal, uEmissive = uEmissive, uPattern = uPattern,
                        rgb = floatArrayOf(0.11f, 0.11f, 0.12f), rough = 0.90f, metal = 0.15f
                    )
                }
                m.drawPart(
                    m.cylinder, vpMatrix, null,
                    v.position.x, 1.94f, v.position.z, 0.76f, 0.38f, 0.76f,
                    rotYDeg = tYawDeg,
                    uMVP = uMVP, uModel = uModel, uColor = uColor, uRough = uRough, uMetal = uMetal, uEmissive = uEmissive, uPattern = uPattern,
                    rgb = floatArrayOf(0.24f, 0.27f, 0.25f), rough = 0.35f, metal = 0.75f
                )
                val tFwdX = -sin(v.turretYaw)
                val tFwdZ = -cos(v.turretYaw)
                m.drawPart(
                    m.cube, vpMatrix, null,
                    v.position.x + tFwdX * 0.92f, 2.02f, v.position.z + tFwdZ * 0.92f, 0.14f, 0.14f, 1.45f,
                    rotYDeg = tYawDeg,
                    uMVP = uMVP, uModel = uModel, uColor = uColor, uRough = uRough, uMetal = uMetal, uEmissive = uEmissive, uPattern = uPattern,
                    rgb = floatArrayOf(0.16f, 0.18f, 0.20f), rough = 0.28f, metal = 0.85f
                )
                if (v.muzzleFlashTimer > 0f) {
                    m.drawPart(
                        m.cube, vpMatrix, null,
                        v.position.x + tFwdX * 1.72f, 2.02f, v.position.z + tFwdZ * 1.72f, 0.34f, 0.34f, 0.34f,
                        uMVP = uMVP, uModel = uModel, uColor = uColor, uRough = uRough, uMetal = uMetal, uEmissive = uEmissive, uPattern = uPattern,
                        rgb = floatArrayOf(1.0f, 0.82f, 0.30f), emissive = 1.8f
                    )
                }
                if (!v.isDestroyed && (profile.nightFactor > 0.3f || !v.isOccupied)) {
                    val hlFwdX = -sinV
                    val hlFwdZ = -cosV
                    m.drawPart(
                        m.cube, vpMatrix, null,
                        v.position.x + hlFwdX * 2.08f, 0.84f, v.position.z + hlFwdZ * 2.08f, 1.45f, 0.16f, 0.12f,
                        rotYDeg = vYawDeg,
                        uMVP = uMVP, uModel = uModel, uColor = uColor, uRough = uRough, uMetal = uMetal, uEmissive = uEmissive, uPattern = uPattern,
                        rgb = if (v.isOccupied) floatArrayOf(1.0f, 0.94f, 0.72f) else floatArrayOf(0.28f, 0.92f, 0.56f),
                        emissive = 1.15f
                    )
                }
            }

            // 8. Draw Articulated 3D Player Character + Ground Shadow
            val p = sim.player
            if (!sim.vehicle.isOccupied) {
                if (effectiveDetail >= 2) {
                    m.drawPart(
                        m.cylinder, vpMatrix, null,
                        p.position.x, 0.02f, p.position.z, 0.92f, 0.03f, 0.92f,
                        uMVP = uMVP, uModel = uModel, uColor = uColor, uRough = uRough, uMetal = uMetal, uEmissive = uEmissive, uPattern = uPattern,
                        rgb = floatArrayOf(0.05f, 0.06f, 0.07f), rough = 0.98f, metal = 0f
                    )
                }
                m.drawSoldier(
                    vpMatrix = vpMatrix,
                    x = p.position.x, y = p.position.y, z = p.position.z,
                    yawRad = p.bodyYaw,
                    scale = 1.0f,
                    animTime = p.animTime,
                    isMoving = p.velocity.lengthXZ() > 0.35f,
                    isCrouching = p.isCrouching || p.isInCover,
                    isAiming = p.isAiming,
                    isReloading = p.isReloading,
                    muzzleFlash = p.muzzleFlashTimer > 0f,
                    appearance = sim.appearance,
                    weaponStats = p.activeWeapon.stats,
                    enemyArchetype = null,
                    uMVP = uMVP, uModel = uModel, uColor = uColor, uRough = uRough, uMetal = uMetal, uEmissive = uEmissive, uPattern = uPattern
                )
            }

            // 9. Draw Articulated 3D Enemies
            val darkBoot = floatArrayOf(0.12f, 0.12f, 0.13f)
            for (i in sim.enemies.indices) {
                val e = sim.enemies[i]
                if (!e.isAlive) continue
                val dx = e.position.x - camPos.x
                val dz = e.position.z - camPos.z
                if (dx * dx + dz * dz > maxDistSq) continue

                if (effectiveDetail >= 2) {
                    m.drawPart(
                        m.cylinder, vpMatrix, null,
                        e.position.x, 0.02f, e.position.z, 0.95f, 0.03f, 0.95f,
                        uMVP = uMVP, uModel = uModel, uColor = uColor, uRough = uRough, uMetal = uMetal, uEmissive = uEmissive, uPattern = uPattern,
                        rgb = floatArrayOf(0.05f, 0.06f, 0.07f), rough = 0.98f, metal = 0f
                    )
                }
                val enemyScale = if (e.archetype.isBoss) 1.24f else if (e.archetype.name == "ARMORED_HEAVY") 1.12f else 1.0f
                val eApp = CharacterAppearance(
                    outfitRgb = e.archetype.primaryColorRgb,
                    helmetRgb = floatArrayOf(0.15f, 0.16f, 0.18f),
                    visorRgb = e.archetype.accentColorRgb,
                    vestRgb = floatArrayOf(0.18f, 0.19f, 0.21f),
                    glovesRgb = darkBoot,
                    bootsRgb = darkBoot,
                    backpackRgb = darkBoot,
                    vestBulk = if (e.archetype.isBoss) 1.28f else 1.05f,
                    hasBackpack = e.archetype.canCoordinateSquad
                )
                m.drawSoldier(
                    vpMatrix = vpMatrix,
                    x = e.position.x, y = e.position.y, z = e.position.z,
                    yawRad = e.yaw,
                    scale = enemyScale,
                    animTime = e.animTime,
                    isMoving = e.state in setOf(
                        EnemyAiState.PATROL, EnemyAiState.INVESTIGATE, EnemyAiState.FLANK,
                        EnemyAiState.REPOSITION, EnemyAiState.RETREAT, EnemyAiState.TAKE_COVER
                    ),
                    isCrouching = e.state == EnemyAiState.TAKE_COVER,
                    isAiming = e.state == EnemyAiState.ENGAGE || e.state == EnemyAiState.ALERT || e.state == EnemyAiState.SUSPICIOUS,
                    isReloading = e.state == EnemyAiState.RELOAD,
                    muzzleFlash = e.muzzleFlashTimer > 0f,
                    appearance = eApp,
                    weaponStats = null,
                    enemyArchetype = e.archetype,
                    uMVP = uMVP, uModel = uModel, uColor = uColor, uRough = uRough, uMetal = uMetal, uEmissive = uEmissive, uPattern = uPattern
                )
            }

            // 10. Draw Bullet Tracers
            for (i in sim.tracers.indices) {
                val tr = sim.tracers[i]
                val delta = tr.end - tr.start
                val len = delta.length().coerceAtMost(45f)
                if (len < 0.2f) continue
                val mid = tr.start + delta * 0.5f
                val yawDeg = Math.toDegrees(atan2(delta.x.toDouble(), delta.z.toDouble())).toFloat()
                val horiz = sqrt(delta.x * delta.x + delta.z * delta.z)
                val pitchDeg = -Math.toDegrees(atan2(delta.y.toDouble(), horiz.toDouble())).toFloat()
                m.drawPart(
                    m.cube, vpMatrix, null,
                    mid.x, mid.y, mid.z, 0.035f, 0.035f, len,
                    rotYDeg = yawDeg, rotXDeg = pitchDeg,
                    uMVP = uMVP, uModel = uModel, uColor = uColor, uRough = uRough, uMetal = uMetal, uEmissive = uEmissive, uPattern = uPattern,
                    rgb = tr.colorRgb, emissive = 1.6f
                )
            }

            // 11. Draw Impact Sparks & Dust Particles
            for (i in sim.particles.indices) {
                val pt = sim.particles[i]
                val s = pt.size * (pt.life / pt.maxLife).coerceIn(0.2f, 1f)
                m.drawPart(
                    m.cube, vpMatrix, null,
                    pt.position.x, pt.position.y, pt.position.z, s, s, s,
                    uMVP = uMVP, uModel = uModel, uColor = uColor, uRough = uRough, uMetal = uMetal, uEmissive = uEmissive, uPattern = uPattern,
                    rgb = pt.colorRgb, emissive = 0.65f
                )
            }

            // 12. Draw 3D Dynamic Weather Particles
            val wParticles = sim.weatherSystem.particles
            for (i in wParticles.indices) {
                val wp = wParticles[i]
                m.drawPart(
                    m.cube, vpMatrix, null,
                    wp.x, wp.y, wp.z, wp.sx, wp.sy, wp.sz,
                    uMVP = uMVP, uModel = uModel, uColor = uColor, uRough = uRough, uMetal = uMetal, uEmissive = uEmissive, uPattern = uPattern,
                    rgb = wp.colorRgb, emissive = 0.35f
                )
            }
        }

        // Dispatch callbacks outside simLock to prevent any lock inversion
        if (audioEvents.isNotEmpty()) onAudioEvents(audioEvents)
        hudSnapToPush?.let(onHudUpdate)
    }
}
