package com.cancellls.lumina.data.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "reminder_lists")
data class ReminderListEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val iconKey: String,
    val colorHex: Long,
    val sortOrder: Int
)

@Entity(
    tableName = "reminders",
    foreignKeys = [
        ForeignKey(
            entity = ReminderListEntity::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["listId"]),
        Index(value = ["dueAtMillis"]),
        Index(value = ["isCompleted"])
    ]
)
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val listId: Long? = null,
    val title: String,
    val notes: String = "",
    val dueAtMillis: Long? = null,
    val hasTime: Boolean = true,
    val snoozedUntilMillis: Long? = null,
    val priority: Int = 0,
    val isFlagged: Boolean = false,
    val recurrenceRule: String = "",
    val isCompleted: Boolean = false,
    val completedAtMillis: Long? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "subtasks",
    foreignKeys = [
        ForeignKey(
            entity = ReminderEntity::class,
            parentColumns = ["id"],
            childColumns = ["reminderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["reminderId"])
    ]
)
data class SubtaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val reminderId: Long,
    val title: String,
    val isCompleted: Boolean = false,
    val position: Int = 0
)

data class ReminderWithSubtasks(
    @Embedded val reminder: ReminderEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "reminderId"
    )
    val subtasks: List<SubtaskEntity>
)
