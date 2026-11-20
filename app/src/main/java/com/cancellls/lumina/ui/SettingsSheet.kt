package com.cancellls.lumina.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cancellls.lumina.BuildConfig
import com.cancellls.lumina.data.AppSettings
import com.cancellls.lumina.domain.GlassMode
import com.cancellls.lumina.domain.GlassPalette
import com.cancellls.lumina.domain.ThemeMode
import com.cancellls.lumina.ui.glass.GlassChip
import com.cancellls.lumina.ui.theme.LocalLuminaColors

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsSheet(
    settings: AppSettings,
    hasNotificationPermission: Boolean,
    canScheduleExactAlarms: Boolean,
    onDismiss: () -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onGlassModeChange: (GlassMode) -> Unit,
    onPaletteChange: (GlassPalette) -> Unit,
    onDefaultSnoozeChange: (Int) -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
    onExportJson: () -> Unit,
    onImportJson: () -> Unit
) {
    val colors = LocalLuminaColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.sheetBg,
        contentColor = colors.textPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.textPrimary
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = colors.textSecondary
                    )
                }
            }

            // Glass Appearance
            Text(
                text = "Appearance",
                style = MaterialTheme.typography.labelLarge,
                color = colors.textSecondary
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThemeMode.entries.forEach { mode ->
                    GlassChip(
                        label = mode.label,
                        selected = settings.themeMode == mode,
                        onClick = { onThemeModeChange(mode) }
                    )
                }
            }

            Text(
                text = "Glass Fidelity (BitChord Engine)",
                style = MaterialTheme.typography.labelLarge,
                color = colors.textSecondary
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GlassMode.entries.forEach { mode ->
                    GlassChip(
                        label = mode.label,
                        selected = settings.glassMode == mode,
                        onClick = { onGlassModeChange(mode) }
                    )
                }
            }

            Text(
                text = "Liquid Glass Palette",
                style = MaterialTheme.typography.labelLarge,
                color = colors.textSecondary
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassPalette.entries.forEach { palette ->
                    val selected = settings.palette == palette
                    val primary = Color(palette.primaryHex)
                    val secondary = Color(palette.secondaryHex)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (selected) primary.copy(alpha = 0.16f)
                                else colors.solidSurface.copy(alpha = 0.65f)
                            )
                            .border(
                                width = 1.dp,
                                color = if (selected) primary else colors.rimBottomRight,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { onPaletteChange(palette) }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(primary, secondary)))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = palette.label,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                            color = colors.textPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        if (selected) {
                            Text(
                                text = "Active",
                                style = MaterialTheme.typography.labelMedium,
                                color = primary
                            )
                        }
                    }
                }
            }

            // Default Snooze
            Text(
                text = "Default Snooze Duration",
                style = MaterialTheme.typography.labelLarge,
                color = colors.textSecondary
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(10, 15, 30, 60).forEach { mins ->
                    GlassChip(
                        label = "${mins}m",
                        selected = settings.defaultSnoozeMinutes == mins,
                        onClick = { onDefaultSnoozeChange(mins) }
                    )
                }
            }

            HorizontalDivider(color = colors.rimBottomRight)

            // Permissions
            Text(
                text = "Alarm Reliability",
                style = MaterialTheme.typography.labelLarge,
                color = colors.textSecondary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenNotificationSettings,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.NotificationsActive,
                        contentDescription = null,
                        tint = if (hasNotificationPermission) colors.success else colors.warning,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (hasNotificationPermission) "Alerts On" else "Allow Alerts")
                }

                OutlinedButton(
                    onClick = onOpenExactAlarmSettings,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Alarm,
                        contentDescription = null,
                        tint = if (canScheduleExactAlarms) colors.success else colors.warning,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (canScheduleExactAlarms) "Exact On" else "Allow Exact")
                }
            }

            HorizontalDivider(color = colors.rimBottomRight)

            // Local Backup & Restore
            Text(
                text = "Offline Backup (JSON)",
                style = MaterialTheme.typography.labelLarge,
                color = colors.textSecondary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onExportJson,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Upload,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export")
                }
                OutlinedButton(
                    onClick = onImportJson,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Download,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Import")
                }
            }

            Text(
                text = "Lumina v${BuildConfig.VERSION_NAME} • 100% Local-First • GPL-3.0 FOSS",
                style = MaterialTheme.typography.labelMedium,
                color = colors.textTertiary,
                modifier = Modifier
                    .padding(top = 6.dp, bottom = 16.dp)
                    .align(Alignment.CenterHorizontally)
            )
        }
    }
}
