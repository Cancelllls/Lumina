package com.cancellls.lumina.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LuminaDao {

    @Query("SELECT * FROM reminder_lists ORDER BY sortOrder ASC, id ASC")
    fun observeLists(): Flow<List<ReminderListEntity>>

    @Query("SELECT * FROM reminder_lists ORDER BY sortOrder ASC, id ASC")
    suspend fun getAllListsOnce(): List<ReminderListEntity>

    @Query("SELECT COUNT(*) FROM reminder_lists")
    suspend fun getListCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertList(entity: ReminderListEntity): Long

    @Update
    suspend fun updateList(entity: ReminderListEntity)

    @Query("DELETE FROM reminder_lists WHERE id = :listId")
    suspend fun deleteListById(listId: Long)

    @Transaction
    @Query("SELECT * FROM reminders ORDER BY isCompleted ASC, COALESCE(snoozedUntilMillis, dueAtMillis, 9223372036854775807) ASC, priority DESC, createdAtMillis DESC")
    fun observeAllRemindersWithSubtasks(): Flow<List<ReminderWithSubtasks>>

    @Transaction
    @Query("SELECT * FROM reminders ORDER BY id ASC")
    suspend fun getAllRemindersWithSubtasksOnce(): List<ReminderWithSubtasks>

    @Transaction
    @Query("SELECT * FROM reminders WHERE id = :reminderId LIMIT 1")
    suspend fun getReminderWithSubtasks(reminderId: Long): ReminderWithSubtasks?

    @Transaction
    @Query("SELECT * FROM reminders WHERE isCompleted = 0 AND COALESCE(snoozedUntilMillis, dueAtMillis) IS NOT NULL")
    suspend fun getActiveScheduledReminders(): List<ReminderWithSubtasks>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(entity: ReminderEntity): Long

    @Update
    suspend fun updateReminder(entity: ReminderEntity)

    @Query("DELETE FROM reminders WHERE id = :reminderId")
    suspend fun deleteReminderById(reminderId: Long)

    @Query("DELETE FROM reminders WHERE isCompleted = 1")
    suspend fun deleteCompletedReminders()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubtasks(subtasks: List<SubtaskEntity>)

    @Query("SELECT * FROM subtasks WHERE id = :subtaskId LIMIT 1")
    suspend fun getSubtaskById(subtaskId: Long): SubtaskEntity?

    @Update
    suspend fun updateSubtask(subtask: SubtaskEntity)

    @Query("DELETE FROM subtasks WHERE reminderId = :reminderId")
    suspend fun deleteSubtasksForReminder(reminderId: Long)

    @Query("DELETE FROM subtasks")
    suspend fun clearAllSubtasks()

    @Query("DELETE FROM reminders")
    suspend fun clearAllReminders()

    @Query("DELETE FROM reminder_lists")
    suspend fun clearAllLists()
}
