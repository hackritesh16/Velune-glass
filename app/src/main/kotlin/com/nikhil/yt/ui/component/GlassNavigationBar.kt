package com.nikhil.yt.ui.component

import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import com.kyant.backdrop.backdrops.rememberCanvasBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.nikhil.yt.constants.NavigationBarHeight
import com.nikhil.yt.constants.SlimNavBarHeight
import com.nikhil.yt.ui.screens.Screens

@Composable
fun GlassNavigationBar(
    navigationItems: List<Screens>,
    currentRoute: String?,
    isRouteSelected: (Screens) -> Boolean,
    onItemClick: (Screens) -> Unit,
    slimNav: Boolean,
    pureBlack: Boolean,
    bottomInset: Dp,
    bottomPadding: Dp,
    slideOffset: Dp,
    modifier: Modifier = Modifier,
) {
    val isDark = pureBlack || MaterialTheme.colorScheme.background.luminance() < 0.5f
    val glassStyle = GlassEffectDefaults.navigationBarStyle(isDark, pureBlack)
    val navBarHeight = if (slimNav) SlimNavBarHeight else NavigationBarHeight
    val totalHeight = bottomInset + bottomPadding + navBarHeight
    val glassShape = RoundedCornerShape(28.dp)

    val supportsBackdrop = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val supportsLens = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    val backdrop = rememberCanvasBackdrop {
        drawRect(
            color = glassStyle.backgroundDimColor.copy(alpha = glassStyle.backgroundDimAlpha),
            size = size
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(totalHeight)
            .offset { IntOffset(0, slideOffset.roundToPx()) }
    ) {
        Box(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 12.dp, end = 12.dp, bottom = bottomInset + bottomPadding)
                    .fillMaxWidth()
                    .height(navBarHeight)
                    .clip(glassShape)
                    .then(
                        if (supportsBackdrop) {
                            Modifier.drawBackdrop(
                                backdrop = backdrop,
                                shape = { glassShape },
                                effects = {
                                    if (glassStyle.useVibrancy) vibrancy()
                                    blur(with(density) { glassStyle.blurRadius.toPx() })
                                    if (supportsLens && glassStyle.useLens) {
                                        lens(
                                            with(density) { glassStyle.lensHeight.toPx() },
                                            with(density) { glassStyle.lensAmount.toPx() }
                                        )
                                    }
                                },
                                onDrawSurface = {
                                    drawRect(glassStyle.surfaceTint.copy(alpha = glassStyle.surfaceAlpha))
                                    drawRect(glassStyle.overlayColor.copy(alpha = glassStyle.overlayAlpha))
                                    drawRect(
                                        brush = glassSheenBrush(
                                            alpha = glassStyle.sheenAlpha,
                                            heightPx = size.height * 0.7f,
                                        ),
                                    )
                                }
                            )
                        } else {
                            Modifier.background(
                                if (pureBlack) Color.Black
                                else MaterialTheme.colorScheme.surfaceContainer
                            )
                        }
                    )
                    .glassBorder(
                        shape = glassShape,
                        isDark = isDark,
                        accent = MaterialTheme.colorScheme.primary,
                    )
        ) {
            GlassNavigationBarContent(
                navigationItems = navigationItems,
                isRouteSelected = isRouteSelected,
                onItemClick = onItemClick,
                slimNav = slimNav,
                pureBlack = pureBlack,
                isDark = isDark,
                modifier = Modifier.fillMaxWidth().height(navBarHeight),
            )
        }
    }
}

@Composable
private fun GlassNavigationBarContent(
    navigationItems: List<Screens>,
    isRouteSelected: (Screens) -> Boolean,
    onItemClick: (Screens) -> Unit,
    slimNav: Boolean,
    pureBlack: Boolean,
    isDark: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        navigationItems.fastForEach { screen ->
            val isSelected = isRouteSelected(screen)

            GlassNavigationBarItem(
                selected = isSelected,
                onClick = { onItemClick(screen) },
                isDark = isDark,
                icon = {
                    Icon(
                        painter = painterResource(
                            id = if (isSelected) screen.iconIdActive else screen.iconIdInactive
                        ),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = if (isSelected) {
                            if (pureBlack) Color.White
                            else MaterialTheme.colorScheme.primary
                        } else {
                            if (pureBlack) Color.White.copy(alpha = 0.5f)
                            else if (isDark) Color.White.copy(alpha = 0.6f)
                            else Color.Black.copy(alpha = 0.5f)
                        }
                    )
                },
                label = if (!slimNav) {
                    {
                        Text(
                            text = stringResource(screen.titleId),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 11.sp,
                            color = if (isSelected) {
                                if (pureBlack) Color.White
                                else MaterialTheme.colorScheme.primary
                            } else {
                                if (pureBlack) Color.White.copy(alpha = 0.5f)
                                else if (isDark) Color.White.copy(alpha = 0.6f)
                                else Color.Black.copy(alpha = 0.5f)
                            }
                        )
                    }
                } else null
            )
        }
    }
}

@Composable
private fun RowScope.GlassNavigationBarItem(
    selected: Boolean,
    onClick: () -> Unit,
    isDark: Boolean,
    icon: @Composable () -> Unit,
    label: (@Composable () -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val selectedIndicatorAlpha by animateFloatAsState(
        targetValue = if (selected) 0.26f else 0f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 380f),
        label = "indicatorAlpha"
    )
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.08f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "iconScale"
    )
    val accent = MaterialTheme.colorScheme.primary

    Column(
        modifier = modifier
            .weight(1f)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Selected pill is a frosted, accent-tinted glass chip instead of a flat white/black block.
        val indicatorColor = if (isDark) Color.White else Color.Black

        Box(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = iconScale
                    scaleY = iconScale
                }
                .clip(RoundedCornerShape(18.dp))
                .drawBehind {
                    if (selectedIndicatorAlpha > 0f) {
                        val radius = androidx.compose.ui.geometry.CornerRadius(18.dp.toPx())
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    accent.copy(alpha = selectedIndicatorAlpha),
                                    indicatorColor.copy(alpha = selectedIndicatorAlpha * 0.35f),
                                ),
                            ),
                            cornerRadius = radius,
                        )
                        drawRoundRect(
                            color = Color.White.copy(alpha = selectedIndicatorAlpha * 1.2f),
                            cornerRadius = radius,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 0.75.dp.toPx()),
                        )
                    }
                }
                .padding(horizontal = 18.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            icon()
        }

        if (label != null) {
            Spacer(modifier = Modifier.height(2.dp))
            label()
        }
    }
}
