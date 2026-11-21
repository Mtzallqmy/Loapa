package com.example.game.render

import android.opengl.GLES20
import android.opengl.Matrix
import com.example.game.data.CharacterAppearance
import com.example.game.data.EffectiveWeaponStats
import com.example.game.data.EnemyArchetype
import com.example.game.data.PlayerAnimState
import com.example.game.data.WeaponId
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class GlMesh(
    val vboId: Int,
    val vertexCount: Int
)

class MeshFactory {
    val cube: GlMesh
    val cylinder: GlMesh
    val sphere: GlMesh
    val skyQuad: GlMesh

    private val modelMat = FloatArray(16)
    private val mvpMat = FloatArray(16)

    init {
        cube = uploadInterleaved(buildCubeVertices())
        cylinder = uploadInterleaved(buildCylinderVertices(14))
        sphere = uploadInterleaved(buildSphereVertices(10, 12))
        skyQuad = uploadQuad()
    }

    private fun uploadInterleaved(data: FloatArray): GlMesh {
        val buf: FloatBuffer = ByteBuffer.allocateDirect(data.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer()
        buf.put(data).position(0)
        val ids = IntArray(1)
        GLES20.glGenBuffers(1, ids, 0)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, ids[0])
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, data.size * 4, buf, GLES20.GL_STATIC_DRAW)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        return GlMesh(ids[0], data.size / 6)
    }

    private fun uploadQuad(): GlMesh {
        val data = floatArrayOf(
            -1f, -1f, 1f, -1f, -1f, 1f,
            -1f, 1f, 1f, -1f, 1f, 1f
        )
        val buf = ByteBuffer.allocateDirect(data.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        buf.put(data).position(0)
        val ids = IntArray(1)
        GLES20.glGenBuffers(1, ids, 0)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, ids[0])
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, data.size * 4, buf, GLES20.GL_STATIC_DRAW)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        return GlMesh(ids[0], 6)
    }

    fun bindMesh(mesh: GlMesh) {
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, mesh.vboId)
        GLES20.glEnableVertexAttribArray(0)
        GLES20.glVertexAttribPointer(0, 3, GLES20.GL_FLOAT, false, 24, 0)
        GLES20.glEnableVertexAttribArray(1)
        GLES20.glVertexAttribPointer(1, 3, GLES20.GL_FLOAT, false, 24, 12)
    }

    fun drawPart(
        mesh: GlMesh,
        vpMatrix: FloatArray,
        parentMat: FloatArray?,
        tx: Float, ty: Float, tz: Float,
        sx: Float, sy: Float, sz: Float,
        rotYDeg: Float = 0f,
        rotXDeg: Float = 0f,
        rotZDeg: Float = 0f,
        uMVP: Int,
        uModel: Int,
        uColor: Int,
        uRough: Int,
        uMetal: Int,
        uEmissive: Int,
        uPattern: Int,
        rgb: FloatArray,
        rough: Float = 0.72f,
        metal: Float = 0.12f,
        emissive: Float = 0f,
        pattern: Int = 0
    ) {
        if (parentMat != null) {
            System.arraycopy(parentMat, 0, modelMat, 0, 16)
        } else {
            Matrix.setIdentityM(modelMat, 0)
        }
        Matrix.translateM(modelMat, 0, tx, ty, tz)
        if (rotYDeg != 0f) Matrix.rotateM(modelMat, 0, rotYDeg, 0f, 1f, 0f)
        if (rotXDeg != 0f) Matrix.rotateM(modelMat, 0, rotXDeg, 1f, 0f, 0f)
        if (rotZDeg != 0f) Matrix.rotateM(modelMat, 0, rotZDeg, 0f, 0f, 1f)
        Matrix.scaleM(modelMat, 0, sx, sy, sz)
        Matrix.multiplyMM(mvpMat, 0, vpMatrix, 0, modelMat, 0)

        GLES20.glUniformMatrix4fv(uMVP, 1, false, mvpMat, 0)
        GLES20.glUniformMatrix4fv(uModel, 1, false, modelMat, 0)
        GLES20.glUniform3f(uColor, rgb[0], rgb[1], rgb[2])
        GLES20.glUniform1f(uRough, rough)
        GLES20.glUniform1f(uMetal, metal)
        GLES20.glUniform1f(uEmissive, emissive)
        GLES20.glUniform1i(uPattern, pattern)

        bindMesh(mesh)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mesh.vertexCount)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
    }

    /** Renders an articulated 3D tactical soldier (Player or Enemy) with animations & gear. */
    fun drawSoldier(
        vpMatrix: FloatArray,
        x: Float, y: Float, z: Float,
        yawRad: Float,
        scale: Float,
        animTime: Float,
        isMoving: Boolean,
        isCrouching: Boolean,
        isAiming: Boolean,
        isReloading: Boolean,
        muzzleFlash: Boolean,
        appearance: CharacterAppearance,
        weaponStats: EffectiveWeaponStats?,
        enemyArchetype: EnemyArchetype?,
        uMVP: Int, uModel: Int, uColor: Int, uRough: Int, uMetal: Int, uEmissive: Int, uPattern: Int
    ) {
        val root = FloatArray(16)
        Matrix.setIdentityM(root, 0)
        Matrix.translateM(root, 0, x, y, z)
        Matrix.rotateM(root, 0, Math.toDegrees(yawRad.toDouble()).toFloat(), 0f, 1f, 0f)
        if (scale != 1f) Matrix.scaleM(root, 0, scale, scale, scale)

        val crouchDrop = if (isCrouching) 0.34f else 0f
        val legSwing = if (isMoving) sin(animTime * 9.5f) * 28f else 0f
        val armBob = if (isReloading) sin(animTime * 10f) * 22f else 0f
        val skinRgb = floatArrayOf(0.72f, 0.56f, 0.44f)

        // Boots & Legs
        drawPart(cube, vpMatrix, root, -0.15f, 0.36f - crouchDrop * 0.4f, 0f, 0.18f, 0.72f - crouchDrop * 0.4f, 0.20f, rotXDeg = legSwing, uMVP = uMVP, uModel = uModel, uColor = uColor, uRough = uRough, uMetal = uMetal, uEmissive = uEmissive, uPattern = uPattern, rgb = appearance.outfitRgb)
        drawPart(cube, vpMatrix, root, 0.15f, 0.36f - crouchDrop * 0.4f, 0f, 0.18f, 0.72f - crouchDrop * 0.4f, 0.20f, rotXDeg = -legSwing, uMVP = uMVP, uModel = uModel, uColor = uColor, uRough = uRough, uMetal = uMetal, uEmissive = uEmissive, uPattern = uPattern, rgb = appearance.outfitRgb)
        drawPart(cube, vpMatrix, root, -0.15f, 0.09f, -0.04f, 0.19f, 0.18f, 0.28f, uMVP = uMVP, uModel = uModel, uColor = uColor, uRough = uRough, uMetal = uMetal, uEmissive = uEmissive, uPattern = uPattern, rgb = appearance.bootsRgb, rough = 0.85f)
        drawPart(cube, vpMatrix, root, 0.15f, 0.09f, -0.04f, 0.19f, 0.18f, 0.28f, uMVP = uMVP, uModel = uModel, uColor = uColor, uRough = uRough, uMetal = uMetal, uEmissive = uEmissive, uPattern = uPattern, rgb = appearance.bootsRgb, rough = 0.85f)

        // Torso & Tactical Vest
        val torsoY = 1.08f - crouchDrop
        drawPart(cube, vpMatrix, root, 0f, torsoY, 0f, 0.46f, 0.62f, 0.26f, uMVP = uMVP, uModel = uModel, uColor = uColor, uRough = uRough, uMetal = uMetal, uEmissive = uEmissive, uPattern = uPattern, rgb = appearance.outfitRgb)
        drawPart(cube, vpMatrix, root, 0f, torsoY + 0.03f, 0f, 0.50f * appearance.vestBulk, 0.48f, 0.31f * appearance.vestBulk, uMVP = uMVP, uModel = uModel, uColor = uColor, uRough = uRough, uMetal = uMetal, uEmissive = uEmissive, uPattern = uPattern, rgb = appearance.vestRgb, rough = 0.65f, metal = 0.22f)

        // Heavy armor shoulder pauldrons for Heavy/Boss
        if (enemyArchetype == EnemyArchetype.ARMORED_HEAVY || enemyArchetype?.isBoss == true) {
            val accent = enemyArchetype.accentColorRgb
            drawPart(cube, vpMatrix, root, -0.34f, torsoY + 0.22f, 0f, 0.22f, 0.22f, 0.28f, uMVP = uMVP, uModel = uModel, uColor = uColor, uRough = uRough, uMetal = uMetal, uEmissive = uEmissive, uPattern = uPattern, rgb = accent, rough = 0.42f, metal = 0.65f)
            drawPart(cube, vpMatrix, root, 0.34f, torsoY + 0.22f, 0f, 0.22f, 0.22f, 0.28f, uMVP = uMVP, uModel = uModel, uColor = uColor, uRough = uRough, uMetal = uMetal, uEmissive = uEmissive, uPattern = uPattern, rgb = accent, rough = 0.42f, metal = 0.65f)
        }

        // Backpack
        if (appearance.hasBackpack) {
            drawPart(cube, vpMatrix, root, 0f, torsoY + 0.04f, 0.22f, 0.34f, 0.44f, 0.18f, uMVP = uMVP, uModel = uModel, uColor = uColor, uRough = uRough, uMetal = uMetal, uEmissive = uEmissive, uPattern = uPattern, rgb = appearance.backpackRgb, rough = 0.8f)
        }

        // Head, Tactical Helmet & Visor
        val headY = 1.56f - crouchDrop
        drawPart(sphere, vpMatrix, root, 0f, headY, 0f, 0.28f, 0.30f, 0.28f, uMVP = uMVP, uModel = uModel, uColor = uColor, uRough = uRough, uMetal = uMetal, uEmissive = uEmissive, uPattern = uPattern, rgb = skinRgb, rough = 0.78f)
        drawPart(sphere, vpMatrix, root, 0f, headY + 0.06f, 0.02f, 0.33f, 0.24f, 0.33f, uMVP = uMVP, uModel = uModel, uColor = uColor, uRough = uRough, uMetal = uMetal, uEmissive = uEmissive, uPattern = uPattern, rgb = appearance.helmetRgb, rough = 0.55f, metal = 0.3f)
        drawPart(cube, vpMatrix, root, 0f, headY + 0.03f, -0.15f, 0.22f, 0.08f, 0.06f, uMVP = uMVP, uModel = uModel, uColor = uColor, uRough = uRough, uMetal = uMetal, uEmissive = uEmissive, uPattern = uPattern, rgb = appearance.visorRgb, rough = 0.15f, metal = 0.6f, emissive = if (enemyArchetype != null) 0.55f else 0.18f)

        // Arms & Gloves
        val rightArmPitch = if (isAiming || muzzleFlash) -78f else -42f + armBob
        val leftArmPitch = if (isAiming || muzzleFlash) -68f else -28f - armBob
        drawPart(cube, vpMatrix, root, 0.31f, torsoY + 0.10f, -0.12f, 0.15f, 0.48f, 0.15f, rotXDeg = rightArmPitch, uMVP = uMVP, uModel = uModel, uColor = uColor, uRough = uRough, uMetal = uMetal, uEmissive = uEmissive, uPattern = uPattern, rgb = appearance.outfitRgb)
        drawPart(cube, vpMatrix, root, -0.31f, torsoY + 0.10f, -0.10f, 0.15f, 0.48f, 0.15f, rotXDeg = leftArmPitch, uMVP = uMVP, uModel = uModel, uColor = uColor, uRough = uRough, uMetal = uMetal, uEmissive = uEmissive, uPattern = uPattern, rgb = appearance.outfitRgb)
        drawPart(cube, vpMatrix, root, 0.28f, torsoY + 0.06f, -0.36f, 0.12f, 0.12f, 0.14f, uMVP = uMVP, uModel = uModel, uColor = uColor, uRough = uRough, uMetal = uMetal, uEmissive = uEmissive, uPattern = uPattern, rgb = appearance.glovesRgb)

        // Ballistic Shield for Shield Defender
        if (enemyArchetype?.hasFrontalShield == true) {
            drawPart(cube, vpMatrix, root, -0.26f, torsoY - 0.05f, -0.48f, 0.68f, 1.24f, 0.09f, uMVP = uMVP, uModel = uModel, uColor = uColor, uRough = uRough, uMetal = uMetal, uEmissive = uEmissive, uPattern = uPattern, rgb = floatArrayOf(0.14f, 0.22f, 0.30f), rough = 0.35f, metal = 0.72f)
            drawPart(cube, vpMatrix, root, -0.26f, torsoY + 0.32f, -0.53f, 0.44f, 0.16f, 0.04f, uMVP = uMVP, uModel = uModel, uColor = uColor, uRough = uRough, uMetal = uMetal, uEmissive = uEmissive, uPattern = uPattern, rgb = floatArrayOf(0.32f, 0.72f, 0.94f), rough = 0.15f, metal = 0.4f, emissive = 0.35f)
        }

        // Weapon in right hands
        val wRoot = FloatArray(16)
        System.arraycopy(root, 0, wRoot, 0, 16)
        Matrix.translateM(wRoot, 0, 0.24f, torsoY + 0.14f, -0.44f)
        if (weaponStats != null) {
            drawWeaponModel(vpMatrix, wRoot, weaponStats, 1.0f, muzzleFlash, uMVP, uModel, uColor, uRough, uMetal, uEmissive, uPattern)
        } else {
            val len = if (enemyArchetype == EnemyArchetype.PRECISION_MARKSMAN) 1.05f else 0.72f
            drawPart(cube, vpMatrix, wRoot, 0f, 0f, -len * 0.35f, 0.08f, 0.12f, len, uMVP = uMVP, uModel = uModel, uColor = uColor, uRough = uRough, uMetal = uMetal, uEmissive = uEmissive, uPattern = uPattern, rgb = floatArrayOf(0.16f, 0.17f, 0.19f), rough = 0.4f, metal = 0.75f)
            if (muzzleFlash) {
                drawPart(sphere, vpMatrix, wRoot, 0f, 0.02f, -len * 0.9f, 0.24f, 0.24f, 0.32f, uMVP = uMVP, uModel = uModel, uColor = uColor, uRough = uRough, uMetal = uMetal, uEmissive = uEmissive, uPattern = uPattern, rgb = floatArrayOf(1f, 0.78f, 0.28f), emissive = 1.6f)
            }
        }
    }

    /** Renders any of the 8 weapons with live attachment geometry & skin colors. */
    fun drawWeaponModel(
        vpMatrix: FloatArray,
        parentMat: FloatArray,
        stats: EffectiveWeaponStats,
        scale: Float,
        muzzleFlash: Boolean,
        uMVP: Int, uModel: Int, uColor: Int, uRough: Int, uMetal: Int, uEmissive: Int, uPattern: Int
    ) {
        val wMat = FloatArray(16)
        System.arraycopy(parentMat, 0, wMat, 0, 16)
        if (scale != 1f) Matrix.scaleM(wMat, 0, scale, scale, scale)

        val w = stats.weapon
        val bodyLen = w.bodyLength
        val barrelLen = w.barrelLength + (if (stats.hasLongBarrel) 0.14f else 0f)
        val stockLen = w.stockLength
        val isPulse = w.id == WeaponId.RAIL_X1_HEAVY

        // Main receiver body
        drawPart(cube, vpMatrix, wMat, 0f, 0f, 0f, 0.09f, 0.13f, bodyLen, uMVP = uMVP, uModel = uModel, uColor = uColor, uRough = uRough, uMetal = uMetal, uEmissive = uEmissive, uPattern = uPattern, rgb = stats.bodyRgb, rough = 0.38f, metal = 0.68f)
        // Barrel
        drawPart(cylinder, vpMatrix, wMat, 0f, 0.025f, -(bodyLen * 0.5f + barrelLen * 0.5f), 0.042f, barrelLen, 0.042f, rotXDeg = 90f, uMVP = uMVP, uModel = uModel, uColor = uColor, uRough = uRough, uMetal = uMetal, uEmissive = uEmissive, uPattern = uPattern, rgb = floatArrayOf(0.14f, 0.15f, 0.17f), rough = 0.32f, metal = 0.85f)
        // Handguard / rail accent
        drawPart(cube, vpMatrix, wMat, 0f, 0.01f, -bodyLen * 0.32f, 0.105f, 0.10f, bodyLen * 0.44f, uMVP = uMVP, uModel = uModel, uColor = uColor, uRough = uRough, uMetal = uMetal, uEmissive = uEmissive, uPattern = uPattern, rgb = stats.accentRgb, rough = 0.45f, metal = 0.45f, emissive = if (isPulse) 0.65f else 0f)
        // Stock
        if (stockLen > 0.02f) {
            drawPart(cube, vpMatrix, wMat, 0f, -0.02f, bodyLen * 0.5f + stockLen * 0.5f, 0.075f, 0.13f, stockLen, uMVP = uMVP, uModel = uModel, uColor = uColor, uRough = uRough, uMetal = uMetal, uEmissive = uEmissive, uPattern = uPattern, rgb = stats.accentRgb, rough = 0.62f, metal = 0.25f)
        }
        // Pistol grip
        drawPart(cube, vpMatrix, wMat, 0f, -0.12f, bodyLen * 0.22f, 0.065f, 0.16f, 0.08f, rotXDeg = 18f, uMVP = uMVP, uModel = uModel, uColor = uColor, uRough = uRough, uMetal = uMetal, uEmissive = uEmissive, uPattern = uPattern, rgb = floatArrayOf(0.12f, 0.13f, 0.14f))
        // Magazine (scales taller if Extended Magazine equipped!)
        val magH = if (stats.hasExtendedMag) 0.28f else if (w.id == WeaponId.MG48_LMG) 0.22f else 0.18f
        val magW = if (w.id == WeaponId.MG48_LMG) 0.18f else 0.068f
        drawPart(cube, vpMatrix, wMat, 0f, -0.08f - magH * 0.42f, -0.06f, magW, magH, 0.11f, rotXDeg = -10f, uMVP = uMVP, uModel = uModel, uColor = uColor, uRough = uRough, uMetal = uMetal, uEmissive = uEmissive, uPattern = uPattern, rgb = floatArrayOf(0.20f, 0.21f, 0.23f), rough = 0.48f, metal = 0.6f)

        // Optional Optic Sight
        if (stats.hasOptic || w.id == WeaponId.SR308_PRECISION || w.id == WeaponId.DMR14_MARKSMAN) {
            val scopeLen = if (w.id == WeaponId.SR308_PRECISION) 0.34f else 0.18f
            drawPart(cylinder, vpMatrix, wMat, 0f, 0.115f, 0.04f, 0.065f, scopeLen, 0.065f, rotXDeg = 90f, uMVP = uMVP, uModel = uModel, uColor = uColor, uRough = uRough, uMetal = uMetal, uEmissive = uEmissive, uPattern = uPattern, rgb = floatArrayOf(0.12f, 0.14f, 0.16f), rough = 0.28f, metal = 0.8f)
        }
        // Optional Foregrip
        if (stats.hasGrip) {
            drawPart(cube, vpMatrix, wMat, 0f, -0.11f, -bodyLen * 0.35f, 0.055f, 0.14f, 0.055f, uMVP = uMVP, uModel = uModel, uColor = uColor, uRough = uRough, uMetal = uMetal, uEmissive = uEmissive, uPattern = uPattern, rgb = floatArrayOf(0.18f, 0.19f, 0.20f))
        }
        // Optional Stabilizer muzzle device
        if (stats.hasStabilizer) {
            drawPart(cylinder, vpMatrix, wMat, 0f, 0.025f, -(bodyLen * 0.5f + barrelLen + 0.05f), 0.068f, 0.12f, 0.068f, rotXDeg = 90f, uMVP = uMVP, uModel = uModel, uColor = uColor, uRough = uRough, uMetal = uMetal, uEmissive = uEmissive, uPattern = uPattern, rgb = floatArrayOf(0.28f, 0.30f, 0.33f), rough = 0.3f, metal = 0.85f)
        }
        // Muzzle Flash burst
        if (muzzleFlash) {
            val flashCol = if (isPulse) floatArrayOf(0.32f, 0.88f, 1.0f) else floatArrayOf(1.0f, 0.82f, 0.32f)
            drawPart(sphere, vpMatrix, wMat, 0f, 0.025f, -(bodyLen * 0.5f + barrelLen + 0.12f), 0.26f, 0.26f, 0.38f, uMVP = uMVP, uModel = uModel, uColor = uColor, uRough = uRough, uMetal = uMetal, uEmissive = uEmissive, uPattern = uPattern, rgb = flashCol, emissive = 1.8f)
        }
    }

    private fun buildCubeVertices(): FloatArray {
        val out = ArrayList<Float>(36 * 6)
        fun face(nx: Float, ny: Float, nz: Float, p0: FloatArray, p1: FloatArray, p2: FloatArray, p3: FloatArray) {
            fun v(p: FloatArray) { out.add(p[0]); out.add(p[1]); out.add(p[2]); out.add(nx); out.add(ny); out.add(nz) }
            v(p0); v(p1); v(p2); v(p0); v(p2); v(p3)
        }
        val h = 0.5f
        face(0f, 0f, 1f, floatArrayOf(-h, -h, h), floatArrayOf(h, -h, h), floatArrayOf(h, h, h), floatArrayOf(-h, h, h))
        face(0f, 0f, -1f, floatArrayOf(h, -h, -h), floatArrayOf(-h, -h, -h), floatArrayOf(-h, h, -h), floatArrayOf(h, h, -h))
        face(-1f, 0f, 0f, floatArrayOf(-h, -h, -h), floatArrayOf(-h, -h, h), floatArrayOf(-h, h, h), floatArrayOf(-h, h, -h))
        face(1f, 0f, 0f, floatArrayOf(h, -h, h), floatArrayOf(h, -h, -h), floatArrayOf(h, h, -h), floatArrayOf(h, h, h))
        face(0f, 1f, 0f, floatArrayOf(-h, h, h), floatArrayOf(h, h, h), floatArrayOf(h, h, -h), floatArrayOf(-h, h, -h))
        face(0f, -1f, 0f, floatArrayOf(-h, -h, -h), floatArrayOf(h, -h, -h), floatArrayOf(h, -h, h), floatArrayOf(-h, -h, h))
        return out.toFloatArray()
    }

    private fun buildCylinderVertices(seg: Int): FloatArray {
        val out = ArrayList<Float>()
        val r = 0.5f
        val h = 0.5f
        fun push(x: Float, y: Float, z: Float, nx: Float, ny: Float, nz: Float) {
            out.add(x); out.add(y); out.add(z); out.add(nx); out.add(ny); out.add(nz)
        }
        for (i in 0 until seg) {
            val a0 = (2.0 * PI * i / seg).toFloat()
            val a1 = (2.0 * PI * (i + 1) / seg).toFloat()
            val c0 = cos(a0); val s0 = sin(a0)
            val c1 = cos(a1); val s1 = sin(a1)
            // Side quad (CCW outward winding so GL_CULL_FACE never culls visible exterior)
            push(c0 * r, -h, s0 * r, c0, 0f, s0)
            push(c1 * r, h, s1 * r, c1, 0f, s1)
            push(c1 * r, -h, s1 * r, c1, 0f, s1)
            push(c0 * r, -h, s0 * r, c0, 0f, s0)
            push(c0 * r, h, s0 * r, c0, 0f, s0)
            push(c1 * r, h, s1 * r, c1, 0f, s1)
            // Top cap
            push(0f, h, 0f, 0f, 1f, 0f)
            push(c1 * r, h, s1 * r, 0f, 1f, 0f)
            push(c0 * r, h, s0 * r, 0f, 1f, 0f)
            // Bottom cap
            push(0f, -h, 0f, 0f, -1f, 0f)
            push(c0 * r, -h, s0 * r, 0f, -1f, 0f)
            push(c1 * r, -h, s1 * r, 0f, -1f, 0f)
        }
        return out.toFloatArray()
    }

    private fun buildSphereVertices(stacks: Int, slices: Int): FloatArray {
        val out = ArrayList<Float>()
        val r = 0.5f
        fun push(x: Float, y: Float, z: Float) {
            out.add(x * r); out.add(y * r); out.add(z * r); out.add(x); out.add(y); out.add(z)
        }
        for (i in 0 until stacks) {
            val p0 = (PI * (-0.5 + i.toDouble() / stacks)).toFloat()
            val p1 = (PI * (-0.5 + (i + 1).toDouble() / stacks)).toFloat()
            for (j in 0 until slices) {
                val t0 = (2.0 * PI * j / slices).toFloat()
                val t1 = (2.0 * PI * (j + 1) / slices).toFloat()
                val x00 = cos(p0) * cos(t0); val y00 = sin(p0); val z00 = cos(p0) * sin(t0)
                val x10 = cos(p1) * cos(t0); val y10 = sin(p1); val z10 = cos(p1) * sin(t0)
                val x11 = cos(p1) * cos(t1); val y11 = sin(p1); val z11 = cos(p1) * sin(t1)
                val x01 = cos(p0) * cos(t1); val y01 = sin(p0); val z01 = cos(p0) * sin(t1)
                push(x00, y00, z00); push(x10, y10, z10); push(x11, y11, z11)
                push(x00, y00, z00); push(x11, y11, z11); push(x01, y01, z01)
            }
        }
        return out.toFloatArray()
    }
}
