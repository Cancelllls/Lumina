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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.OutlinedFlag
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Snooze
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cancellls.lumina.domain.Priority
import com.cancellls.lumina.domain.RecurrenceEngine
import com.cancellls.lumina.domain.ReminderItem
import com.cancellls.lumina.domain.ReminderListItem
import com.cancellls.lumina.domain.SubtaskItem
import com.cancellls.lumina.ui.glass.GlassChip
import com.cancellls.lumina.ui.theme.LocalLuminaColors
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReminderEditorSheet(
    modal: ActiveModal.ReminderEditor,
    lists: List<ReminderListItem>,
    onDismiss: () -> Unit,
    onSave: (
        existing: ReminderItem?,
        title: String,
        notes: String,
        listId: Long?,
        dueAtMillis: Long?,
        hasTime: Boolean,
        priority: Priority,
        isFlagged: Boolean,
        recurrenceRule: String,
        subtasks: List<SubtaskItem>
    ) -> Unit,
    onDelete: (Long) -> Unit,
    onSnooze: (Long, Int) -> Unit,
    onDuplicate: (ReminderItem) -> Unit,
    onShare: (ReminderItem) -> Unit
) {
    val colors = LocalLuminaColors.current
    val existing = modal.existing
    val zone = ZoneId.systemDefault()

    var title by remember { mutableStateOf(existing?.title ?: modal.prefillTitle) }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var selectedListId by remember {
        mutableStateOf(existing?.listId ?: modal.defaultListId ?: lists.firstOrNull()?.id)
    }

    val initialDue = existing?.dueAtMillis ?: modal.prefillDueMillis
    val initialHasTime = existing?.hasTime ?: modal.prefillHasTime
    val initialZdt = initialDue?.let { Instant.ofEpochMilli(it).atZone(zone) }

    var selectedDate by remember { mutableStateOf(initialZdt?.toLocalDate()) }
    var selectedTime by remember {
        mutableStateOf(
            if (initialZdt != null && initialHasTime) initialZdt.toLocalTime() else null
        )
    }
    var priority by remember { mutableStateOf(existing?.priority ?: modal.prefillPriority) }
    var isFlagged by remember { mutableStateOf(existing?.isFlagged ?: modal.prefillFlagged) }
    var recurrenceRule by remember { mutableStateOf(existing?.recurrenceRule ?: modal.prefillRecurrence) }

    val subtasks = remember {
        mutableStateListOf<SubtaskItem>().apply {
            existing?.subtasks?.let { addAll(it) }
        }
    }
    var newSubtaskTitle by remember { mutableStateOf("") }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showCustomRecurrence by remember {
        mutableStateOf(recurrenceRule.startsWith("EVERY_N_"))
    }
    var customInterval by remember {
        mutableIntStateOf(
            recurrenceRule.substringAfter(":", "2").toIntOrNull()?.coerceIn(1, 99) ?: 2
        )
    }
    var customUnit by remember {
        mutableStateOf(
            when {
                recurrenceRule.startsWith("EVERY_N_WEEKS") -> "WEEKS"
                recurrenceRule.startsWith("EVERY_N_MONTHS") -> "MONTHS"
                else -> "DAYS"
            }
        )
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    fun computeDueMillis(): Pair<Long?, Boolean> {
        val date = selectedDate ?: return null to false
        val time = selectedTime ?: LocalTime.of(9, 0)
        val hasTime = selectedTime != null
        val millis = ZonedDateTime.of(date, time, zone).toInstant().toEpochMilli()
        return millis to hasTime
    }

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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (existing == null) "New Reminder" else "Edit Reminder",
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

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                placeholder = { Text("What do you need to remember?") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                placeholder = { Text("Add details, links, or context...") },
                minLines = 2,
                maxLines = 4,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Schedule Section
            Text(
                text = "Schedule",
                style = MaterialTheme.typography.labelLarge,
                color = colors.textSecondary
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val today = LocalDate.now(zone)
                GlassChip(
                    label = "Today",
                    selected = selectedDate == today,
                    onClick = { selectedDate = today }
                )
                GlassChip(
                    label = "Tomorrow",
                    selected = selectedDate == today.plusDays(1),
                    onClick = { selectedDate = today.plusDays(1) }
                )
                GlassChip(
                    label = "Next Week",
                    selected = selectedDate == today.plusWeeks(1),
                    onClick = { selectedDate = today.plusWeeks(1) }
                )
                GlassChip(
                    label = selectedDate?.let {
                        UiFormatters.formatDateOnly(
                            it.atStartOfDay(zone).toInstant().toEpochMilli(),
                            zone
                        )
                    } ?: "Pick Date",
                    icon = Icons.Rounded.CalendarMonth,
                    selected = selectedDate != null,
                    onClick = { showDatePicker = true }
                )
                GlassChip(
                    label = selectedTime?.let {
                        val dt = (selectedDate ?: today).atTime(it).atZone(zone).toInstant().toEpochMilli()
                        UiFormatters.formatTimeOnly(dt, zone)
                    } ?: "Pick Time",
                    icon = Icons.Rounded.AccessTime,
                    selected = selectedTime != null,
                    onClick = {
                        if (selectedDate == null) selectedDate = today
                        showTimePicker = true
                    }
                )
                if (selectedDate != null || selectedTime != null) {
                    GlassChip(
                        label = "Clear",
                        icon = Icons.Rounded.Close,
                        tint = colors.danger,
                        onClick = {
                            selectedDate = null
                            selectedTime = null
                        }
                    )
                }
            }

            // Quick Actions & Snooze for existing reminder
            if (existing != null) {
                Text(
                    text = if (existing.isCompleted) "Quick Actions" else "Snooze & Actions",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.textSecondary
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!existing.isCompleted) {
                        listOf(15 to "+15m", 60 to "+1h", 180 to "+3h").forEach { (mins, label) ->
                            GlassChip(
                                label = label,
                                icon = Icons.Rounded.Snooze,
                                tint = colors.accentSecondary,
                                onClick = { onSnooze(existing.id, mins) }
                            )
                        }
                    }
                    GlassChip(
                        label = "Duplicate",
                        icon = Icons.Rounded.ContentCopy,
                        tint = colors.textSecondary,
                        onClick = { onDuplicate(existing) }
                    )
                    GlassChip(
                        label = "Share",
                        icon = Icons.Rounded.Share,
                        tint = colors.textSecondary,
                        onClick = { onShare(existing) }
                    )
                }
            }

            // Recurrence Section
            Text(
                text = "Repeat",
                style = MaterialTheme.typography.labelLarge,
                color = colors.textSecondary
            )

            val presetRules = listOf(
                RecurrenceEngine.NONE to "Never",
                RecurrenceEngine.DAILY to "Daily",
                RecurrenceEngine.WEEKDAYS to "Weekdays",
                RecurrenceEngine.WEEKLY to "Weekly",
                RecurrenceEngine.MONTHLY to "Monthly",
                RecurrenceEngine.YEARLY to "Yearly"
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presetRules.forEach { (rule, label) ->
                    GlassChip(
                        label = label,
                        selected = !showCustomRecurrence && recurrenceRule == rule,
                        onClick = {
                            showCustomRecurrence = false
                            recurrenceRule = rule
                            if (rule.isNotEmpty() && selectedDate == null) {
                                selectedDate = LocalDate.now(zone)
                            }
                        }
                    )
                }
                GlassChip(
                    label = if (showCustomRecurrence) RecurrenceEngine.formatLabel(recurrenceRule) else "Custom",
                    selected = showCustomRecurrence,
                    onClick = {
                        showCustomRecurrence = true
                        if (selectedDate == null) selectedDate = LocalDate.now(zone)
                        recurrenceRule = when (customUnit) {
                            "WEEKS" -> RecurrenceEngine.everyNWeeks(customInterval)
                            "MONTHS" -> RecurrenceEngine.everyNMonths(customInterval)
                            else -> RecurrenceEngine.everyNDays(customInterval)
                        }
                    }
                )
            }

            AnimatedVisibility(visible = showCustomRecurrence) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.solidSurface.copy(alpha = 0.65f))
                        .border(1.dp, colors.rimBottomRight, RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Every",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textPrimary
                    )
                    IconButton(
                        onClick = {
                            if (customInterval > 1) {
                                customInterval--
                                recurrenceRule = buildCustomRule(customInterval, customUnit)
                            }
                        },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(Icons.Rounded.Remove, contentDescription = "Decrease")
                    }
                    Text(
                        text = customInterval.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.accent
                    )
                    IconButton(
                        onClick = {
                            if (customInterval < 99) {
                                customInterval++
                                recurrenceRule = buildCustomRule(customInterval, customUnit)
                            }
                        },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = "Increase")
                    }
                    listOf("DAYS" to "Days", "WEEKS" to "Weeks", "MONTHS" to "Months").forEach { (unit, label) ->
                        GlassChip(
                            label = label,
                            selected = customUnit == unit,
                            onClick = {
                                customUnit = unit
                                recurrenceRule = buildCustomRule(customInterval, unit)
                            }
                        )
                    }
                }
            }

            // Priority & Flag
            Text(
                text = "Priority & Flag",
                style = MaterialTheme.typography.labelLarge,
                color = colors.textSecondary
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Priority.entries.forEach { p ->
                    val pColor = when (p) {
                        Priority.HIGH -> colors.danger
                        Priority.MEDIUM -> colors.warning
                        Priority.LOW -> colors.accent
                        Priority.NONE -> colors.textSecondary
                    }
                    GlassChip(
                        label = p.label,
                        tint = pColor,
                        selected = priority == p,
                        onClick = { priority = p }
                    )
                }
                GlassChip(
                    label = if (isFlagged) "Flagged" else "Flag",
                    icon = if (isFlagged) Icons.Rounded.Flag else Icons.Rounded.OutlinedFlag,
                    tint = colors.warning,
                    selected = isFlagged,
                    onClick = { isFlagged = !isFlagged }
                )
            }

            // List Picker
            if (lists.isNotEmpty()) {
                Text(
                    text = "List",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.textSecondary
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    lists.forEach { list ->
                        GlassChip(
                            label = list.name,
                            tint = Color(list.colorHex),
                            selected = selectedListId == list.id,
                            onClick = { selectedListId = list.id }
                        )
                    }
                }
            }

            // Subtasks
            Text(
                text = "Subtasks (${subtasks.size})",
                style = MaterialTheme.typography.labelLarge,
                color = colors.textSecondary
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                subtasks.forEachIndexed { idx, sub ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.solidSurface.copy(alpha = 0.65f))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(if (sub.isCompleted) colors.accent else Color.Transparent)
                                .border(1.5.dp, colors.accent, CircleShape)
                                .clickable {
                                    subtasks[idx] = sub.copy(isCompleted = !sub.isCompleted)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (sub.isCompleted) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = sub.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { subtasks.removeAt(idx) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Remove subtask",
                                tint = colors.textTertiary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newSubtaskTitle,
                        onValueChange = { newSubtaskTitle = it },
                        placeholder = { Text("Add subtask...") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    )
                    GlassChip(
                        label = "Add",
                        icon = Icons.Rounded.Add,
                        selected = newSubtaskTitle.isNotBlank(),
                        onClick = {
                            val t = newSubtaskTitle.trim()
                            if (t.isNotEmpty()) {
                                subtasks += SubtaskItem(title = t, position = subtasks.size)
                                newSubtaskTitle = ""
                            }
                        }
                    )
                }
            }

            HorizontalDivider(color = colors.rimBottomRight)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (existing != null) {
                    OutlinedButton(
                        onClick = { onDelete(existing.id) },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.danger),
                        modifier = Modifier.height(50.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete")
                    }
                }

                Button(
                    onClick = {
                        val (dueMillis, hasTime) = computeDueMillis()
                        onSave(
                            existing,
                            title,
                            notes,
                            selectedListId,
                            dueMillis,
                            hasTime,
                            priority,
                            isFlagged,
                            recurrenceRule,
                            subtasks.toList()
                        )
                    },
                    enabled = title.isNotBlank(),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                ) {
                    Text(
                        text = if (existing == null) "Create Reminder" else "Save Changes",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        val initialSelectedUtc = (selectedDate ?: LocalDate.now(zone))
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialSelectedUtc)

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { utcMillis ->
                            selectedDate = Instant.ofEpochMilli(utcMillis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Set Date")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val initTime = selectedTime ?: LocalTime.of(9, 0)
        val timePickerState = rememberTimePickerState(
            initialHour = initTime.hour,
            initialMinute = initTime.minute,
            is24Hour = false
        )

        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Select Time") },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                        showTimePicker = false
                    }
                ) {
                    Text("Set Time")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun buildCustomRule(interval: Int, unit: String): String = when (unit) {
    "WEEKS" -> RecurrenceEngine.everyNWeeks(interval)
    "MONTHS" -> RecurrenceEngine.everyNMonths(interval)
    else -> RecurrenceEngine.everyNDays(interval)
}
