package com.example.game.render

import android.opengl.GLES20
import android.util.Log

/**
 * Universal OpenGL ES 2.0 (GLSL ES 1.00) Shader Library.
 * Runs at full hardware acceleration across all Android devices (API 24-36) and
 * software/cloud emulators without requiring ES 3.0-only extensions.
 */
object ShaderLibrary {
    private const val TAG = "CombatFrontsGL"

    private const val WORLD_VERTEX = """
        precision highp float;
        attribute vec3 aPos;
        attribute vec3 aNormal;

        uniform mat4 uMVP;
        uniform mat4 uModel;

        varying vec3 vWorldPos;
        varying vec3 vNormal;

        void main() {
            vec4 wp = uModel * vec4(aPos, 1.0);
            vWorldPos = wp.xyz;
            vec3 n = (uModel * vec4(aNormal, 0.0)).xyz;
            float nLen = length(n);
            vNormal = nLen > 0.0001 ? (n / nLen) : vec3(0.0, 1.0, 0.0);
            gl_Position = uMVP * vec4(aPos, 1.0);
        }
    """

    private const val WORLD_FRAGMENT = """
        precision mediump float;

        varying vec3 vWorldPos;
        varying vec3 vNormal;

        uniform vec3 uColor;
        uniform float uRoughness;
        uniform float uMetallic;
        uniform float uEmissive;
        uniform int uPatternType;
        uniform vec3 uCamPos;
        uniform vec3 uSunDir;
        uniform vec3 uSunColor;
        uniform vec3 uAmbientColor;
        uniform vec3 uFogColor;
        uniform float uFogDensity;
        uniform float uNightFactor;
        uniform vec3 uFlashPos;
        uniform vec3 uFlashColor;

        float hash21(vec2 p) {
            p = fract(p * vec2(123.34, 456.21));
            p += dot(p, p + 45.32);
            return fract(p.x * p.y);
        }

        void main() {
            vec3 N = normalize(vNormal);
            vec3 V = normalize(uCamPos - vWorldPos);
            vec3 L = normalize(uSunDir);
            vec3 H = normalize(L + V);

            vec3 albedo = uColor;
            float rough = clamp(uRoughness, 0.08, 0.98);
            float emissiveAdd = uEmissive;

            // Procedural PBR-inspired surface patterns (zero texture memory bloat)
            if (uPatternType == 1) {
                float isWall = 1.0 - abs(N.y);
                if (isWall > 0.5) {
                    float coordX = abs(N.x) > 0.5 ? vWorldPos.z : vWorldPos.x;
                    float wx = fract(coordX * 0.42);
                    float wy = fract(vWorldPos.y * 0.34);
                    float ledge = step(0.88, wy);
                    albedo *= (1.0 - ledge * 0.22);
                    if (vWorldPos.y > 1.8 && wx > 0.22 && wx < 0.78 && wy > 0.22 && wy < 0.72) {
                        float litSeed = hash21(floor(vec2(coordX * 0.42, vWorldPos.y * 0.34)));
                        if (uNightFactor > 0.3 && litSeed > 0.58) {
                            albedo = vec3(0.95, 0.78, 0.44);
                            emissiveAdd = max(emissiveAdd, 0.75);
                        } else {
                            albedo = mix(vec3(0.11, 0.16, 0.21), albedo, 0.2);
                            rough = 0.18;
                        }
                    }
                }
            } else if (uPatternType == 2) {
                float grain = hash21(floor(vWorldPos.xz * 6.0)) * 0.08 - 0.04;
                albedo += vec3(grain);
                if (abs(vWorldPos.x) < 0.16 && fract(vWorldPos.z * 0.22) > 0.42) {
                    albedo = vec3(0.76, 0.68, 0.42);
                }
            } else if (uPatternType == 3) {
                float ripple = sin(vWorldPos.x * 1.4 + vWorldPos.z * 0.7) * 0.045;
                albedo += vec3(ripple, ripple * 0.85, ripple * 0.55);
            } else if (uPatternType == 4) {
                float coord = abs(N.x) > 0.5 ? vWorldPos.z : vWorldPos.x;
                float rib = sin(coord * 14.0) * 0.08;
                albedo *= (1.0 + rib);
                rough = 0.45;
            } else if (uPatternType == 5) {
                float strata = sin(vWorldPos.y * 2.2 + sin(vWorldPos.x * 0.5) * 1.5) * 0.07;
                albedo += vec3(strata);
            }

            // Hemisphere ambient + directional sunlight
            float ndotl = max(dot(N, L), 0.0);
            float hemi = N.y * 0.5 + 0.5;
            vec3 skyAmb = uAmbientColor * mix(0.55, 1.15, hemi);

            // Blinn-Phong specular highlight
            float shininess = mix(96.0, 6.0, rough);
            float spec = pow(max(dot(N, H), 0.0), shininess) * mix(0.12, 0.75, uMetallic + (1.0 - rough) * 0.4);
            vec3 specColor = mix(vec3(1.0), albedo, uMetallic) * uSunColor * spec;

            // Rim lighting for crisp character & cover readability
            float rim = pow(1.0 - max(dot(N, V), 0.0), 3.0) * 0.14;

            vec3 lit = albedo * (skyAmb + uSunColor * ndotl * 1.08) + specColor + uFogColor * rim;
            // Dynamic muzzle flash & explosion point-light illumination
            float flashEnergy = uFlashColor.r + uFlashColor.g + uFlashColor.b;
            if (flashEnergy > 0.01) {
                vec3 toFlash = uFlashPos - vWorldPos;
                float fDist = length(toFlash);
                float atten = 1.0 / (1.0 + 0.09 * fDist * fDist);
                vec3 fDir = fDist > 0.001 ? (toFlash / fDist) : vec3(0.0, 1.0, 0.0);
                float fNdotL = max(dot(N, fDir), 0.28);
                lit += albedo * uFlashColor * (fNdotL * atten * 1.85);
            }
            lit += albedo * emissiveAdd;

            // Exponential squared atmospheric fog
            float dist = length(uCamPos - vWorldPos);
            float fogArg = max(dist * uFogDensity, 0.0);
            float fogFactor = clamp(exp(-pow(fogArg, 1.85)), 0.0, 1.0);
            vec3 finalRgb = mix(uFogColor, lit, fogFactor);

            // Subtle cinematic contrast curve
            finalRgb = finalRgb / (finalRgb + vec3(0.45)) * 1.38;
            gl_FragColor = vec4(clamp(finalRgb, 0.0, 1.0), 1.0);
        }
    """

    private const val SKY_VERTEX = """
        precision mediump float;
        attribute vec2 aPos;
        varying vec2 vUv;
        void main() {
            vUv = aPos * 0.5 + 0.5;
            gl_Position = vec4(aPos, 0.9999, 1.0);
        }
    """

    private const val SKY_FRAGMENT = """
        precision mediump float;
        varying vec2 vUv;
        uniform vec3 uZenith;
        uniform vec3 uHorizon;
        void main() {
            float t = clamp(pow(max(vUv.y, 0.0001), 0.65), 0.0, 1.0);
            vec3 col = mix(uHorizon, uZenith, t);
            gl_FragColor = vec4(col, 1.0);
        }
    """

    fun createWorldProgram(): Int = linkProgram(WORLD_VERTEX, WORLD_FRAGMENT, bindNormal = true)
    fun createSkyProgram(): Int = linkProgram(SKY_VERTEX, SKY_FRAGMENT, bindNormal = false)

    private fun compileShader(type: Int, src: String): Int {
        val shader = GLES20.glCreateShader(type)
        if (shader == 0) return 0
        GLES20.glShaderSource(shader, src)
        GLES20.glCompileShader(shader)
        val status = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0)
        if (status[0] == 0) {
            Log.e(TAG, "Shader compile error: ${GLES20.glGetShaderInfoLog(shader)}")
            GLES20.glDeleteShader(shader)
            return 0
        }
        return shader
    }

    private fun linkProgram(vertSrc: String, fragSrc: String, bindNormal: Boolean): Int {
        val vs = compileShader(GLES20.GL_VERTEX_SHADER, vertSrc)
        val fs = compileShader(GLES20.GL_FRAGMENT_SHADER, fragSrc)
        if (vs == 0 || fs == 0) {
            if (vs != 0) GLES20.glDeleteShader(vs)
            if (fs != 0) GLES20.glDeleteShader(fs)
            return 0
        }
        val prog = GLES20.glCreateProgram()
        if (prog == 0) {
            GLES20.glDeleteShader(vs)
            GLES20.glDeleteShader(fs)
            return 0
        }
        GLES20.glAttachShader(prog, vs)
        GLES20.glAttachShader(prog, fs)
        GLES20.glBindAttribLocation(prog, 0, "aPos")
        if (bindNormal) {
            GLES20.glBindAttribLocation(prog, 1, "aNormal")
        }
        GLES20.glLinkProgram(prog)
        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(prog, GLES20.GL_LINK_STATUS, linkStatus, 0)
        GLES20.glDeleteShader(vs)
        GLES20.glDeleteShader(fs)
        if (linkStatus[0] == 0) {
            Log.e(TAG, "Program link error: ${GLES20.glGetProgramInfoLog(prog)}")
            GLES20.glDeleteProgram(prog)
            return 0
        }
        return prog
    }
}
