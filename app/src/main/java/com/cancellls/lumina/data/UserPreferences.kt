package com.cancellls.lumina.data

import android.content.Context
import android.content.SharedPreferences
import com.cancellls.lumina.domain.GlassPalette
import com.cancellls.lumina.domain.SortOrder
import com.cancellls.lumina.domain.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val palette: GlassPalette = GlassPalette.AURORA_TEAL,
    val defaultSnoozeMinutes: Int = 15,
    val sortOrder: SortOrder = SortOrder.DUE_DATE,
    val showCompletedInLists: Boolean = false,
    val dismissedPermissionBanner: Boolean = false
)

class UserPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(readInitial())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private fun readInitial(): AppSettings {
        return AppSettings(
            themeMode = prefs.getString(KEY_THEME, ThemeMode.SYSTEM.name)
                ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            palette = prefs.getString(KEY_PALETTE, GlassPalette.AURORA_TEAL.name)
                ?.let { runCatching { GlassPalette.valueOf(it) }.getOrNull() }
                ?: GlassPalette.AURORA_TEAL,
            defaultSnoozeMinutes = prefs.getInt(KEY_SNOOZE_MINUTES, 15).coerceIn(5, 180),
            sortOrder = prefs.getString(KEY_SORT_ORDER, SortOrder.DUE_DATE.name)
                ?.let { runCatching { SortOrder.valueOf(it) }.getOrNull() }
                ?: SortOrder.DUE_DATE,
            showCompletedInLists = prefs.getBoolean(KEY_SHOW_COMPLETED, false),
            dismissedPermissionBanner = prefs.getBoolean(KEY_DISMISSED_BANNER, false)
        )
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME, mode.name).apply()
        _settings.update { it.copy(themeMode = mode) }
    }

    fun setPalette(palette: GlassPalette) {
        prefs.edit().putString(KEY_PALETTE, palette.name).apply()
        _settings.update { it.copy(palette = palette) }
    }

    fun setDefaultSnoozeMinutes(minutes: Int) {
        val clamped = minutes.coerceIn(5, 180)
        prefs.edit().putInt(KEY_SNOOZE_MINUTES, clamped).apply()
        _settings.update { it.copy(defaultSnoozeMinutes = clamped) }
    }

    fun setSortOrder(sortOrder: SortOrder) {
        prefs.edit().putString(KEY_SORT_ORDER, sortOrder.name).apply()
        _settings.update { it.copy(sortOrder = sortOrder) }
    }

    fun setShowCompletedInLists(show: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_COMPLETED, show).apply()
        _settings.update { it.copy(showCompletedInLists = show) }
    }

    fun dismissPermissionBanner() {
        prefs.edit().putBoolean(KEY_DISMISSED_BANNER, true).apply()
        _settings.update { it.copy(dismissedPermissionBanner = true) }
    }

    companion object {
        private const val PREFS_NAME = "lumina_prefs"
        private const val KEY_THEME = "theme_mode"
        private const val KEY_PALETTE = "glass_palette"
        private const val KEY_SNOOZE_MINUTES = "default_snooze_minutes"
        private const val KEY_SORT_ORDER = "sort_order"
        private const val KEY_SHOW_COMPLETED = "show_completed_in_lists"
        private const val KEY_DISMISSED_BANNER = "dismissed_permission_banner"
    }
}
