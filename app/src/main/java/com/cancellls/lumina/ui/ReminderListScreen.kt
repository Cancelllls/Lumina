package com.cancellls.lumina.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cancellls.lumina.domain.ReminderItem
import com.cancellls.lumina.domain.SmartListFilter
import com.cancellls.lumina.domain.SmartListType
import com.cancellls.lumina.domain.SortOrder
import com.cancellls.lumina.ui.glass.GlassCard
import com.cancellls.lumina.ui.glass.GlassChromeBar
import com.cancellls.lumina.ui.glass.GlassOrbButton
import com.cancellls.lumina.ui.glass.ListIconCatalog
import com.cancellls.lumina.ui.glass.TopFadeBlur
import com.cancellls.lumina.ui.theme.LocalLuminaColors
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.glass.hazeGlassContainer
import dev.chrisbanes.haze.hazeSource

@Composable
fun ReminderListScreen(
    screen: LuminaScreen,
    uiState: MainUiState,
    meshHazeState: HazeState,
    chromeHazeState: HazeState,
    onBack: () -> Unit,
    onSortOrderChange: (SortOrder) -> Unit,
    onToggleShowCompleted: (Boolean) -> Unit,
    onClearCompleted: () -> Unit,
    onToggleComplete: (Long) -> Unit,
    onToggleFlag: (Long) -> Unit,
    onToggleSubtask: (Long) -> Unit,
    onDeleteReminder: (Long) -> Unit,
    onOpenReminder: (ReminderItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalLuminaColors.current
    val listById = remember(uiState.lists) { uiState.lists.associateBy { it.id } }
    val now = System.currentTimeMillis()

    val customList = (screen as? LuminaScreen.CustomList)?.let { listById[it.listId] }

    val screenTitle = when (screen) {
        is LuminaScreen.SmartList -> screen.type.title
        is LuminaScreen.CustomList -> customList?.name ?: "List"
        LuminaScreen.Home -> ""
    }

    val screenIcon = when (screen) {
        is LuminaScreen.SmartList -> when (screen.type) {
            SmartListType.TODAY -> Icons.Rounded.Today
            SmartListType.SCHEDULED -> Icons.Rounded.CalendarMonth
            SmartListType.FLAGGED -> Icons.Rounded.Flag
            SmartListType.ALL -> Icons.Rounded.Inbox
            SmartListType.COMPLETED -> Icons.Rounded.DoneAll
        }
        is LuminaScreen.CustomList -> ListIconCatalog.iconForKey(customList?.iconKey ?: "inbox")
        LuminaScreen.Home -> Icons.Rounded.Inbox
    }

    val screenAccent = when (screen) {
        is LuminaScreen.SmartList -> when (screen.type) {
            SmartListType.TODAY -> Color(0xFF38BDF8)
            SmartListType.SCHEDULED -> Color(0xFFFA2D48)
            SmartListType.FLAGGED -> Color(0xFFFF9F0A)
            SmartListType.ALL -> colors.accent
            SmartListType.COMPLETED -> colors.success
        }
        is LuminaScreen.CustomList -> customList?.let { Color(it.colorHex) } ?: colors.accent
        LuminaScreen.Home -> colors.accent
    }

    val items = remember(screen, uiState.allReminders, uiState.settings.sortOrder, uiState.settings.showCompletedInLists) {
        when (screen) {
            is LuminaScreen.SmartList -> SmartListFilter.filterSmartList(
                reminders = uiState.allReminders,
                type = screen.type,
                sortOrder = uiState.settings.sortOrder,
                nowMillis = now
            )
            is LuminaScreen.CustomList -> SmartListFilter.filterCustomList(
                reminders = uiState.allReminders,
                listId = screen.listId,
                showCompleted = uiState.settings.showCompletedInLists,
                sortOrder = uiState.settings.sortOrder
            )
            LuminaScreen.Home -> emptyList()
        }
    }

    val isCompletedScreen = screen == LuminaScreen.SmartList(SmartListType.COMPLETED)
    val overdueItems = remember(items, isCompletedScreen) {
        if (isCompletedScreen) emptyList() else items.filter { SmartListFilter.isOverdue(it, now) }
    }
    val regularItems = remember(items, overdueItems) {
        val overdueIds = overdueItems.map { it.id }.toSet()
        items.filter { it.id !in overdueIds }
    }

    var menuExpanded by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = chromeHazeState),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 104.dp,
                bottom = 148.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (items.isEmpty()) {
                item(key = "empty") {
                    GlassCard(
                        hazeState = meshHazeState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircleOutline,
                                contentDescription = null,
                                tint = screenAccent,
                                modifier = Modifier.size(40.dp)
                            )
                            Text(
                                text = "All clear in $screenTitle",
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.textPrimary
                            )
                            Text(
                                text = if (isCompletedScreen) {
                                    "Completed reminders appear here so you can review or undo them."
                                } else {
                                    "Tap the + orb below to capture a reminder."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textSecondary
                            )
                        }
                    }
                }
            } else {
                if (overdueItems.isNotEmpty()) {
                    item(key = "header_overdue") {
                        Text(
                            text = "Overdue (${overdueItems.size})",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = colors.danger,
                            modifier = Modifier.padding(start = 6.dp, top = 4.dp, bottom = 2.dp)
                        )
                    }
                    items(overdueItems, key = { it.id }) { item ->
                        ReminderRow(
                            item = item,
                            listMeta = item.listId?.let { listById[it] },
                            hazeState = meshHazeState,
                            onToggleComplete = { onToggleComplete(item.id) },
                            onToggleFlag = { onToggleFlag(item.id) },
                            onToggleSubtask = onToggleSubtask,
                            onDelete = { onDeleteReminder(item.id) },
                            onClick = { onOpenReminder(item) }
                        )
                    }
                }

                if (regularItems.isNotEmpty()) {
                    if (overdueItems.isNotEmpty()) {
                        item(key = "header_upcoming") {
                            Text(
                                text = "Upcoming (${regularItems.size})",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = colors.textSecondary,
                                modifier = Modifier.padding(start = 6.dp, top = 8.dp, bottom = 2.dp)
                            )
                        }
                    }
                    items(regularItems, key = { it.id }) { item ->
                        ReminderRow(
                            item = item,
                            listMeta = item.listId?.let { listById[it] },
                            hazeState = meshHazeState,
                            onToggleComplete = { onToggleComplete(item.id) },
                            onToggleFlag = { onToggleFlag(item.id) },
                            onToggleSubtask = onToggleSubtask,
                            onDelete = { onDeleteReminder(item.id) },
                            onClick = { onOpenReminder(item) }
                        )
                    }
                }
            }
        }

        // Progressive Apple Scroll-Edge Blur
        TopFadeBlur(
            hazeState = chromeHazeState,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // BitChord Floating Top Islands (Back Orb + Title Pill + Menu Orb)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .hazeGlassContainer(mergeDistance = 10.dp)
                .align(Alignment.TopCenter),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            GlassOrbButton(
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back",
                hazeState = chromeHazeState,
                onClick = onBack
            )

            GlassChromeBar(
                hazeState = chromeHazeState,
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(screenAccent.copy(alpha = 0.22f))
                            .border(0.8.dp, screenAccent.copy(alpha = 0.6f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = screenIcon,
                            contentDescription = null,
                            tint = screenAccent,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = screenTitle,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = items.size.toString(),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFeatureSettings = "tnum"
                        ),
                        color = colors.textSecondary
                    )
                }
            }

            Box {
                GlassOrbButton(
                    icon = Icons.Rounded.MoreVert,
                    contentDescription = "List menu",
                    hazeState = chromeHazeState,
                    onClick = { menuExpanded = true }
                )

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    SortOrder.entries.forEach { order ->
                        val selected = uiState.settings.sortOrder == order
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = if (selected) "Sort: ${order.label} ✓" else "Sort: ${order.label}"
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onSortOrderChange(order)
                            }
                        )
                    }

                    if (screen is LuminaScreen.CustomList) {
                        val showCompleted = uiState.settings.showCompletedInLists
                        DropdownMenuItem(
                            text = {
                                Text(if (showCompleted) "Hide Completed" else "Show Completed")
                            },
                            onClick = {
                                menuExpanded = false
                                onToggleShowCompleted(!showCompleted)
                            }
                        )
                    }

                    if (isCompletedScreen && items.isNotEmpty()) {
                        DropdownMenuItem(
                            text = { Text("Clear Completed", color = colors.danger) },
                            onClick = {
                                menuExpanded = false
                                onClearCompleted()
                            }
                        )
                    }
                }
            }
        }
    }
}
