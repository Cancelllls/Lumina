package com.cancellls.lumina.ui

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.PriorityHigh
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.cancellls.lumina.domain.ParsedReminderInput
import com.cancellls.lumina.domain.Priority
import com.cancellls.lumina.domain.RecurrenceEngine
import com.cancellls.lumina.ui.glass.GlassChip
import com.cancellls.lumina.ui.glass.GlassChromeBar
import com.cancellls.lumina.ui.glass.GlassIconButton
import com.cancellls.lumina.ui.theme.LocalLuminaColors
import dev.chrisbanes.haze.HazeState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickAddDock(
    text: String,
    parsed: ParsedReminderInput,
    chromeHazeState: HazeState,
    onTextChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onOpenFullEditor: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalLuminaColors.current
    val hasDetectedMeta = text.isNotBlank() && (
        parsed.dueAtMillis != null ||
            parsed.recurrenceRule.isNotEmpty() ||
            parsed.priority != Priority.NONE ||
            parsed.isFlagged
        )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AnimatedVisibility(visible = hasDetectedMeta) {
            GlassChromeBar(
                hazeState = chromeHazeState,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (parsed.dueAtMillis != null) {
                        GlassChip(
                            label = UiFormatters.formatDueChip(parsed.dueAtMillis, parsed.hasTime),
                            icon = Icons.Rounded.Schedule,
                            tint = colors.accent,
                            selected = true
                        )
                    }
                    if (parsed.recurrenceRule.isNotEmpty()) {
                        GlassChip(
                            label = RecurrenceEngine.formatLabel(parsed.recurrenceRule),
                            icon = Icons.Rounded.Repeat,
                            tint = colors.accentSecondary,
                            selected = true
                        )
                    }
                    if (parsed.priority != Priority.NONE) {
                        val pColor = when (parsed.priority) {
                            Priority.HIGH -> colors.danger
                            Priority.MEDIUM -> colors.warning
                            Priority.LOW -> colors.accent
                            Priority.NONE -> colors.textSecondary
                        }
                        GlassChip(
                            label = parsed.priority.label,
                            icon = Icons.Rounded.PriorityHigh,
                            tint = pColor,
                            selected = true
                        )
                    }
                    if (parsed.isFlagged) {
                        GlassChip(
                            label = "Flagged",
                            icon = Icons.Rounded.Flag,
                            tint = colors.warning,
                            selected = true
                        )
                    }
                }
            }
        }

        GlassChromeBar(
            hazeState = chromeHazeState,
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassIconButton(
                    icon = Icons.Rounded.Tune,
                    contentDescription = "Detailed reminder editor",
                    onClick = onOpenFullEditor
                )

                Spacer(modifier = Modifier.width(10.dp))

                Box(modifier = Modifier.weight(1f)) {
                    if (text.isEmpty()) {
                        Text(
                            text = "Remind me... e.g. Pay rent tomorrow 9am",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textTertiary,
                            maxLines = 1
                        )
                    }
                    BasicTextField(
                        value = text,
                        onValueChange = onTextChange,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = colors.textPrimary
                        ),
                        cursorBrush = SolidColor(colors.accent),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { onSubmit() }),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                val canSend = text.isNotBlank()
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (canSend) {
                                Brush.linearGradient(
                                    colors = listOf(colors.accent, colors.accentSecondary)
                                )
                            } else {
                                Brush.linearGradient(
                                    colors = listOf(
                                        colors.accent.copy(alpha = 0.28f),
                                        colors.accentSecondary.copy(alpha = 0.24f)
                                    )
                                )
                            }
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.45f), CircleShape)
                        .clickable {
                            if (canSend) onSubmit() else onOpenFullEditor()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (canSend) Icons.AutoMirrored.Rounded.Send else Icons.Rounded.Add,
                        contentDescription = if (canSend) "Add reminder" else "New reminder",
                        tint = Color.White,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }
    }
}
