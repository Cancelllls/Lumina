package com.cancellls.lumina.ui.glass

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cancellls.lumina.ui.theme.LocalLuminaColors
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import kotlin.math.cos
import kotlin.math.sin

object ListIconCatalog {
    val iconKeys: List<String> = listOf(
        "home", "work", "shopping", "inbox",
        "health", "finance", "study", "travel",
        "star", "heart", "fitness", "code"
    )

    val colorHexes: List<Long> = listOf(
        0xFF14B8A6, // Teal
        0xFF3B82F6, // Cobalt
        0xFFF59E0B, // Amber
        0xFFEC4899, // Rose
        0xFF8B5CF6, // Violet
        0xFF10B981, // Emerald
        0xFFEF4444, // Coral
        0xFF06B6D4  // Cyan
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
            animation = tween(durationMillis = 22000, easing = LinearEasing),
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
            x = w * (0.22f + 0.12f * cos(phase)),
            y = h * (0.18f + 0.08f * sin(phase))
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(colors.orbPrimary, Color.Transparent),
                center = orb1Center,
                radius = w * 0.72f
            ),
            radius = w * 0.72f,
            center = orb1Center
        )

        val orb2Center = Offset(
            x = w * (0.82f + 0.10f * sin(phase + 1.4f)),
            y = h * (0.44f + 0.10f * cos(phase + 0.8f))
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
            x = w * (0.35f + 0.14f * cos(phase + 2.7f)),
            y = h * (0.84f + 0.07f * sin(phase + 2.1f))
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(colors.orbTertiary, Color.Transparent),
                center = orb3Center,
                radius = w * 0.75f
            ),
            radius = w * 0.75f,
            center = orb3Center
        )
    }
}

@Composable
fun GlassCard(
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    accentTint: Color? = null,
    blurRadius: Dp = 20.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = LocalLuminaColors.current
    val topSurface = if (accentTint != null) {
        accentTint.copy(alpha = if (colors.isDark) 0.18f else 0.14f)
    } else {
        colors.glassSurfaceTop
    }
    val bottomSurface = if (accentTint != null) {
        colors.glassSurfaceBottom
    } else {
        colors.glassSurfaceBottom
    }
    val tintOverlay = accentTint?.copy(alpha = if (colors.isDark) 0.10f else 0.08f)
        ?: if (colors.isDark) Color(0x18FFFFFF) else Color(0x55FFFFFF)

    val clickMod = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier

    Box(
        modifier = modifier
            .clip(shape)
            .hazeEffect(
                state = hazeState,
                style = HazeStyle(
                    backgroundColor = colors.hazeFallbackBg,
                    tint = HazeTint(tintOverlay),
                    blurRadius = blurRadius,
                    noiseFactor = 0.08f
                )
            )
            .background(
                Brush.linearGradient(
                    colors = listOf(topSurface, bottomSurface)
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        colors.rimTopLeft,
                        colors.rimBottomRight
                    )
                ),
                shape = shape
            )
            .then(
                if (!colors.isDark) {
                    Modifier.border(0.5.dp, colors.subtleOuterBorder, shape)
                } else {
                    Modifier
                }
            )
            .then(clickMod),
        content = content
    )
}

@Composable
fun GlassChromeBar(
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(26.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val colors = LocalLuminaColors.current
    Box(
        modifier = modifier
            .clip(shape)
            .hazeEffect(
                state = hazeState,
                style = HazeStyle(
                    backgroundColor = colors.hazeFallbackBg,
                    tint = HazeTint(if (colors.isDark) Color(0x280B1324) else Color(0x88FFFFFF)),
                    blurRadius = 26.dp,
                    noiseFactor = 0.06f
                )
            )
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        colors.glassChromeSurface,
                        colors.glassSurfaceBottom
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(colors.rimTopLeft, colors.rimBottomRight)
                ),
                shape = shape
            )
            .then(
                if (!colors.isDark) {
                    Modifier.border(0.5.dp, colors.subtleOuterBorder, shape)
                } else {
                    Modifier
                }
            ),
        content = content
    )
}

@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color? = null,
    containerColor: Color? = null
) {
    val colors = LocalLuminaColors.current
    val bg = containerColor ?: if (colors.isDark) Color(0x22FFFFFF) else Color(0x80FFFFFF)
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(bg)
            .border(1.dp, colors.rimTopLeft.copy(alpha = 0.65f), CircleShape)
            .clickable(onClick = onClick),
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
    val activeColor = tint ?: colors.accent
    val bgColor = when {
        selected -> activeColor.copy(alpha = if (colors.isDark) 0.28f else 0.18f)
        colors.isDark -> Color(0x1EFFFFFF)
        else -> Color(0x99FFFFFF)
    }
    val borderColor = when {
        selected -> activeColor.copy(alpha = 0.75f)
        colors.isDark -> Color(0x2EFFFFFF)
        else -> colors.subtleOuterBorder
    }
    val contentColor = if (selected) activeColor else (tint ?: colors.textSecondary)

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(50))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
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
    val haptics = LocalHapticFeedback.current
    val fillAlpha by animateFloatAsState(
        targetValue = if (isCompleted) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "ring_fill"
    )
    val scale by animateFloatAsState(
        targetValue = if (isCompleted) 1.08f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "ring_scale"
    )
    val animatedBorder by animateColorAsState(
        targetValue = if (isCompleted) ringColor else ringColor.copy(alpha = 0.75f),
        label = "ring_border"
    )

    Box(
        modifier = modifier
            .size(size + 12.dp)
            .clip(CircleShape)
            .clickable {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
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
