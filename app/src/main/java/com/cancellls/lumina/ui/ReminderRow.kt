package com.cancellls.lumina.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.OutlinedFlag
import androidx.compose.material.icons.rounded.PriorityHigh
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Snooze
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cancellls.lumina.domain.Priority
import com.cancellls.lumina.domain.RecurrenceEngine
import com.cancellls.lumina.domain.ReminderItem
import com.cancellls.lumina.domain.ReminderListItem
import com.cancellls.lumina.ui.glass.GlassChip
import com.cancellls.lumina.ui.glass.GlassCompletionRing
import com.cancellls.lumina.ui.glass.luminaGlassSurface
import com.cancellls.lumina.ui.haptics.LuminaHaptic
import com.cancellls.lumina.ui.haptics.rememberLuminaHaptics
import com.cancellls.lumina.ui.theme.LocalLuminaColors
import dev.chrisbanes.haze.HazeState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun ReminderRow(
    item: ReminderItem,
    listMeta: ReminderListItem?,
    hazeState: HazeState,
    onToggleComplete: () -> Unit,
    onToggleFlag: () -> Unit,
    onToggleSubtask: (Long) -> Unit,
    onSnooze: (Int) -> Unit,
    onDuplicate: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalLuminaColors.current
    val haptics = rememberLuminaHaptics()
    var subtasksExpanded by rememberSaveable(item.id) { mutableStateOf(false) }
    var contextMenuOpen by remember { mutableStateOf(false) }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    haptics.play(LuminaHaptic.Confirm)
                    onToggleComplete()
                    false
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    haptics.play(LuminaHaptic.Heavy)
                    onDelete()
                    true
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        }
    )

    val ringColor = when {
        item.priority == Priority.HIGH -> colors.danger
        item.priority == Priority.MEDIUM -> colors.warning
        listMeta != null -> Color(listMeta.colorHex)
        else -> colors.accent
    }

    val now = System.currentTimeMillis()
    val isOverdue = UiFormatters.isOverdue(item, now)

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val bgColor = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> colors.success.copy(alpha = 0.28f)
                SwipeToDismissBoxValue.EndToStart -> colors.danger.copy(alpha = 0.28f)
                SwipeToDismissBoxValue.Settled -> Color.Transparent
            }
            val alignment = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                else -> Alignment.CenterEnd
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(22.dp))
                    .background(bgColor)
                    .padding(horizontal = 20.dp),
                contentAlignment = alignment
            ) {
                when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircleOutline,
                            contentDescription = "Complete",
                            tint = colors.success,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    SwipeToDismissBoxValue.EndToStart -> {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "Delete",
                            tint = colors.danger,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    SwipeToDismissBoxValue.Settled -> Unit
                }
            }
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .luminaGlassSurface(
                    hazeState = hazeState,
                    shape = RoundedCornerShape(22.dp),
                    isChrome = false
                )
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = {
                        haptics.play(LuminaHaptic.Heavy)
                        contextMenuOpen = true
                    }
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    GlassCompletionRing(
                        isCompleted = item.isCompleted,
                        ringColor = ringColor,
                        onToggle = onToggleComplete
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                            ),
                            color = if (item.isCompleted) colors.textTertiary else colors.textPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (item.notes.isNotBlank()) {
                            Text(
                                text = item.notes,
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        val effectiveDue = item.effectiveDueMillis
                        val hasMeta = effectiveDue != null ||
                            item.recurrenceRule.isNotEmpty() ||
                            item.priority != Priority.NONE ||
                            item.subtasks.isNotEmpty() ||
                            listMeta != null

                        if (hasMeta) {
                            FlowRow(
                                modifier = Modifier.padding(top = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (effectiveDue != null) {
                                    val dueLabel = UiFormatters.formatDueChip(
                                        dueMillis = effectiveDue,
                                        hasTime = item.hasTime,
                                        isSnoozed = item.snoozedUntilMillis != null
                                    )
                                    val dueTint = if (isOverdue) colors.danger else colors.accent
                                    GlassChip(
                                        label = dueLabel,
                                        icon = Icons.Rounded.Schedule,
                                        tint = dueTint,
                                        selected = isOverdue
                                    )
                                }

                                if (item.recurrenceRule.isNotEmpty()) {
                                    GlassChip(
                                        label = RecurrenceEngine.formatLabel(item.recurrenceRule),
                                        icon = Icons.Rounded.Repeat,
                                        tint = colors.accentSecondary
                                    )
                                }

                                if (item.priority != Priority.NONE) {
                                    val pColor = when (item.priority) {
                                        Priority.HIGH -> colors.danger
                                        Priority.MEDIUM -> colors.warning
                                        Priority.LOW -> colors.accent
                                        Priority.NONE -> colors.textSecondary
                                    }
                                    GlassChip(
                                        label = item.priority.label,
                                        icon = Icons.Rounded.PriorityHigh,
                                        tint = pColor
                                    )
                                }

                                if (item.subtasks.isNotEmpty()) {
                                    val doneCount = item.subtasks.count { it.isCompleted }
                                    GlassChip(
                                        label = "$doneCount/${item.subtasks.size}",
                                        icon = if (subtasksExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                        tint = colors.textSecondary,
                                        onClick = { subtasksExpanded = !subtasksExpanded }
                                    )
                                }

                                if (listMeta != null) {
                                    GlassChip(
                                        label = listMeta.name,
                                        tint = Color(listMeta.colorHex)
                                    )
                                }
                            }
                        }
                    }

                    IconButton(
                        onClick = {
                            haptics.play(LuminaHaptic.Tick)
                            onToggleFlag()
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (item.isFlagged) Icons.Rounded.Flag else Icons.Rounded.OutlinedFlag,
                            contentDescription = "Toggle flag",
                            tint = if (item.isFlagged) colors.warning else colors.textTertiary,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }

                AnimatedVisibility(visible = subtasksExpanded && item.subtasks.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 42.dp, top = 8.dp, end = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = colors.rimBottomRight
                        )
                        item.subtasks.forEach { sub ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        haptics.play(LuminaHaptic.Tick)
                                        onToggleSubtask(sub.id)
                                    }
                                    .padding(vertical = 4.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (sub.isCompleted) colors.accent else Color.Transparent
                                        )
                                        .border(
                                            1.5.dp,
                                            if (sub.isCompleted) colors.accent else colors.textTertiary,
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (sub.isCompleted) {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = sub.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        textDecoration = if (sub.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = if (sub.isCompleted) colors.textTertiary else colors.textPrimary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            DropdownMenu(
                expanded = contextMenuOpen,
                onDismissRequest = { contextMenuOpen = false }
            ) {
                DropdownMenuItem(
                    text = { Text(if (item.isCompleted) "Mark Incomplete" else "Complete") },
                    leadingIcon = {
                        Icon(Icons.Rounded.CheckCircleOutline, contentDescription = null)
                    },
                    onClick = {
                        contextMenuOpen = false
                        onToggleComplete()
                    }
                )
                DropdownMenuItem(
                    text = { Text(if (item.isFlagged) "Remove Flag" else "Flag") },
                    leadingIcon = {
                        Icon(Icons.Rounded.Flag, contentDescription = null)
                    },
                    onClick = {
                        contextMenuOpen = false
                        onToggleFlag()
                    }
                )
                if (!item.isCompleted) {
                    DropdownMenuItem(
                        text = { Text("Snooze 15m") },
                        leadingIcon = {
                            Icon(Icons.Rounded.Snooze, contentDescription = null)
                        },
                        onClick = {
                            contextMenuOpen = false
                            onSnooze(15)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Snooze 1h") },
                        leadingIcon = {
                            Icon(Icons.Rounded.Schedule, contentDescription = null)
                        },
                        onClick = {
                            contextMenuOpen = false
                            onSnooze(60)
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text("Duplicate") },
                    leadingIcon = {
                        Icon(Icons.Rounded.ContentCopy, contentDescription = null)
                    },
                    onClick = {
                        contextMenuOpen = false
                        onDuplicate()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Share") },
                    leadingIcon = {
                        Icon(Icons.Rounded.Share, contentDescription = null)
                    },
                    onClick = {
                        contextMenuOpen = false
                        onShare()
                    }
                )
                HorizontalDivider(thickness = 0.5.dp)
                DropdownMenuItem(
                    text = { Text("Delete", color = colors.danger) },
                    leadingIcon = {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = null, tint = colors.danger)
                    },
                    onClick = {
                        contextMenuOpen = false
                        onDelete()
                    }
                )
            }
        }
    }
}
