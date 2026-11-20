package com.cancellls.lumina.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class SmartListFilterTest {

    private val zone = ZoneId.of("UTC")
    private val nowZdt = ZonedDateTime.of(2026, 11, 20, 12, 0, 0, 0, zone)
    private val nowMs = nowZdt.toInstant().toEpochMilli()

    private val sampleReminders = listOf(
        ReminderItem(
            id = 1,
            listId = 10,
            title = "Overdue tax check",
            dueAtMillis = nowZdt.minusHours(3).toInstant().toEpochMilli(),
            priority = Priority.HIGH,
            isFlagged = true
        ),
        ReminderItem(
            id = 2,
            listId = 10,
            title = "Evening workout",
            notes = "Leg day + hydration",
            dueAtMillis = nowZdt.plusHours(5).toInstant().toEpochMilli(),
            subtasks = listOf(SubtaskItem(id = 1, title = "Pack gym bottle"))
        ),
        ReminderItem(
            id = 3,
            listId = 20,
            title = "Book flights",
            dueAtMillis = nowZdt.plusDays(2).toInstant().toEpochMilli(),
            isFlagged = true
        ),
        ReminderItem(
            id = 4,
            listId = 20,
            title = "Someday idea",
            dueAtMillis = null
        ),
        ReminderItem(
            id = 5,
            listId = 10,
            title = "Old finished item",
            dueAtMillis = nowZdt.minusDays(1).toInstant().toEpochMilli(),
            isCompleted = true,
            completedAtMillis = nowMs - 10_000L
        ),
        ReminderItem(
            id = 6,
            listId = 10,
            title = "Newer finished item",
            isCompleted = true,
            completedAtMillis = nowMs - 1_000L
        )
    )

    @Test
    fun `smart counts categorize active and completed reminders accurately`() {
        val counts = SmartListFilter.computeCounts(sampleReminders, nowMs, zone)
        assertEquals(2, counts.today)
        assertEquals(3, counts.scheduled)
        assertEquals(2, counts.flagged)
        assertEquals(4, counts.all)
        assertEquals(2, counts.completed)
    }

    @Test
    fun `today filter includes overdue and today but excludes tomorrow and completed`() {
        val today = SmartListFilter.filterSmartList(sampleReminders, SmartListType.TODAY, SortOrder.DUE_DATE, nowMs, zone)
        assertEquals(listOf(1L, 2L), today.map { it.id })
    }

    @Test
    fun `completed filter sorts reverse chronologically`() {
        val completed = SmartListFilter.filterSmartList(sampleReminders, SmartListType.COMPLETED, SortOrder.DUE_DATE, nowMs, zone)
        assertEquals(listOf(6L, 5L), completed.map { it.id })
    }

    @Test
    fun `search matches title notes subtasks and list names`() {
        val listNames = mapOf(10L to "Personal", 20L to "Travel")
        assertEquals(listOf(2L), SmartListFilter.search(sampleReminders, listNames, "hydration").map { it.id })
        assertEquals(listOf(2L), SmartListFilter.search(sampleReminders, listNames, "bottle").map { it.id })
        assertEquals(listOf(3L, 4L), SmartListFilter.search(sampleReminders, listNames, "travel").map { it.id })
    }
}
