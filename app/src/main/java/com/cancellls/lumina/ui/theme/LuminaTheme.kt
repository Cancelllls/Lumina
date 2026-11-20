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
    val glassSurfaceTop: Color,
    val glassSurfaceBottom: Color,
    val glassChromeSurface: Color,
    val hazeFallbackBg: Color,
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

fun buildLuminaColors(isDark: Boolean, palette: GlassPalette): LuminaColors {
    val accent = Color(palette.primaryHex)
    val secondary = Color(palette.secondaryHex)
    val tertiary = when (palette) {
        GlassPalette.AURORA_TEAL -> Color(0xFF818CF8)
        GlassPalette.COBALT_GLASS -> Color(0xFF2DD4BF)
        GlassPalette.AMBER_DUSK -> Color(0xFF6366F1)
        GlassPalette.EMERALD_MIST -> Color(0xFF38BDF8)
    }

    return if (isDark) {
        LuminaColors(
            isDark = true,
            canvasBg = Color(0xFF070B14),
            orbPrimary = accent.copy(alpha = 0.28f),
            orbSecondary = secondary.copy(alpha = 0.24f),
            orbTertiary = tertiary.copy(alpha = 0.20f),
            accent = accent,
            accentSecondary = secondary,
            glassSurfaceTop = Color(0xA6131D30),
            glassSurfaceBottom = Color(0x880B1221),
            glassChromeSurface = Color(0xBF0B1324),
            hazeFallbackBg = Color(0xFF0B1324),
            rimTopLeft = Color(0x57FFFFFF),
            rimBottomRight = Color(0x14FFFFFF),
            subtleOuterBorder = Color(0x1AFFFFFF),
            textPrimary = Color(0xFFF8FAFC),
            textSecondary = Color(0xFFB8C4D6),
            textTertiary = Color(0xFF7B8AA3),
            danger = Color(0xFFFB7185),
            success = Color(0xFF34D399),
            warning = Color(0xFFFBBF24),
            sheetBg = Color(0xF50E1627)
        )
    } else {
        LuminaColors(
            isDark = false,
            canvasBg = Color(0xFFEFF4FA),
            orbPrimary = accent.copy(alpha = 0.22f),
            orbSecondary = secondary.copy(alpha = 0.20f),
            orbTertiary = tertiary.copy(alpha = 0.16f),
            accent = if (palette == GlassPalette.AMBER_DUSK) Color(0xFFD97706) else Color(palette.primaryHex),
            accentSecondary = secondary,
            glassSurfaceTop = Color(0xCCFFFFFF),
            glassSurfaceBottom = Color(0xA6F8FAFC),
            glassChromeSurface = Color(0xD9FFFFFF),
            hazeFallbackBg = Color(0xFFF1F5F9),
            rimTopLeft = Color(0xFFFFFFFF),
            rimBottomRight = Color(0x4D94A3B8),
            subtleOuterBorder = Color(0x1F0F172A),
            textPrimary = Color(0xFF0F172A),
            textSecondary = Color(0xFF475569),
            textTertiary = Color(0xFF64748B),
            danger = Color(0xFFDC2626),
            success = Color(0xFF059669),
            warning = Color(0xFFD97706),
            sheetBg = Color(0xFAF8FAFC)
        )
    }
}

private val LuminaTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.2).sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )
)

@Composable
fun LuminaTheme(
    themeMode: ThemeMode,
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
            surface = luminaColors.sheetBg,
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
            surface = luminaColors.sheetBg,
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

    CompositionLocalProvider(LocalLuminaColors provides luminaColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = LuminaTypography,
            content = content
        )
    }
}
