package com.cancellls.lumina.data

import androidx.room.withTransaction
import com.cancellls.lumina.alarm.ReminderAlarmScheduler
import com.cancellls.lumina.data.db.LuminaDatabase
import com.cancellls.lumina.data.db.ReminderEntity
import com.cancellls.lumina.data.db.ReminderListEntity
import com.cancellls.lumina.data.db.ReminderWithSubtasks
import com.cancellls.lumina.data.db.SubtaskEntity
import com.cancellls.lumina.domain.BackupCodec
import com.cancellls.lumina.domain.BackupPayload
import com.cancellls.lumina.domain.Priority
import com.cancellls.lumina.domain.RecurrenceEngine
import com.cancellls.lumina.domain.ReminderItem
import com.cancellls.lumina.domain.ReminderListItem
import com.cancellls.lumina.domain.SubtaskItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class ToggleCompleteResult(
    val previousReminder: ReminderItem,
    val nowCompleted: Boolean,
    val spawnedNextId: Long? = null
)

class ReminderRepository(
    private val database: LuminaDatabase,
    private val alarmScheduler: ReminderAlarmScheduler,
    private val onWidgetRefresh: () -> Unit
) {
    private val dao = database.luminaDao()

    fun observeLists(): Flow<List<ReminderListItem>> =
        dao.observeLists().map { list -> list.map { it.toDomain() } }

    fun observeReminders(): Flow<List<ReminderItem>> =
        dao.observeAllRemindersWithSubtasks().map { items -> items.map { it.toDomain() } }

    suspend fun getReminderById(reminderId: Long): ReminderItem? =
        dao.getReminderWithSubtasks(reminderId)?.toDomain()

    suspend fun getActiveRemindersSnapshot(): List<ReminderItem> =
        dao.getAllRemindersWithSubtasksOnce()
            .map { it.toDomain() }
            .filter { !it.isCompleted }

    suspend fun createReminder(
        title: String,
        notes: String = "",
        listId: Long? = null,
        dueAtMillis: Long? = null,
        hasTime: Boolean = true,
        priority: Priority = Priority.NONE,
        isFlagged: Boolean = false,
        recurrenceRule: String = RecurrenceEngine.NONE,
        subtasks: List<SubtaskItem> = emptyList()
    ): Long {
        val now = System.currentTimeMillis()
        val trimmedTitle = title.trim()
        require(trimmedTitle.isNotEmpty()) { "Reminder title cannot be empty." }

        val entity = ReminderEntity(
            listId = listId,
            title = trimmedTitle,
            notes = notes.trim(),
            dueAtMillis = dueAtMillis,
            hasTime = hasTime,
            snoozedUntilMillis = null,
            priority = priority.level,
            isFlagged = isFlagged,
            recurrenceRule = recurrenceRule.takeIf { RecurrenceEngine.isValidRule(it) } ?: "",
            isCompleted = false,
            completedAtMillis = null,
            createdAtMillis = now,
            updatedAtMillis = now
        )

        val insertedId = database.withTransaction {
            val id = dao.insertReminder(entity)
            if (subtasks.isNotEmpty()) {
                dao.insertSubtasks(
                    subtasks.mapIndexed { index, sub ->
                        SubtaskEntity(
                            reminderId = id,
                            title = sub.title.trim(),
                            isCompleted = sub.isCompleted,
                            position = index
                        )
                    }.filter { it.title.isNotEmpty() }
                )
            }
            id
        }

        dao.getReminderWithSubtasks(insertedId)?.toDomain()?.let { alarmScheduler.schedule(it) }
        onWidgetRefresh()
        return insertedId
    }

    suspend fun updateReminder(item: ReminderItem) {
        val now = System.currentTimeMillis()
        val updatedEntity = ReminderEntity(
            id = item.id,
            listId = item.listId,
            title = item.title.trim(),
            notes = item.notes.trim(),
            dueAtMillis = item.dueAtMillis,
            hasTime = item.hasTime,
            snoozedUntilMillis = item.snoozedUntilMillis,
            priority = item.priority.level,
            isFlagged = item.isFlagged,
            recurrenceRule = item.recurrenceRule.takeIf { RecurrenceEngine.isValidRule(it) } ?: "",
            isCompleted = item.isCompleted,
            completedAtMillis = item.completedAtMillis,
            createdAtMillis = item.createdAtMillis,
            updatedAtMillis = now
        )

        database.withTransaction {
            dao.updateReminder(updatedEntity)
            dao.deleteSubtasksForReminder(item.id)
            val validSubtasks = item.subtasks
                .map { it.copy(title = it.title.trim()) }
                .filter { it.title.isNotEmpty() }
            if (validSubtasks.isNotEmpty()) {
                dao.insertSubtasks(
                    validSubtasks.mapIndexed { index, sub ->
                        SubtaskEntity(
                            reminderId = item.id,
                            title = sub.title,
                            isCompleted = sub.isCompleted,
                            position = index
                        )
                    }
                )
            }
        }

        val refreshed = dao.getReminderWithSubtasks(item.id)?.toDomain()
        if (refreshed != null) {
            alarmScheduler.schedule(refreshed)
        }
        onWidgetRefresh()
    }

    suspend fun toggleComplete(reminderId: Long): ToggleCompleteResult? {
        val existing = dao.getReminderWithSubtasks(reminderId)?.toDomain() ?: return null
        val now = System.currentTimeMillis()
        val markingComplete = !existing.isCompleted

        var spawnedNextId: Long? = null

        database.withTransaction {
            dao.updateReminder(
                existing.toEntity().copy(
                    isCompleted = markingComplete,
                    completedAtMillis = if (markingComplete) now else null,
                    snoozedUntilMillis = if (markingComplete) null else existing.snoozedUntilMillis,
                    updatedAtMillis = now
                )
            )

            if (markingComplete && existing.recurrenceRule.isNotEmpty()) {
                val baseDue = existing.dueAtMillis ?: now
                val nextDue = RecurrenceEngine.nextOccurrence(
                    currentDueMillis = baseDue,
                    rule = existing.recurrenceRule,
                    nowMillis = now
                )
                if (nextDue != null) {
                    val nextEntity = ReminderEntity(
                        listId = existing.listId,
                        title = existing.title,
                        notes = existing.notes,
                        dueAtMillis = nextDue,
                        hasTime = existing.hasTime,
                        snoozedUntilMillis = null,
                        priority = existing.priority.level,
                        isFlagged = existing.isFlagged,
                        recurrenceRule = existing.recurrenceRule,
                        isCompleted = false,
                        completedAtMillis = null,
                        createdAtMillis = now,
                        updatedAtMillis = now
                    )
                    val nextId = dao.insertReminder(nextEntity)
                    if (existing.subtasks.isNotEmpty()) {
                        dao.insertSubtasks(
                            existing.subtasks.mapIndexed { idx, sub ->
                                SubtaskEntity(
                                    reminderId = nextId,
                                    title = sub.title,
                                    isCompleted = false,
                                    position = idx
                                )
                            }
                        )
                    }
                    spawnedNextId = nextId
                }
            }
        }

        if (markingComplete) {
            alarmScheduler.cancel(reminderId)
            spawnedNextId?.let { nextId ->
                dao.getReminderWithSubtasks(nextId)?.toDomain()?.let { alarmScheduler.schedule(it) }
            }
        } else {
            dao.getReminderWithSubtasks(reminderId)?.toDomain()?.let { alarmScheduler.schedule(it) }
        }

        onWidgetRefresh()
        return ToggleCompleteResult(
            previousReminder = existing,
            nowCompleted = markingComplete,
            spawnedNextId = spawnedNextId
        )
    }

    suspend fun undoToggleComplete(previousReminder: ReminderItem, spawnedNextId: Long?) {
        database.withTransaction {
            if (spawnedNextId != null) {
                dao.deleteReminderById(spawnedNextId)
            }
            dao.updateReminder(previousReminder.toEntity().copy(updatedAtMillis = System.currentTimeMillis()))
        }
        if (spawnedNextId != null) {
            alarmScheduler.cancel(spawnedNextId)
        }
        dao.getReminderWithSubtasks(previousReminder.id)?.toDomain()?.let { alarmScheduler.schedule(it) }
        onWidgetRefresh()
    }

    suspend fun snoozeReminder(reminderId: Long, minutes: Int) {
        val existing = dao.getReminderWithSubtasks(reminderId)?.toDomain() ?: return
        val now = System.currentTimeMillis()
        val snoozedUntil = now + minutes.coerceAtLeast(1) * 60_000L

        dao.updateReminder(
            existing.toEntity().copy(
                snoozedUntilMillis = snoozedUntil,
                isCompleted = false,
                completedAtMillis = null,
                updatedAtMillis = now
            )
        )
        dao.getReminderWithSubtasks(reminderId)?.toDomain()?.let { alarmScheduler.schedule(it) }
        onWidgetRefresh()
    }

    suspend fun toggleFlag(reminderId: Long) {
        val existing = dao.getReminderWithSubtasks(reminderId)?.toDomain() ?: return
        dao.updateReminder(
            existing.toEntity().copy(
                isFlagged = !existing.isFlagged,
                updatedAtMillis = System.currentTimeMillis()
            )
        )
        onWidgetRefresh()
    }

    suspend fun toggleSubtask(subtaskId: Long) {
        val sub = dao.getSubtaskById(subtaskId) ?: return
        dao.updateSubtask(sub.copy(isCompleted = !sub.isCompleted))
        onWidgetRefresh()
    }

    suspend fun deleteReminder(reminderId: Long): ReminderItem? {
        val snapshot = dao.getReminderWithSubtasks(reminderId)?.toDomain() ?: return null
        alarmScheduler.cancel(reminderId)
        dao.deleteReminderById(reminderId)
        onWidgetRefresh()
        return snapshot
    }

    suspend fun restoreReminder(item: ReminderItem): Long {
        val restoredId = database.withTransaction {
            val id = dao.insertReminder(item.toEntity().copy(id = 0L, updatedAtMillis = System.currentTimeMillis()))
            if (item.subtasks.isNotEmpty()) {
                dao.insertSubtasks(
                    item.subtasks.mapIndexed { idx, sub ->
                        SubtaskEntity(
                            reminderId = id,
                            title = sub.title,
                            isCompleted = sub.isCompleted,
                            position = idx
                        )
                    }
                )
            }
            id
        }
        dao.getReminderWithSubtasks(restoredId)?.toDomain()?.let { alarmScheduler.schedule(it) }
        onWidgetRefresh()
        return restoredId
    }

    suspend fun clearCompleted() {
        dao.deleteCompletedReminders()
        onWidgetRefresh()
    }

    suspend fun createList(name: String, iconKey: String, colorHex: Long): Long {
        val existing = dao.getAllListsOnce()
        val nextSort = (existing.maxOfOrNull { it.sortOrder } ?: -1) + 1
        val id = dao.insertList(
            ReminderListEntity(
                name = name.trim(),
                iconKey = iconKey,
                colorHex = colorHex,
                sortOrder = nextSort
            )
        )
        onWidgetRefresh()
        return id
    }

    suspend fun updateList(list: ReminderListItem) {
        dao.updateList(
            ReminderListEntity(
                id = list.id,
                name = list.name.trim(),
                iconKey = list.iconKey,
                colorHex = list.colorHex,
                sortOrder = list.sortOrder
            )
        )
        onWidgetRefresh()
    }

    suspend fun deleteList(listId: Long) {
        dao.deleteListById(listId)
        onWidgetRefresh()
    }

    suspend fun exportBackupJson(): String {
        val lists = dao.getAllListsOnce().map { it.toDomain() }
        val reminders = dao.getAllRemindersWithSubtasksOnce().map { it.toDomain() }
        return BackupCodec.encode(
            BackupPayload(
                exportedAtMillis = System.currentTimeMillis(),
                lists = lists,
                reminders = reminders
            )
        )
    }

    suspend fun importBackupJson(json: String): Pair<Int, Int> {
        val payload = BackupCodec.decode(json)

        val oldReminders = dao.getAllRemindersWithSubtasksOnce()
        for (old in oldReminders) {
            alarmScheduler.cancel(old.reminder.id)
        }

        database.withTransaction {
            dao.clearAllSubtasks()
            dao.clearAllReminders()
            dao.clearAllLists()

            val listIdMap = mutableMapOf<Long, Long>()
            for (list in payload.lists) {
                val newListId = dao.insertList(
                    ReminderListEntity(
                        name = list.name,
                        iconKey = list.iconKey,
                        colorHex = list.colorHex,
                        sortOrder = list.sortOrder
                    )
                )
                listIdMap[list.id] = newListId
            }

            for (item in payload.reminders) {
                val mappedListId = item.listId?.let { listIdMap[it] }
                val newReminderId = dao.insertReminder(
                    ReminderEntity(
                        listId = mappedListId,
                        title = item.title,
                        notes = item.notes,
                        dueAtMillis = item.dueAtMillis,
                        hasTime = item.hasTime,
                        snoozedUntilMillis = item.snoozedUntilMillis,
                        priority = item.priority.level,
                        isFlagged = item.isFlagged,
                        recurrenceRule = item.recurrenceRule,
                        isCompleted = item.isCompleted,
                        completedAtMillis = item.completedAtMillis,
                        createdAtMillis = item.createdAtMillis,
                        updatedAtMillis = item.updatedAtMillis
                    )
                )
                if (item.subtasks.isNotEmpty()) {
                    dao.insertSubtasks(
                        item.subtasks.mapIndexed { idx, sub ->
                            SubtaskEntity(
                                reminderId = newReminderId,
                                title = sub.title,
                                isCompleted = sub.isCompleted,
                                position = idx
                            )
                        }
                    )
                }
            }
        }

        if (payload.lists.isEmpty()) {
            database.seedDefaultListsIfEmpty()
        }

        rescheduleAllActiveAlarms()
        onWidgetRefresh()
        return payload.lists.size to payload.reminders.size
    }

    suspend fun rescheduleAllActiveAlarms() {
        val cutoff = System.currentTimeMillis() - 5 * 60_000L
        val active = dao.getActiveScheduledReminders().map { it.toDomain() }
        for (reminder in active) {
            val effective = reminder.effectiveDueMillis ?: continue
            if (effective >= cutoff) {
                alarmScheduler.schedule(reminder)
            }
        }
    }

    private fun ReminderListEntity.toDomain(): ReminderListItem = ReminderListItem(
        id = id,
        name = name,
        iconKey = iconKey,
        colorHex = colorHex,
        sortOrder = sortOrder
    )

    private fun ReminderWithSubtasks.toDomain(): ReminderItem = ReminderItem(
        id = reminder.id,
        listId = reminder.listId,
        title = reminder.title,
        notes = reminder.notes,
        dueAtMillis = reminder.dueAtMillis,
        hasTime = reminder.hasTime,
        snoozedUntilMillis = reminder.snoozedUntilMillis,
        priority = Priority.fromLevel(reminder.priority),
        isFlagged = reminder.isFlagged,
        recurrenceRule = reminder.recurrenceRule,
        isCompleted = reminder.isCompleted,
        completedAtMillis = reminder.completedAtMillis,
        createdAtMillis = reminder.createdAtMillis,
        updatedAtMillis = reminder.updatedAtMillis,
        subtasks = subtasks
            .sortedBy { it.position }
            .map { sub ->
                SubtaskItem(
                    id = sub.id,
                    reminderId = sub.reminderId,
                    title = sub.title,
                    isCompleted = sub.isCompleted,
                    position = sub.position
                )
            }
    )

    private fun ReminderItem.toEntity(): ReminderEntity = ReminderEntity(
        id = id,
        listId = listId,
        title = title,
        notes = notes,
        dueAtMillis = dueAtMillis,
        hasTime = hasTime,
        snoozedUntilMillis = snoozedUntilMillis,
        priority = priority.level,
        isFlagged = isFlagged,
        recurrenceRule = recurrenceRule,
        isCompleted = isCompleted,
        completedAtMillis = completedAtMillis,
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis
    )
}
