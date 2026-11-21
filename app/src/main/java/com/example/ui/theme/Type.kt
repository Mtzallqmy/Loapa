package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val CairoFontFamily = FontFamily(
    Font(R.font.cairo, FontWeight.Normal),
    Font(R.font.cairo, FontWeight.Medium),
    Font(R.font.cairo, FontWeight.SemiBold),
    Font(R.font.cairo, FontWeight.Bold)
)

val JetBrainsMonoFontFamily = FontFamily(
    Font(R.font.jetbrains_mono, FontWeight.Normal),
    Font(R.font.jetbrains_mono, FontWeight.Medium),
    Font(R.font.jetbrains_mono, FontWeight.Bold)
)

fun buildCombatTypography(scale: Float = 1f): Typography {
    val s = scale.coerceIn(0.85f, 1.3f)
    return Typography(
        displayLarge = TextStyle(
            fontFamily = CairoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (36 * s).sp,
            lineHeight = (44 * s).sp
        ),
        displaySmall = TextStyle(
            fontFamily = CairoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (26 * s).sp,
            lineHeight = (34 * s).sp
        ),
        headlineMedium = TextStyle(
            fontFamily = CairoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (20 * s).sp,
            lineHeight = (28 * s).sp
        ),
        titleLarge = TextStyle(
            fontFamily = CairoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (18 * s).sp,
            lineHeight = (24 * s).sp
        ),
        titleMedium = TextStyle(
            fontFamily = CairoFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = (15 * s).sp,
            lineHeight = (21 * s).sp
        ),
        bodyLarge = TextStyle(
            fontFamily = CairoFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (14 * s).sp,
            lineHeight = (20 * s).sp
        ),
        bodyMedium = TextStyle(
            fontFamily = CairoFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (13 * s).sp,
            lineHeight = (18 * s).sp
        ),
        labelLarge = TextStyle(
            fontFamily = CairoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = (13 * s).sp,
            lineHeight = (18 * s).sp
        ),
        labelMedium = TextStyle(
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = (12 * s).sp,
            lineHeight = (16 * s).sp
        ),
        labelSmall = TextStyle(
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = (10 * s).sp,
            lineHeight = (14 * s).sp
        )
    )
}

val Typography = buildCombatTypography(1f)
