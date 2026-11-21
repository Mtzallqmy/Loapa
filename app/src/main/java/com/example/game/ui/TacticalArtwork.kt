package com.example.game.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.game.data.CityId
import com.example.ui.theme.ArmorCyan
import com.example.ui.theme.DesertGold
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.TacticalEmerald

/**
 * Vector-crisp, resolution-independent Compose Canvas artwork replacing external raster JPEGs.
 * Eliminates bitmap OOM/decoding errors across all screen densities and emulators.
 */
@Composable
fun TacticalCrestEmblem(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        drawRoundRect(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF1F2D3A), Color(0xFF10171F), Color(0xFF0A0F14)),
                start = Offset.Zero,
                end = Offset(w, h)
            ),
            cornerRadius = CornerRadius(w * 0.16f, h * 0.16f)
        )

        // Outer tactical shield
        val shield = Path().apply {
            moveTo(w * 0.50f, h * 0.12f)
            lineTo(w * 0.82f, h * 0.24f)
            lineTo(w * 0.82f, h * 0.52f)
            lineTo(w * 0.50f, h * 0.88f)
            lineTo(w * 0.18f, h * 0.52f)
            lineTo(w * 0.18f, h * 0.24f)
            close()
        }
        drawPath(shield, Color(0xFF131C26))
        drawPath(shield, DesertGold, style = Stroke(width = w * 0.036f))

        // Inner bevel shield
        val innerShield = Path().apply {
            moveTo(w * 0.50f, h * 0.19f)
            lineTo(w * 0.75f, h * 0.29f)
            lineTo(w * 0.75f, h * 0.50f)
            lineTo(w * 0.50f, h * 0.80f)
            lineTo(w * 0.25f, h * 0.50f)
            lineTo(w * 0.25f, h * 0.29f)
            close()
        }
        drawPath(innerShield, Color(0xFF1C2936))

        // Upper metallic chevron
        val chev1 = Path().apply {
            moveTo(w * 0.50f, h * 0.29f)
            lineTo(w * 0.67f, h * 0.42f)
            lineTo(w * 0.67f, h * 0.50f)
            lineTo(w * 0.50f, h * 0.37f)
            lineTo(w * 0.33f, h * 0.50f)
            lineTo(w * 0.33f, h * 0.42f)
            close()
        }
        drawPath(chev1, DesertGold)

        // Lower bright gold chevron
        val chev2 = Path().apply {
            moveTo(w * 0.50f, h * 0.44f)
            lineTo(w * 0.65f, h * 0.55f)
            lineTo(w * 0.65f, h * 0.62f)
            lineTo(w * 0.50f, h * 0.51f)
            lineTo(w * 0.35f, h * 0.62f)
            lineTo(w * 0.35f, h * 0.55f)
            close()
        }
        drawPath(chev2, Color(0xFFFFCA62))

        // Emerald precision reticle at shield base
        val rc = Offset(w * 0.50f, h * 0.66f)
        val rr = w * 0.055f
        drawCircle(TacticalEmerald, rr, rc, style = Stroke(width = w * 0.018f))
        drawLine(TacticalEmerald, Offset(rc.x, rc.y - rr * 1.55f), Offset(rc.x, rc.y - rr * 0.75f), w * 0.016f, StrokeCap.Round)
        drawLine(TacticalEmerald, Offset(rc.x, rc.y + rr * 0.75f), Offset(rc.x, rc.y + rr * 1.55f), w * 0.016f, StrokeCap.Round)
        drawLine(TacticalEmerald, Offset(rc.x - rr * 1.55f, rc.y), Offset(rc.x - rr * 0.75f, rc.y), w * 0.016f, StrokeCap.Round)
        drawLine(TacticalEmerald, Offset(rc.x + rr * 0.75f, rc.y), Offset(rc.x + rr * 1.55f, rc.y), w * 0.016f, StrokeCap.Round)
        drawCircle(TacticalEmerald, w * 0.016f, rc)
    }
}

/** Atmospheric cinematic twilight military skyline & operator silhouette for the Main Menu. */
@Composable
fun HeroMainMenuBackdrop(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Dusk sky gradient with warm desert horizon glow
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF0A1018),
                    Color(0xFF182534),
                    Color(0xFF3E352B),
                    Color(0xFF14181E)
                )
            )
        )

        // Warm tactical amber sun haze on right horizon
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x66E5A93C), Color(0x22C77A28), Color.Transparent),
                center = Offset(w * 0.72f, h * 0.54f),
                radius = w * 0.44f
            ),
            radius = w * 0.44f,
            center = Offset(w * 0.72f, h * 0.54f)
        )

        // Subtle tactical HUD coordinate grid lines
        val gridCol = Color(0x14E5A93C)
        for (i in 1..7) {
            val x = w * (i / 8f)
            drawLine(gridCol, Offset(x, 0f), Offset(x, h), 1f)
        }
        for (j in 1..4) {
            val y = h * (j / 5f)
            drawLine(gridCol, Offset(0f, y), Offset(w, y), 1f)
        }

        // Distant mountain ridge silhouette
        val mountains = Path().apply {
            moveTo(0f, h)
            lineTo(0f, h * 0.62f)
            lineTo(w * 0.16f, h * 0.48f)
            lineTo(w * 0.34f, h * 0.58f)
            lineTo(w * 0.56f, h * 0.42f)
            lineTo(w * 0.78f, h * 0.52f)
            lineTo(w, h * 0.39f)
            lineTo(w, h)
            close()
        }
        drawPath(mountains, Color(0xFF141C24))

        // Midground urban skyline towers + crane
        val towers = listOf(
            Triple(0.06f, 0.46f, 0.08f),
            Triple(0.16f, 0.54f, 0.07f),
            Triple(0.28f, 0.43f, 0.09f),
            Triple(0.42f, 0.50f, 0.08f),
            Triple(0.56f, 0.38f, 0.10f),
            Triple(0.70f, 0.48f, 0.09f),
            Triple(0.84f, 0.41f, 0.11f)
        )
        towers.forEachIndexed { idx, (fx, fy, fw) ->
            drawRect(
                color = if (idx % 2 == 0) Color(0xFF0E141B) else Color(0xFF121A22),
                topLeft = Offset(w * fx, h * fy),
                size = Size(w * fw, h * (1f - fy))
            )
            // Warm lit tactical windows
            val cols = 3
            for (r in 0..3) {
                for (c in 0 until cols) {
                    if ((idx + r + c) % 3 == 0) {
                        drawRect(
                            color = DesertGold.copy(alpha = 0.25f),
                            topLeft = Offset(w * (fx + 0.014f + c * 0.022f), h * (fy + 0.04f + r * 0.065f)),
                            size = Size(w * 0.010f, h * 0.022f)
                        )
                    }
                }
            }
        }

        // Harbor/Urban gantry silhouette beam
        drawLine(Color(0xFF0A0E14), Offset(w * 0.64f, h * 0.32f), Offset(w * 0.88f, h * 0.25f), 4f)
        drawLine(Color(0xFF0A0E14), Offset(w * 0.76f, h * 0.28f), Offset(w * 0.76f, h * 0.85f), 5f)

        // Foreground tactical rooftop parapet & soldier silhouette on right-center
        drawRect(Color(0xFF080C10), Offset(0f, h * 0.82f), Size(w, h * 0.18f))
        val opX = w * 0.73f
        val opBaseY = h * 0.84f
        // Helmet & visor glow
        drawCircle(Color(0xFF0D131A), h * 0.045f, Offset(opX, opBaseY - h * 0.29f))
        drawRect(DesertGold.copy(alpha = 0.7f), Offset(opX - h * 0.03f, opBaseY - h * 0.295f), Size(h * 0.036f, h * 0.012f))
        // Shoulders & tactical plate carrier torso
        drawRoundRect(
            color = Color(0xFF0D131A),
            topLeft = Offset(opX - h * 0.068f, opBaseY - h * 0.24f),
            size = Size(h * 0.136f, h * 0.18f),
            cornerRadius = CornerRadius(h * 0.03f, h * 0.03f)
        )
        // Tactical rifle angled forward
        drawLine(
            color = Color(0xFF0A0F14),
            start = Offset(opX - h * 0.16f, opBaseY - h * 0.17f),
            end = Offset(opX + h * 0.07f, opBaseY - h * 0.12f),
            strokeWidth = h * 0.022f,
            cap = StrokeCap.Round
        )
        // Laser sight beam
        drawLine(
            color = TacticalEmerald.copy(alpha = 0.38f),
            start = Offset(opX - h * 0.16f, opBaseY - h * 0.17f),
            end = Offset(w * 0.42f, h * 0.58f),
            strokeWidth = 2f
        )

        // Subtle atmospheric dust motes
        val sparks = listOf(0.55f to 0.32f, 0.63f to 0.46f, 0.78f to 0.36f, 0.85f to 0.58f, 0.68f to 0.64f)
        sparks.forEach { (sx, sy) ->
            drawCircle(DesertGold.copy(alpha = 0.35f), 2.5f, Offset(w * sx, h * sy))
        }
    }
}

/** Bespoke distinct 4-city theater artwork banners for Campaign & Mission Briefing screens. */
@Composable
fun CityArtworkCanvas(cityId: CityId, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        when (cityId) {
            CityId.RIVER_CITY -> {
                // Warm twilight urban metropolis with concrete skyline & river reflection
                drawRect(Brush.verticalGradient(listOf(Color(0xFF1A2A3B), Color(0xFF3E4E5E), Color(0xFF1A252E))))
                // River band at bottom
                drawRect(Color(0xFF123044), Offset(0f, h * 0.72f), Size(w, h * 0.28f))
                // City blocks
                val buildings = listOf(
                    Triple(0.05f, 0.30f, 0.11f), Triple(0.18f, 0.20f, 0.13f),
                    Triple(0.34f, 0.38f, 0.10f), Triple(0.47f, 0.16f, 0.14f),
                    Triple(0.64f, 0.28f, 0.12f), Triple(0.79f, 0.22f, 0.15f)
                )
                buildings.forEachIndexed { i, (bx, by, bw) ->
                    drawRect(
                        color = if (i % 2 == 0) Color(0xFF18222B) else Color(0xFF212D38),
                        topLeft = Offset(w * bx, h * by),
                        size = Size(w * bw, h * (0.74f - by))
                    )
                    drawRect(DesertGold.copy(alpha = 0.35f), Offset(w * (bx + 0.02f), h * (by + 0.08f)), Size(w * 0.02f, h * 0.05f))
                }
                // Bridge deck over river
                drawLine(DesertGold.copy(alpha = 0.55f), Offset(0f, h * 0.73f), Offset(w, h * 0.73f), 3f)
            }
            CityId.MIRAGE_CITY -> {
                // Arid desert golden dunes, fuel silos & watchtowers
                drawRect(Brush.verticalGradient(listOf(Color(0xFF423121), Color(0xFF8C6438), Color(0xFFC29052))))
                drawCircle(Color(0x88FFD27D), h * 0.22f, Offset(w * 0.78f, h * 0.30f))
                // Distant sand dunes
                val dune1 = Path().apply {
                    moveTo(0f, h)
                    lineTo(0f, h * 0.62f)
                    cubicTo(w * 0.25f, h * 0.42f, w * 0.55f, h * 0.74f, w, h * 0.54f)
                    lineTo(w, h)
                    close()
                }
                drawPath(dune1, Color(0xFF6B4B29))
                // Industrial warehouse + cylindrical silos + watchtower
                drawRect(Color(0xFF2B2118), Offset(w * 0.14f, h * 0.46f), Size(w * 0.24f, h * 0.36f))
                drawRect(Color(0xFF3A2D20), Offset(w * 0.44f, h * 0.40f), Size(w * 0.08f, h * 0.42f))
                drawRect(Color(0xFF33271C), Offset(w * 0.54f, h * 0.42f), Size(w * 0.08f, h * 0.40f))
                drawRect(Color(0xFF1E1711), Offset(w * 0.72f, h * 0.26f), Size(w * 0.07f, h * 0.58f))
                drawRect(Color(0xFF281F16), Offset(0f, h * 0.76f), Size(w, h * 0.24f))
            }
            CityId.HARBOR_CITY -> {
                // Coastal steel port, towering gantry cranes & stacked shipping containers
                drawRect(Brush.verticalGradient(listOf(Color(0xFF102030), Color(0xFF243B52), Color(0xFF112333))))
                // Sea water strip
                drawRect(Color(0xFF0A2438), Offset(0f, h * 0.72f), Size(w, h * 0.28f))
                // Tall orange-rust gantry cranes
                listOf(0.28f, 0.72f).forEach { cx ->
                    drawLine(Color(0xFFD16634), Offset(w * cx, h * 0.16f), Offset(w * cx, h * 0.74f), 4f)
                    drawLine(Color(0xFFD16634), Offset(w * (cx - 0.14f), h * 0.22f), Offset(w * (cx + 0.18f), h * 0.16f), 3.5f)
                }
                // Colorful stacked shipping containers
                val containers = listOf(
                    Triple(0.08f, 0.58f, Color(0xFFAE3D2E)),
                    Triple(0.24f, 0.58f, Color(0xFF28608C)),
                    Triple(0.24f, 0.46f, Color(0xFFC27A29)),
                    Triple(0.46f, 0.58f, Color(0xFF357A52)),
                    Triple(0.64f, 0.58f, Color(0xFFAE3D2E)),
                    Triple(0.80f, 0.54f, Color(0xFF28608C))
                )
                containers.forEach { (cx, cy, col) ->
                    drawRect(col, Offset(w * cx, h * cy), Size(w * 0.14f, h * 0.15f))
                    drawRect(ObsidianBlack.copy(alpha = 0.35f), Offset(w * cx, h * cy), Size(w * 0.14f, h * 0.15f), style = Stroke(1.5f))
                }
                drawLine(ArmorCyan.copy(alpha = 0.45f), Offset(0f, h * 0.80f), Offset(w, h * 0.80f), 2f)
            }
            CityId.MOUNTAIN_CITY -> {
                // Alpine rocky fortress stronghold with summit radar beacon
                drawRect(Brush.verticalGradient(listOf(Color(0xFF152232), Color(0xFF3A4958), Color(0xFF1D2228))))
                val ridge1 = Path().apply {
                    moveTo(0f, h)
                    lineTo(w * 0.22f, h * 0.32f)
                    lineTo(w * 0.48f, h * 0.56f)
                    lineTo(w * 0.74f, h * 0.18f)
                    lineTo(w, h * 0.44f)
                    lineTo(w, h)
                    close()
                }
                drawPath(ridge1, Color(0xFF1E2832))
                val ridge2 = Path().apply {
                    moveTo(0f, h)
                    lineTo(w * 0.32f, h * 0.52f)
                    lineTo(w * 0.60f, h * 0.38f)
                    lineTo(w * 0.88f, h * 0.62f)
                    lineTo(w, h * 0.50f)
                    lineTo(w, h)
                    close()
                }
                drawPath(ridge2, Color(0xFF121920))
                // Citadel towers + summit radar beacon
                drawRect(Color(0xFF2A333C), Offset(w * 0.69f, h * 0.20f), Size(w * 0.10f, h * 0.26f))
                drawLine(DesertGold, Offset(w * 0.74f, h * 0.10f), Offset(w * 0.74f, h * 0.20f), 2.5f)
                drawCircle(DesertGold.copy(alpha = 0.85f), 5f, Offset(w * 0.74f, h * 0.10f))
            }
        }
    }
}
