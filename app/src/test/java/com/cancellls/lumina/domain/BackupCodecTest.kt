package com.cancellls.lumina.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCodecTest {

    @Test
    fun `encode and decode preserves lists reminders recurrence and subtasks`() {
        val payload = BackupPayload(
            exportedAtMillis = 1_795_000_000_000L,
            lists = listOf(
                ReminderListItem(id = 1, name = "Personal", iconKey = "home", colorHex = 0xFF14B8A6, sortOrder = 0),
                ReminderListItem(id = 2, name = "Work", iconKey = "work", colorHex = 0xFF3B82F6, sortOrder = 1)
            ),
            reminders = listOf(
                ReminderItem(
                    id = 10,
                    listId = 2,
                    title = "Weekly sync",
                    notes = "Prepare architecture notes",
                    dueAtMillis = 1_795_100_000_000L,
                    hasTime = true,
                    snoozedUntilMillis = null,
                    priority = Priority.HIGH,
                    isFlagged = true,
                    recurrenceRule = RecurrenceEngine.WEEKLY,
                    isCompleted = false,
                    completedAtMillis = null,
                    createdAtMillis = 1_795_000_000_000L,
                    updatedAtMillis = 1_795_000_100_000L,
                    subtasks = listOf(
                        SubtaskItem(id = 100, reminderId = 10, title = "Draft slides", isCompleted = true, position = 0),
                        SubtaskItem(id = 101, reminderId = 10, title = "Review metrics", isCompleted = false, position = 1)
                    )
                )
            )
        )

        val encoded = BackupCodec.encode(payload)
        val decoded = BackupCodec.decode(encoded)

        assertEquals(payload.schemaVersion, decoded.schemaVersion)
        assertEquals(payload.lists, decoded.lists)
        assertEquals(1, decoded.reminders.size)

        val r = decoded.reminders.first()
        assertEquals("Weekly sync", r.title)
        assertEquals("Prepare architecture notes", r.notes)
        assertEquals(2L, r.listId)
        assertEquals(Priority.HIGH, r.priority)
        assertTrue(r.isFlagged)
        assertEquals(RecurrenceEngine.WEEKLY, r.recurrenceRule)
        assertEquals(2, r.subtasks.size)
        assertEquals("Draft slides", r.subtasks[0].title)
        assertTrue(r.subtasks[0].isCompleted)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `decode rejects corrupt json`() {
        BackupCodec.decode("{ not valid json")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `decode rejects unsupported future schema version`() {
        BackupCodec.decode("""{"schemaVersion": 999, "lists": [], "reminders": []}""")
    }
}
