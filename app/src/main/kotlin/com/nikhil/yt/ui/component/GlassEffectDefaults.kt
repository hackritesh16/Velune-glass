package com.nikhil.yt.ui.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class GlassSurfaceStyle(
    val surfaceTint: Color,
    val surfaceAlpha: Float,
    val overlayColor: Color,
    val overlayAlpha: Float,
    val blurRadius: Dp,
    val useVibrancy: Boolean,
    val useLens: Boolean,
    val lensHeight: Dp,
    val lensAmount: Dp,
    val borderColor: Color,
    val borderAlpha: Float,
    val backgroundDimAlpha: Float,
    val backgroundDimColor: Color,
    val sheenAlpha: Float = 0.12f,
)

object GlassEffectDefaults {

    val NavigationBarDark = GlassSurfaceStyle(
        surfaceTint = Color.Black,
        surfaceAlpha = 0.18f,
        overlayColor = Color.Black,
        overlayAlpha = 0.16f,
        blurRadius = 34.dp,
        useVibrancy = true,
        useLens = true,
        lensHeight = 20.dp,
        lensAmount = 32.dp,
        borderColor = Color.White,
        borderAlpha = 0.20f,
        backgroundDimAlpha = 0.22f,
        backgroundDimColor = Color.Black,
    )

    val NavigationBarLight = GlassSurfaceStyle(
        surfaceTint = Color.White,
        surfaceAlpha = 0.34f,
        overlayColor = Color.White,
        overlayAlpha = 0.26f,
        blurRadius = 40.dp,
        useVibrancy = true,
        useLens = true,
        lensHeight = 20.dp,
        lensAmount = 32.dp,
        borderColor = Color.White,
        borderAlpha = 0.40f,
        backgroundDimAlpha = 0.08f,
        backgroundDimColor = Color.Black,
    )

    val NavigationBarPureBlack = GlassSurfaceStyle(
        surfaceTint = Color(0xFF050508),
        surfaceAlpha = 0.46f,
        overlayColor = Color(0xFF0A0A14),
        overlayAlpha = 0.30f,
        blurRadius = 32.dp,
        useVibrancy = true,
        useLens = true,
        lensHeight = 14.dp,
        lensAmount = 24.dp,
        borderColor = Color.White,
        borderAlpha = 0.10f,
        backgroundDimAlpha = 0.40f,
        backgroundDimColor = Color.Black,
    )

    val MiniPlayerDark = GlassSurfaceStyle(
        surfaceTint = Color.Black,
        surfaceAlpha = 0.20f,
        overlayColor = Color.Black,
        overlayAlpha = 0.16f,
        blurRadius = 32.dp,
        useVibrancy = true,
        useLens = true,
        lensHeight = 20.dp,
        lensAmount = 32.dp,
        borderColor = Color.White,
        borderAlpha = 0.22f,
        backgroundDimAlpha = 0.20f,
        backgroundDimColor = Color.Black,
    )

    val MiniPlayerLight = GlassSurfaceStyle(
        surfaceTint = Color.White,
        surfaceAlpha = 0.34f,
        overlayColor = Color.White,
        overlayAlpha = 0.26f,
        blurRadius = 38.dp,
        useVibrancy = true,
        useLens = true,
        lensHeight = 20.dp,
        lensAmount = 32.dp,
        borderColor = Color.White,
        borderAlpha = 0.40f,
        backgroundDimAlpha = 0.08f,
        backgroundDimColor = Color.Black,
    )

    val MiniPlayerPureBlack = GlassSurfaceStyle(
        surfaceTint = Color(0xFF050508),
        surfaceAlpha = 0.44f,
        overlayColor = Color(0xFF0A0A14),
        overlayAlpha = 0.28f,
        blurRadius = 30.dp,
        useVibrancy = true,
        useLens = true,
        lensHeight = 16.dp,
        lensAmount = 26.dp,
        borderColor = Color.White,
        borderAlpha = 0.12f,
        backgroundDimAlpha = 0.38f,
        backgroundDimColor = Color.Black,
    )

    fun navigationBarStyle(isDark: Boolean, isPureBlack: Boolean): GlassSurfaceStyle {
        return when {
            isPureBlack -> NavigationBarPureBlack
            isDark -> NavigationBarDark
            else -> NavigationBarLight
        }
    }

    fun miniPlayerStyle(isDark: Boolean, isPureBlack: Boolean): GlassSurfaceStyle {
        return when {
            isPureBlack -> MiniPlayerPureBlack
            isDark -> MiniPlayerDark
            else -> MiniPlayerLight
        }
    }
}
