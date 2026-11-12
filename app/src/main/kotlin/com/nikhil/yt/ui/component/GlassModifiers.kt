package com.nikhil.yt.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Specular glass rim: bright at the top-left "light source", fading through the
 * (optional) accent colour, and catching a little light again at the bottom-right.
 * Much more convincing than a flat 1dp outline, and costs nothing at draw time.
 */
fun glassRimBrush(isDark: Boolean, accent: Color = Color.Unspecified): Brush {
    val tint = if (accent == Color.Unspecified) Color.White else accent
    return Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = if (isDark) 0.42f else 0.80f),
            tint.copy(alpha = if (isDark) 0.12f else 0.18f),
            Color.White.copy(alpha = if (isDark) 0.05f else 0.18f),
            Color.White.copy(alpha = if (isDark) 0.24f else 0.55f),
        ),
    )
}

fun Modifier.glassBorder(
    shape: Shape,
    isDark: Boolean,
    accent: Color = Color.Unspecified,
    width: Dp = 1.dp,
): Modifier = this.border(width = width, brush = glassRimBrush(isDark, accent), shape = shape)

/** Glass rim as a [BorderStroke], for components with a `border` parameter (Surface, Card, chips). */
@Composable
fun glassBorderStroke(
    width: Dp = 1.dp,
    accent: Color = MaterialTheme.colorScheme.primary,
): BorderStroke {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return BorderStroke(width, glassRimBrush(isDark, accent))
}

/** Soft top-down sheen drawn over a glass surface (use inside `onDrawSurface`). */
fun glassSheenBrush(alpha: Float, heightPx: Float): Brush =
    Brush.verticalGradient(
        colors = listOf(Color.White.copy(alpha = alpha), Color.Transparent),
        startY = 0f,
        endY = heightPx,
    )

/**
 * Cheap "frosted" fill for in-content panels (cards, tiles) where a real backdrop blur
 * would be too expensive: a faint top-lit gradient + glass rim.
 */
@Composable
fun Modifier.glassPanel(
    shape: Shape,
    tint: Color = MaterialTheme.colorScheme.surfaceVariant,
    strength: Float = 1f,
): Modifier {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val top = if (isDark) 0.16f else 0.55f
    val bottom = if (isDark) 0.05f else 0.25f
    return this
        .background(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = top * strength).compositeOver(tint.copy(alpha = 0.30f)),
                    tint.copy(alpha = 0.18f + bottom * strength),
                ),
            ),
            shape = shape,
        )
        .glassBorder(shape = shape, isDark = isDark, accent = MaterialTheme.colorScheme.primary)
}

private fun Color.compositeOver(background: Color): Color {
    val a = this.alpha + background.alpha * (1f - this.alpha)
    if (a <= 0f) return Color.Transparent
    fun mix(fg: Float, bg: Float) =
        (fg * this.alpha + bg * background.alpha * (1f - this.alpha)) / a
    return Color(
        red = mix(this.red, background.red),
        green = mix(this.green, background.green),
        blue = mix(this.blue, background.blue),
        alpha = a,
    )
}
