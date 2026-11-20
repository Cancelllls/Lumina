package com.cancellls.lumina.domain

import java.time.Instant
import java.time.ZoneId

object SmartListFilter {

    fun endOfTodayMillis(
        nowMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Long {
        val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
        return today.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli() - 1L
    }

    fun isOverdue(reminder: ReminderItem, nowMillis: Long): Boolean {
        if (reminder.isCompleted) return false
        val due = reminder.effectiveDueMillis ?: return false
        return due < nowMillis
    }

    fun isDueTodayOrOverdue(
        reminder: ReminderItem,
        nowMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Boolean {
        if (reminder.isCompleted) return false
        val due = reminder.effectiveDueMillis ?: return false
        return due <= endOfTodayMillis(nowMillis, zoneId)
    }

    fun computeCounts(
        reminders: List<ReminderItem>,
        nowMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): SmartCounts {
        val endOfToday = endOfTodayMillis(nowMillis, zoneId)
        var today = 0
        var scheduled = 0
        var flagged = 0
        var active = 0
        var completed = 0

        for (item in reminders) {
            if (item.isCompleted) {
                completed++
            } else {
                active++
                val due = item.effectiveDueMillis
                if (due != null) {
                    scheduled++
                    if (due <= endOfToday) today++
                }
                if (item.isFlagged) flagged++
            }
        }
        return SmartCounts(
            today = today,
            scheduled = scheduled,
            flagged = flagged,
            all = active,
            completed = completed
        )
    }

    fun filterSmartList(
        reminders: List<ReminderItem>,
        type: SmartListType,
        sortOrder: SortOrder = SortOrder.DUE_DATE,
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): List<ReminderItem> {
        val filtered = when (type) {
            SmartListType.TODAY -> reminders.filter { isDueTodayOrOverdue(it, nowMillis, zoneId) }
            SmartListType.SCHEDULED -> reminders.filter { !it.isCompleted && it.effectiveDueMillis != null }
            SmartListType.FLAGGED -> reminders.filter { !it.isCompleted && it.isFlagged }
            SmartListType.ALL -> reminders.filter { !it.isCompleted }
            SmartListType.COMPLETED -> return reminders
                .filter { it.isCompleted }
                .sortedByDescending { it.completedAtMillis ?: it.updatedAtMillis }
        }
        return sortReminders(filtered, sortOrder)
    }

    fun filterCustomList(
        reminders: List<ReminderItem>,
        listId: Long,
        showCompleted: Boolean = false,
        sortOrder: SortOrder = SortOrder.DUE_DATE
    ): List<ReminderItem> {
        val matching = reminders.filter {
            it.listId == listId && (showCompleted || !it.isCompleted)
        }
        val active = sortReminders(matching.filter { !it.isCompleted }, sortOrder)
        return if (showCompleted) {
            val done = matching
                .filter { it.isCompleted }
                .sortedByDescending { it.completedAtMillis ?: it.updatedAtMillis }
            active + done
        } else {
            active
        }
    }

    fun search(
        reminders: List<ReminderItem>,
        listNameById: Map<Long, String>,
        query: String
    ): List<ReminderItem> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()
        return reminders.filter { item ->
            item.title.lowercase().contains(q) ||
                item.notes.lowercase().contains(q) ||
                item.subtasks.any { it.title.lowercase().contains(q) } ||
                (item.listId?.let { listNameById[it]?.lowercase()?.contains(q) } == true)
        }.sortedWith(
            compareBy<ReminderItem> { it.isCompleted }
                .thenBy { it.effectiveDueMillis ?: Long.MAX_VALUE }
                .thenByDescending { it.priority.level }
        )
    }

    fun sortReminders(items: List<ReminderItem>, sortOrder: SortOrder): List<ReminderItem> {
        return when (sortOrder) {
            SortOrder.DUE_DATE -> items.sortedWith(
                compareBy<ReminderItem> { it.effectiveDueMillis ?: Long.MAX_VALUE }
                    .thenByDescending { it.priority.level }
                    .thenBy { it.createdAtMillis }
            )
            SortOrder.PRIORITY -> items.sortedWith(
                compareByDescending<ReminderItem> { it.priority.level }
                    .thenBy { it.effectiveDueMillis ?: Long.MAX_VALUE }
                    .thenBy { it.createdAtMillis }
            )
            SortOrder.CREATED -> items.sortedWith(
                compareByDescending<ReminderItem> { it.createdAtMillis }
            )
        }
    }
}
