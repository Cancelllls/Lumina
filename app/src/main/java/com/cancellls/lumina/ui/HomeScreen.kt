package com.cancellls.lumina.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cancellls.lumina.domain.ReminderItem
import com.cancellls.lumina.domain.ReminderListItem
import com.cancellls.lumina.domain.SmartListFilter
import com.cancellls.lumina.domain.SmartListType
import com.cancellls.lumina.ui.glass.GlassCard
import com.cancellls.lumina.ui.glass.GlassChip
import com.cancellls.lumina.ui.glass.GlassChromeBar
import com.cancellls.lumina.ui.glass.GlassCompletionRing
import com.cancellls.lumina.ui.glass.GlassOrbButton
import com.cancellls.lumina.ui.glass.ListIconCatalog
import com.cancellls.lumina.ui.glass.TopFadeBlur
import com.cancellls.lumina.ui.theme.LocalLuminaColors
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.glass.hazeGlassContainer
import dev.chrisbanes.haze.hazeSource

@Composable
fun HomeScreen(
    uiState: MainUiState,
    meshHazeState: HazeState,
    chromeHazeState: HazeState,
    showPermissionBanner: Boolean,
    onGrantPermissionsClick: () -> Unit,
    onDismissPermissionBanner: () -> Unit,
    onToggleSearch: (Boolean) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSmartList: (SmartListType) -> Unit,
    onOpenCustomList: (Long) -> Unit,
    onCreateList: () -> Unit,
    onEditList: (ReminderListItem) -> Unit,
    onDeleteList: (Long) -> Unit,
    onToggleComplete: (Long) -> Unit,
    onToggleFlag: (Long) -> Unit,
    onToggleSubtask: (Long) -> Unit,
    onDeleteReminder: (Long) -> Unit,
    onOpenReminder: (ReminderItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalLuminaColors.current
    val listById = remember(uiState.lists) { uiState.lists.associateBy { it.id } }
    val listNameById = remember(uiState.lists) { uiState.lists.associate { it.id to it.name } }

    val searchResults = remember(uiState.allReminders, uiState.searchQuery, listNameById) {
        if (uiState.searchQuery.isBlank()) {
            emptyList()
        } else {
            SmartListFilter.search(uiState.allReminders, listNameById, uiState.searchQuery)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = chromeHazeState),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 12.dp,
                bottom = 148.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "header") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(top = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(modifier = Modifier.padding(end = 104.dp)) {
                        Text(
                            text = "Lumina",
                            style = MaterialTheme.typography.headlineLarge,
                            color = colors.textPrimary
                        )
                        Text(
                            text = UiFormatters.formatHeaderDate(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary
                        )
                    }

                    AnimatedVisibility(visible = uiState.isSearchActive) {
                        GlassChromeBar(
                            hazeState = meshHazeState,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Search,
                                    contentDescription = null,
                                    tint = colors.textSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Box(modifier = Modifier.weight(1f)) {
                                    if (uiState.searchQuery.isEmpty()) {
                                        Text(
                                            text = "Search reminders, notes, lists...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = colors.textTertiary
                                        )
                                    }
                                    BasicTextField(
                                        value = uiState.searchQuery,
                                        onValueChange = onSearchQueryChange,
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                                            color = colors.textPrimary
                                        ),
                                        cursorBrush = SolidColor(colors.accent),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                if (uiState.searchQuery.isNotEmpty()) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Clear search",
                                        tint = colors.textSecondary,
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clickable { onSearchQueryChange("") }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (uiState.isSearchActive && uiState.searchQuery.isNotBlank()) {
                if (searchResults.isEmpty()) {
                    item(key = "search_empty") {
                        GlassCard(
                            hazeState = meshHazeState,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Search,
                                    contentDescription = null,
                                    tint = colors.textTertiary,
                                    modifier = Modifier.size(34.dp)
                                )
                                Text(
                                    text = "No matching reminders",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = "Try another keyword across titles, notes, or list names.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.textSecondary
                                )
                            }
                        }
                    }
                } else {
                    items(searchResults, key = { it.id }) { item ->
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
                return@LazyColumn
            }

            if (showPermissionBanner) {
                item(key = "permission_banner") {
                    GlassCard(
                        hazeState = meshHazeState,
                        accentTint = colors.warning,
                        shape = RoundedCornerShape(22.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(colors.warning.copy(alpha = 0.22f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.NotificationsActive,
                                    contentDescription = null,
                                    tint = colors.warning,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Enable exact reminders",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = "Allow notifications & exact alarms so reminders ring on time.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.textSecondary
                                )
                            }
                            GlassChip(
                                label = "Allow",
                                tint = colors.warning,
                                selected = true,
                                onClick = onGrantPermissionsClick
                            )
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Dismiss",
                                tint = colors.textTertiary,
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable(onClick = onDismissPermissionBanner)
                            )
                        }
                    }
                }
            }

            // 2x2 Bento Glass Smart Grid
            item(key = "bento_row_1") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SmartBentoTile(
                        title = "Today",
                        count = uiState.smartCounts.today,
                        icon = Icons.Rounded.Today,
                        accentColor = Color(0xFF38BDF8),
                        hazeState = meshHazeState,
                        onClick = { onOpenSmartList(SmartListType.TODAY) },
                        modifier = Modifier.weight(1f)
                    )
                    SmartBentoTile(
                        title = "Scheduled",
                        count = uiState.smartCounts.scheduled,
                        icon = Icons.Rounded.CalendarMonth,
                        accentColor = Color(0xFFFA2D48),
                        hazeState = meshHazeState,
                        onClick = { onOpenSmartList(SmartListType.SCHEDULED) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item(key = "bento_row_2") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SmartBentoTile(
                        title = "Flagged",
                        count = uiState.smartCounts.flagged,
                        icon = Icons.Rounded.Flag,
                        accentColor = Color(0xFFFF9F0A),
                        hazeState = meshHazeState,
                        onClick = { onOpenSmartList(SmartListType.FLAGGED) },
                        modifier = Modifier.weight(1f)
                    )
                    SmartBentoTile(
                        title = "All",
                        count = uiState.smartCounts.all,
                        icon = Icons.Rounded.Inbox,
                        accentColor = colors.accent,
                        hazeState = meshHazeState,
                        onClick = { onOpenSmartList(SmartListType.ALL) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Completed Strip
            item(key = "completed_strip") {
                GlassCard(
                    hazeState = meshHazeState,
                    shape = RoundedCornerShape(20.dp),
                    onClick = { onOpenSmartList(SmartListType.COMPLETED) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(colors.success.copy(alpha = 0.22f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DoneAll,
                                contentDescription = null,
                                tint = colors.success,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Completed",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = uiState.smartCounts.completed.toString(),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFeatureSettings = "tnum"
                            ),
                            color = colors.textSecondary
                        )
                    }
                }
            }

            // Up Next Glass Preview Card
            if (uiState.upNextReminders.isNotEmpty()) {
                item(key = "up_next_card") {
                    GlassCard(
                        hazeState = meshHazeState,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Up Next",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = colors.textPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "See Today",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = colors.accent,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onOpenSmartList(SmartListType.TODAY) }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            uiState.upNextReminders.forEachIndexed { index, item ->
                                val listMeta = item.listId?.let { listById[it] }
                                val ringColor = listMeta?.let { Color(it.colorHex) } ?: colors.accent
                                val isOverdue = UiFormatters.isOverdue(item)

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { onOpenReminder(item) }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    GlassCompletionRing(
                                        isCompleted = item.isCompleted,
                                        ringColor = if (isOverdue) colors.danger else ringColor,
                                        onToggle = { onToggleComplete(item.id) },
                                        size = 22.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.title,
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontWeight = FontWeight.Medium
                                            ),
                                            color = colors.textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        val dueMillis = item.effectiveDueMillis
                                        if (dueMillis != null) {
                                            Text(
                                                text = UiFormatters.formatDueChip(
                                                    dueMillis = dueMillis,
                                                    hasTime = item.hasTime,
                                                    isSnoozed = item.snoozedUntilMillis != null
                                                ),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = if (isOverdue) colors.danger else colors.textSecondary
                                            )
                                        }
                                    }
                                }

                                if (index < uiState.upNextReminders.lastIndex) {
                                    HorizontalDivider(
                                        thickness = 0.5.dp,
                                        color = colors.rimBottomRight.copy(alpha = 0.55f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // My Lists Glass Card
            item(key = "my_lists") {
                GlassCard(
                    hazeState = meshHazeState,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "My Lists",
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.textPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            GlassChip(
                                label = "New List",
                                icon = Icons.Rounded.Add,
                                tint = colors.accent,
                                selected = true,
                                onClick = onCreateList
                            )
                        }

                        if (uiState.lists.isEmpty()) {
                            Text(
                                text = "Tap New List to organize reminders by category.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textSecondary,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        } else {
                            uiState.lists.forEachIndexed { idx, list ->
                                var menuOpen by remember { mutableStateOf(false) }
                                val listColor = Color(list.colorHex)
                                val activeCount = uiState.listActiveCounts[list.id] ?: 0

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable { onOpenCustomList(list.id) }
                                        .padding(horizontal = 6.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(listColor.copy(alpha = 0.22f))
                                            .border(0.8.dp, listColor.copy(alpha = 0.55f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = ListIconCatalog.iconForKey(list.iconKey),
                                            contentDescription = null,
                                            tint = listColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = list.name,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = FontWeight.Medium
                                        ),
                                        color = colors.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = activeCount.toString(),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontFeatureSettings = "tnum"
                                        ),
                                        color = colors.textSecondary
                                    )
                                    Box {
                                        IconButton(
                                            onClick = { menuOpen = true },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.MoreVert,
                                                contentDescription = "List options",
                                                tint = colors.textTertiary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        DropdownMenu(
                                            expanded = menuOpen,
                                            onDismissRequest = { menuOpen = false }
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("Edit List") },
                                                onClick = {
                                                    menuOpen = false
                                                    onEditList(list)
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Delete List", color = colors.danger) },
                                                onClick = {
                                                    menuOpen = false
                                                    onDeleteList(list.id)
                                                }
                                            )
                                        }
                                    }
                                }

                                if (idx < uiState.lists.lastIndex) {
                                    HorizontalDivider(
                                        thickness = 0.5.dp,
                                        color = colors.rimBottomRight.copy(alpha = 0.45f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // BitChord Progressive Apple Scroll-Edge Blur
        TopFadeBlur(
            hazeState = chromeHazeState,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // Floating Circular Liquid Glass Controls over TopFadeBlur
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .hazeGlassContainer(mergeDistance = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            GlassOrbButton(
                icon = if (uiState.isSearchActive) Icons.Rounded.Close else Icons.Rounded.Search,
                contentDescription = if (uiState.isSearchActive) "Close search" else "Search reminders",
                hazeState = chromeHazeState,
                onClick = { onToggleSearch(!uiState.isSearchActive) }
            )
            GlassOrbButton(
                icon = Icons.Rounded.Settings,
                contentDescription = "Settings",
                hazeState = chromeHazeState,
                onClick = onOpenSettings
            )
        }
    }
}

@Composable
private fun SmartBentoTile(
    title: String,
    count: Int,
    icon: ImageVector,
    accentColor: Color,
    hazeState: HazeState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalLuminaColors.current
    GlassCard(
        hazeState = hazeState,
        accentTint = accentColor,
        onClick = onClick,
        modifier = modifier.height(106.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.24f))
                        .border(0.8.dp, accentColor.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(19.dp)
                    )
                }

                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        fontFeatureSettings = "tnum"
                    ),
                    color = colors.textPrimary
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = colors.textSecondary
            )
        }
    }
}
