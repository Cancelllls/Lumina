package com.cancellls.lumina.ui.glass

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cancellls.lumina.domain.GlassMode
import com.cancellls.lumina.ui.haptics.LuminaHaptic
import com.cancellls.lumina.ui.haptics.rememberLuminaHaptics
import com.cancellls.lumina.ui.theme.LocalGlassMode
import com.cancellls.lumina.ui.theme.LocalLuminaColors
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.blur.materials.HazeMaterials
import dev.chrisbanes.haze.glass.GlassStyle
import dev.chrisbanes.haze.glass.hazeGlass
import kotlin.math.cos
import kotlin.math.sin

object LuminaGlassMetrics {
    val TopBarContentHeight = 52.dp
    val TopControlSize = 44.dp
    val PageGutter = 16.dp
}

object ListIconCatalog {
    val iconKeys: List<String> = listOf(
        "home", "work", "shopping", "inbox",
        "health", "finance", "study", "travel",
        "star", "heart", "fitness", "code"
    )

    val colorHexes: List<Long> = listOf(
        0xFF14B8A6, // Teal
        0xFF3B82F6, // Cobalt
        0xFFFA2D48, // Apple Crimson
        0xFFF59E0B, // Amber
        0xFF8B5CF6, // Violet
        0xFF30D158, // Emerald
        0xFF06B6D4, // Cyan
        0xFFEC4899  // Rose
    )

    fun iconForKey(key: String): ImageVector = when (key) {
        "home" -> Icons.Rounded.Home
        "work" -> Icons.Rounded.Work
        "shopping" -> Icons.Rounded.ShoppingBag
        "inbox" -> Icons.Rounded.Inbox
        "health" -> Icons.Rounded.MedicalServices
        "finance" -> Icons.Rounded.AccountBalanceWallet
        "study" -> Icons.Rounded.School
        "travel" -> Icons.Rounded.Flight
        "star" -> Icons.Rounded.Star
        "heart" -> Icons.Rounded.Favorite
        "fitness" -> Icons.Rounded.FitnessCenter
        "code" -> Icons.Rounded.Code
        else -> Icons.AutoMirrored.Rounded.List
    }
}

@Composable
fun AmbientMeshBackdrop(modifier: Modifier = Modifier) {
    val colors = LocalLuminaColors.current
    val transition = rememberInfiniteTransition(label = "ambient_mesh")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 24000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "mesh_phase"
    )

    Canvas(
        modifier = modifier.background(colors.canvasBg)
    ) {
        val w = size.width
        val h = size.height

        val orb1Center = Offset(
            x = w * (0.20f + 0.14f * cos(phase)),
            y = h * (0.16f + 0.08f * sin(phase))
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(colors.orbPrimary, Color.Transparent),
                center = orb1Center,
                radius = w * 0.74f
            ),
            radius = w * 0.74f,
            center = orb1Center
        )

        val orb2Center = Offset(
            x = w * (0.84f + 0.10f * sin(phase + 1.4f)),
            y = h * (0.46f + 0.10f * cos(phase + 0.8f))
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(colors.orbSecondary, Color.Transparent),
                center = orb2Center,
                radius = w * 0.68f
            ),
            radius = w * 0.68f,
            center = orb2Center
        )

        val orb3Center = Offset(
            x = w * (0.36f + 0.14f * cos(phase + 2.7f)),
            y = h * (0.86f + 0.07f * sin(phase + 2.1f))
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(colors.orbTertiary, Color.Transparent),
                center = orb3Center,
                radius = w * 0.76f
            ),
            radius = w * 0.76f,
            center = orb3Center
        )
    }
}

private val TopFootFadeBrush = Brush.verticalGradient(
    colorStops = Array(9) { i ->
        val t = i / 8f
        val at = 0.58f + 0.42f * t
        at to Color.Black.copy(alpha = 1f - EaseInOut.transform(t))
    }
)

private val BottomHeadFadeBrush = Brush.verticalGradient(
    colorStops = Array(9) { i ->
        val t = i / 8f
        val at = 0.42f * t
        at to Color.Black.copy(alpha = EaseInOut.transform(t))
    }
)

/**
 * BitChord-inspired progressive Apple scroll-edge blur under the status bar
 * and floating top controls.
 */
@Composable
fun TopFadeBlur(
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    height: Dp = 76.dp
) {
    val glassMode = LocalGlassMode.current
    if (glassMode == GlassMode.REDUCED_BLUR) return

    val progressiveStyle = remember {
        HazeBlurStyle {
            blurRadius(24.dp)
            progressive(
                HazeProgressive.verticalGradient(
                    startIntensity = 1f,
                    endIntensity = 0f
                )
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(height)
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                drawRect(TopFootFadeBrush, blendMode = BlendMode.DstIn)
            }
            .hazeBlur(
                input = HazeInput.Sources(hazeState),
                style = progressiveStyle
            )
    )
}

/**
 * Bottom scroll-edge progressive blur so scrolling rows glide softly behind
 * the floating Dual-Island Liquid Glass Dock.
 */
@Composable
fun BottomFadeBlur(
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    height: Dp = 84.dp
) {
    val glassMode = LocalGlassMode.current
    if (glassMode == GlassMode.REDUCED_BLUR) return

    val progressiveStyle = remember {
        HazeBlurStyle {
            blurRadius(22.dp)
            progressive(
                HazeProgressive.verticalGradient(
                    startIntensity = 0f,
                    endIntensity = 1f
                )
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .height(height)
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                drawRect(BottomHeadFadeBrush, blendMode = BlendMode.DstIn)
            }
            .hazeBlur(
                input = HazeInput.Sources(hazeState),
                style = progressiveStyle
            )
    )
}

@Composable
fun Modifier.luminaGlassSurface(
    hazeState: HazeState,
    shape: RoundedCornerShape,
    accentTint: Color? = null,
    isChrome: Boolean = false
): Modifier {
    val colors = LocalLuminaColors.current
    val glassMode = LocalGlassMode.current

    val effectiveTint = when {
        accentTint != null -> accentTint.copy(alpha = if (colors.isDark) 0.22f else 0.16f)
        isChrome -> colors.chromeTint
        else -> colors.glassTint
    }

    val rimBrush = Brush.linearGradient(
        colors = listOf(colors.rimTopLeft, colors.rimBottomRight)
    )

    val elevationMod = if (isChrome) {
        Modifier.shadow(
            elevation = if (colors.isDark) 12.dp else 8.dp,
            shape = shape,
            ambientColor = Color.Black.copy(alpha = 0.35f),
            spotColor = Color.Black.copy(alpha = 0.35f)
        )
    } else {
        Modifier
    }

    val supportsRenderEffect = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    return when {
        glassMode == GlassMode.REDUCED_BLUR -> {
            this
                .then(elevationMod)
                .clip(shape)
                .background(
                    if (accentTint != null) {
                        accentTint.copy(alpha = if (colors.isDark) 0.20f else 0.15f)
                    } else {
                        colors.solidSurface
                    }
                )
                .border(0.8.dp, colors.subtleOuterBorder, shape)
        }
        glassMode == GlassMode.LIQUID_GLASS && supportsRenderEffect -> {
            val style = remember(shape, effectiveTint) {
                GlassStyle.regular.then {
                    shape(shape)
                    tint(effectiveTint)
                }
            }
            this
                .then(elevationMod)
                .clip(shape)
                .hazeGlass(
                    input = HazeInput.Sources(hazeState),
                    style = style
                )
                .border(0.8.dp, rimBrush, shape)
                .then(
                    if (!colors.isDark) {
                        Modifier.border(0.5.dp, colors.subtleOuterBorder, shape)
                    } else {
                        Modifier
                    }
                )
        }
        else -> {
            val frostStyle = if (isChrome) HazeMaterials.regular() else HazeMaterials.thin()
            this
                .then(elevationMod)
                .clip(shape)
                .hazeBlur(
                    input = HazeInput.Sources(hazeState),
                    style = frostStyle
                )
                .background(effectiveTint)
                .border(0.8.dp, rimBrush, shape)
                .then(
                    if (!colors.isDark) {
                        Modifier.border(0.5.dp, colors.subtleOuterBorder, shape)
                    } else {
                        Modifier
                    }
                )
        }
    }
}

@Composable
fun GlassCard(
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(24.dp),
    accentTint: Color? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val clickMod = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier

    Box(
        modifier = modifier
            .luminaGlassSurface(
                hazeState = hazeState,
                shape = shape,
                accentTint = accentTint,
                isChrome = false
            )
            .then(clickMod),
        content = content
    )
}

@Composable
fun GlassChromeBar(
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(50),
    accentTint: Color? = null,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier.luminaGlassSurface(
            hazeState = hazeState,
            shape = shape,
            accentTint = accentTint,
            isChrome = true
        ),
        content = content
    )
}

@Composable
fun GlassOrbButton(
    icon: ImageVector,
    contentDescription: String,
    hazeState: HazeState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = LuminaGlassMetrics.TopControlSize,
    tint: Color? = null,
    accentTint: Color? = null
) {
    val colors = LocalLuminaColors.current
    val haptics = rememberLuminaHaptics()
    Box(
        modifier = modifier
            .size(size)
            .luminaGlassSurface(
                hazeState = hazeState,
                shape = CircleShape,
                accentTint = accentTint,
                isChrome = true
            )
            .clickable {
                haptics.play(LuminaHaptic.Select)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint ?: colors.textPrimary,
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * BitChord's signature grouped 44dp floating multi-action pill.
 */
@Composable
fun GlassActionPill(
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    GlassChromeBar(
        hazeState = hazeState,
        shape = RoundedCornerShape(50),
        modifier = modifier.height(LuminaGlassMetrics.TopControlSize)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            content()
        }
    }
}

@Composable
fun GlassChip(
    label: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    tint: Color? = null,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val colors = LocalLuminaColors.current
    val haptics = rememberLuminaHaptics()
    val activeColor = tint ?: colors.accent
    val bgColor = when {
        selected -> activeColor.copy(alpha = if (colors.isDark) 0.26f else 0.16f)
        colors.isDark -> Color(0x1EFFFFFF)
        else -> Color(0x99FFFFFF)
    }
    val borderColor = when {
        selected -> activeColor.copy(alpha = 0.68f)
        colors.isDark -> Color(0x24FFFFFF)
        else -> colors.subtleOuterBorder
    }
    val contentColor = if (selected) activeColor else (tint ?: colors.textSecondary)

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(bgColor)
            .border(0.8.dp, borderColor, RoundedCornerShape(50))
            .then(
                if (onClick != null) {
                    Modifier.clickable {
                        haptics.play(LuminaHaptic.Tick)
                        onClick()
                    }
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(14.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            maxLines = 1
        )
    }
}

@Composable
fun GlassCompletionRing(
    isCompleted: Boolean,
    ringColor: Color,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp
) {
    val haptics = rememberLuminaHaptics()
    val fillAlpha by animateFloatAsState(
        targetValue = if (isCompleted) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "ring_fill"
    )
    val scale by animateFloatAsState(
        targetValue = if (isCompleted) 1.10f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "ring_scale"
    )
    val animatedBorder by animateColorAsState(
        targetValue = if (isCompleted) ringColor else ringColor.copy(alpha = 0.78f),
        label = "ring_border"
    )

    Box(
        modifier = modifier
            .size(size + 12.dp)
            .clip(CircleShape)
            .clickable {
                haptics.play(LuminaHaptic.Confirm)
                onToggle()
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .scale(scale)
                .clip(CircleShape)
                .background(ringColor.copy(alpha = fillAlpha))
                .border(2.dp, animatedBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "Completed",
                    tint = Color.White,
                    modifier = Modifier.size(size * 0.62f)
                )
            }
        }
    }
}
