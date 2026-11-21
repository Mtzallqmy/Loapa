package com.example.game.render

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.example.game.data.CharacterAppearance
import com.example.game.data.EffectiveWeaponStats
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.sin

enum class StudioPreviewMode {
    WEAPON,
    CHARACTER
}

data class StudioPreviewState(
    val mode: StudioPreviewMode,
    val weaponStats: EffectiveWeaponStats,
    val appearance: CharacterAppearance,
    val rotationYawDeg: Float = 35f,
    val zoom: Float = 1.0f // 0.65..1.55
)

/**
 * Interactive 360-degree rotatable & zoomable OpenGL ES 2.0 3D Preview Renderer
 * for both the Weapon Armory and Character Customization Studio.
 */
class PreviewGLRenderer(
    private val stateProvider: () -> StudioPreviewState
) : GLSurfaceView.Renderer {

    private var worldProgram = 0
    private var skyProgram = 0
    private var meshes: MeshFactory? = null

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
    private val weaponMat = FloatArray(16)
    private var aspect = 1.5f
    private var idleTime = 0f

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
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        aspect = if (height > 0) width.toFloat() / height.toFloat() else 1.5f
    }

    override fun onDrawFrame(gl: GL10?) {
        idleTime += 0.016f
        val st = stateProvider()
        val m = meshes
        if (m == null || worldProgram == 0) {
            GLES20.glClearColor(0.08f, 0.11f, 0.15f, 1f)
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
            return
        }

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        // Studio backdrop gradient
        if (skyProgram != 0) {
            GLES20.glDisable(GLES20.GL_DEPTH_TEST)
            GLES20.glDisableVertexAttribArray(1)
            GLES20.glUseProgram(skyProgram)
            GLES20.glUniform3f(uSkyZenith, 0.06f, 0.09f, 0.13f)
            GLES20.glUniform3f(uSkyHorizon, 0.16f, 0.21f, 0.26f)
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, m.skyQuad.vboId)
            GLES20.glEnableVertexAttribArray(0)
            GLES20.glVertexAttribPointer(0, 2, GLES20.GL_FLOAT, false, 8, 0)
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6)
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
            GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        }

        val baseDist = if (st.mode == StudioPreviewMode.WEAPON) 1.55f else 2.85f
        val dist = (baseDist / st.zoom.coerceIn(0.65f, 1.55f))
        val targetY = if (st.mode == StudioPreviewMode.WEAPON) 0.55f else 1.02f
        val camY = targetY + if (st.mode == StudioPreviewMode.WEAPON) 0.22f else 0.28f

        Matrix.perspectiveM(projMatrix, 0, 48f, aspect, 0.1f, 40f)
        Matrix.setLookAtM(viewMatrix, 0, 0f, camY, dist, 0f, targetY, 0f, 0f, 1f, 0f)
        Matrix.multiplyMM(vpMatrix, 0, projMatrix, 0, viewMatrix, 0)

        GLES20.glUseProgram(worldProgram)
        GLES20.glUniform3f(uCamPos, 0f, camY, dist)
        GLES20.glUniform3f(uSunDir, 0.55f, 0.72f, 0.60f)
        GLES20.glUniform3f(uSunColor, 1.0f, 0.92f, 0.80f)
        GLES20.glUniform3f(uAmbient, 0.36f, 0.42f, 0.48f)
        GLES20.glUniform3f(uFogColor, 0.10f, 0.14f, 0.18f)
        GLES20.glUniform1f(uFogDensity, 0.02f)
        GLES20.glUniform1f(uNightFactor, 0f)

        // Metallic circular tactical turntable pedestal
        m.drawPart(
            m.cylinder, vpMatrix, null,
            0f, -0.06f, 0f, 1.85f, 0.12f, 1.85f,
            uMVP = uMVP, uModel = uModel, uColor = uColor, uRough = uRough, uMetal = uMetal, uEmissive = uEmissive, uPattern = uPattern,
            rgb = floatArrayOf(0.16f, 0.20f, 0.25f), rough = 0.35f, metal = 0.72f
        )
        m.drawPart(
            m.cylinder, vpMatrix, null,
            0f, 0.01f, 0f, 1.72f, 0.02f, 1.72f,
            uMVP = uMVP, uModel = uModel, uColor = uColor, uRough = uRough, uMetal = uMetal, uEmissive = uEmissive, uPattern = uPattern,
            rgb = floatArrayOf(0.88f, 0.66f, 0.24f), emissive = 0.35f
        )

        if (st.mode == StudioPreviewMode.WEAPON) {
            Matrix.setIdentityM(weaponMat, 0)
            Matrix.translateM(weaponMat, 0, 0f, 0.58f + sin(idleTime * 1.8f) * 0.03f, 0f)
            Matrix.rotateM(weaponMat, 0, st.rotationYawDeg, 0f, 1f, 0f)
            Matrix.rotateM(weaponMat, 0, 90f, 0f, 1f, 0f)
            m.drawWeaponModel(
                vpMatrix, weaponMat, st.weaponStats, 1.45f, false,
                uMVP, uModel, uColor, uRough, uMetal, uEmissive, uPattern
            )
        } else {
            val yawRad = Math.toRadians(st.rotationYawDeg.toDouble()).toFloat()
            m.drawSoldier(
                vpMatrix = vpMatrix,
                x = 0f, y = 0.02f, z = 0f,
                yawRad = yawRad,
                scale = 1.02f,
                animTime = idleTime,
                isMoving = false,
                isCrouching = false,
                isAiming = false,
                isReloading = false,
                muzzleFlash = false,
                appearance = st.appearance,
                weaponStats = st.weaponStats,
                enemyArchetype = null,
                uMVP = uMVP, uModel = uModel, uColor = uColor, uRough = uRough, uMetal = uMetal, uEmissive = uEmissive, uPattern = uPattern
            )
        }
    }
}
