package com.cancellls.lumina.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.cancellls.lumina.domain.GlassMode
import com.cancellls.lumina.domain.GlassPalette
import com.cancellls.lumina.domain.ThemeMode

@Immutable
data class LuminaColors(
    val isDark: Boolean,
    val canvasBg: Color,
    val orbPrimary: Color,
    val orbSecondary: Color,
    val orbTertiary: Color,
    val accent: Color,
    val accentSecondary: Color,
    val glassTint: Color,
    val chromeTint: Color,
    val solidSurface: Color,
    val rimTopLeft: Color,
    val rimBottomRight: Color,
    val subtleOuterBorder: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val danger: Color,
    val success: Color,
    val warning: Color,
    val sheetBg: Color
)

val LocalLuminaColors = staticCompositionLocalOf {
    buildLuminaColors(isDark = true, palette = GlassPalette.AURORA_TEAL)
}

val LocalGlassMode = staticCompositionLocalOf { GlassMode.LIQUID_GLASS }

fun buildLuminaColors(isDark: Boolean, palette: GlassPalette): LuminaColors {
    val accent = Color(palette.primaryHex)
    val secondary = Color(palette.secondaryHex)
    val tertiary = when (palette) {
        GlassPalette.AURORA_TEAL -> Color(0xFF818CF8)
        GlassPalette.COBALT_GLASS -> Color(0xFF2DD4BF)
        GlassPalette.AMBER_DUSK -> Color(0xFFFB7185)
        GlassPalette.EMERALD_MIST -> Color(0xFF38BDF8)
    }

    return if (isDark) {
        LuminaColors(
            isDark = true,
            // Pitch-OLED dark canvas inspired by BitChord's Apple-style dark surface
            canvasBg = Color(0xFF000000),
            orbPrimary = accent.copy(alpha = 0.24f),
            orbSecondary = secondary.copy(alpha = 0.20f),
            orbTertiary = tertiary.copy(alpha = 0.16f),
            accent = accent,
            accentSecondary = secondary,
            glassTint = Color(0x66121216),
            chromeTint = Color(0x78141418),
            solidSurface = Color(0xFF141418),
            rimTopLeft = Color(0x42FFFFFF),
            rimBottomRight = Color(0x14FFFFFF),
            subtleOuterBorder = Color(0x26FFFFFF),
            textPrimary = Color(0xFFFFFFFF),
            textSecondary = Color(0xFFB4B4BD),
            textTertiary = Color(0xFF7D7D87),
            danger = Color(0xFFFA2D48),
            success = Color(0xFF30D158),
            warning = Color(0xFFFF9F0A),
            sheetBg = Color(0xF5121216)
        )
    } else {
        LuminaColors(
            isDark = false,
            canvasBg = Color(0xFFF6F6F9),
            orbPrimary = accent.copy(alpha = 0.20f),
            orbSecondary = secondary.copy(alpha = 0.18f),
            orbTertiary = tertiary.copy(alpha = 0.14f),
            accent = if (palette == GlassPalette.AMBER_DUSK) Color(0xFFE11D48) else Color(palette.primaryHex),
            accentSecondary = secondary,
            glassTint = Color(0xA6FFFFFF),
            chromeTint = Color(0xB8FAFAFC),
            solidSurface = Color(0xFFFFFFFF),
            rimTopLeft = Color(0xFFFFFFFF),
            rimBottomRight = Color(0x3D94A3B8),
            subtleOuterBorder = Color(0x14000000),
            textPrimary = Color(0xFF09090B),
            textSecondary = Color(0xFF52525B),
            textTertiary = Color(0xFF71717A),
            danger = Color(0xFFE11D48),
            success = Color(0xFF059669),
            warning = Color(0xFFD97706),
            sheetBg = Color(0xFAFFFFFF)
        )
    }
}

// Apple / BitChord tight display tracking & open small-text spacing
private val LuminaTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.3).sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 21.sp,
        letterSpacing = (-0.1).sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.15.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 19.sp,
        letterSpacing = 0.15.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.2.sp
    )
)

@Composable
fun LuminaTheme(
    themeMode: ThemeMode,
    glassMode: GlassMode,
    palette: GlassPalette,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }

    val luminaColors = buildLuminaColors(isDark = isDark, palette = palette)
    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = luminaColors.accent,
            secondary = luminaColors.accentSecondary,
            background = luminaColors.canvasBg,
            surface = luminaColors.solidSurface,
            surfaceVariant = Color(0xFF1C1C1E),
            outline = Color(0xFF2C2C2E),
            onPrimary = Color.White,
            onBackground = luminaColors.textPrimary,
            onSurface = luminaColors.textPrimary,
            error = luminaColors.danger
        )
    } else {
        lightColorScheme(
            primary = luminaColors.accent,
            secondary = luminaColors.accentSecondary,
            background = luminaColors.canvasBg,
            surface = luminaColors.solidSurface,
            surfaceVariant = Color(0xFFF2F2F7),
            outline = Color(0xFFE5E5EA),
            onPrimary = Color.White,
            onBackground = luminaColors.textPrimary,
            onSurface = luminaColors.textPrimary,
            error = luminaColors.danger
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !isDark
            insetsController.isAppearanceLightNavigationBars = !isDark
        }
    }

    CompositionLocalProvider(
        LocalLuminaColors provides luminaColors,
        LocalGlassMode provides glassMode
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = LuminaTypography,
            content = content
        )
    }
}
