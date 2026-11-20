package com.cancellls.lumina.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class RecurrenceEngineTest {

    private val zone = ZoneId.of("UTC")

    @Test
    fun `daily and weekly advance preserving wall clock time`() {
        val friday = ZonedDateTime.of(2026, 11, 20, 9, 30, 0, 0, zone)
        val fridayMs = friday.toInstant().toEpochMilli()

        val nextDaily = RecurrenceEngine.nextOccurrence(fridayMs, RecurrenceEngine.DAILY, zone, fridayMs)
        assertNotNull(nextDaily)
        val dailyZdt = Instant.ofEpochMilli(nextDaily!!).atZone(zone)
        assertEquals(LocalDate.of(2026, 11, 21), dailyZdt.toLocalDate())
        assertEquals(9, dailyZdt.hour)
        assertEquals(30, dailyZdt.minute)

        val nextWeekly = RecurrenceEngine.nextOccurrence(fridayMs, RecurrenceEngine.WEEKLY, zone, fridayMs)
        val weeklyZdt = Instant.ofEpochMilli(nextWeekly!!).atZone(zone)
        assertEquals(LocalDate.of(2026, 11, 27), weeklyZdt.toLocalDate())
    }

    @Test
    fun `weekdays skips weekend from friday to monday`() {
        val friday = ZonedDateTime.of(2026, 11, 20, 8, 0, 0, 0, zone).toInstant().toEpochMilli()
        val next = RecurrenceEngine.nextOccurrence(friday, RecurrenceEngine.WEEKDAYS, zone, friday)!!
        val nextZdt = Instant.ofEpochMilli(next).atZone(zone)
        assertEquals(DayOfWeek.MONDAY, nextZdt.dayOfWeek)
        assertEquals(LocalDate.of(2026, 11, 23), nextZdt.toLocalDate())
    }

    @Test
    fun `monthly clamps end of month dates cleanly`() {
        val jan31 = ZonedDateTime.of(2026, 1, 31, 10, 0, 0, 0, zone).toInstant().toEpochMilli()
        val feb = RecurrenceEngine.nextOccurrence(jan31, RecurrenceEngine.MONTHLY, zone, jan31)!!
        val febZdt = Instant.ofEpochMilli(feb).atZone(zone)
        assertEquals(LocalDate.of(2026, 2, 28), febZdt.toLocalDate())
    }

    @Test
    fun `yearly and custom intervals advance accurately`() {
        val base = ZonedDateTime.of(2026, 5, 10, 12, 0, 0, 0, zone).toInstant().toEpochMilli()

        val yearly = Instant.ofEpochMilli(
            RecurrenceEngine.nextOccurrence(base, RecurrenceEngine.YEARLY, zone, base)!!
        ).atZone(zone)
        assertEquals(LocalDate.of(2027, 5, 10), yearly.toLocalDate())

        val every3Days = Instant.ofEpochMilli(
            RecurrenceEngine.nextOccurrence(base, RecurrenceEngine.everyNDays(3), zone, base)!!
        ).atZone(zone)
        assertEquals(LocalDate.of(2026, 5, 13), every3Days.toLocalDate())

        val every2Weeks = Instant.ofEpochMilli(
            RecurrenceEngine.nextOccurrence(base, RecurrenceEngine.everyNWeeks(2), zone, base)!!
        ).atZone(zone)
        assertEquals(LocalDate.of(2026, 5, 24), every2Weeks.toLocalDate())
    }

    @Test
    fun `overdue recurring reminder jumps strictly past nowMillis`() {
        val oldDue = ZonedDateTime.of(2026, 11, 1, 9, 0, 0, 0, zone).toInstant().toEpochMilli()
        val now = ZonedDateTime.of(2026, 11, 20, 10, 0, 0, 0, zone).toInstant().toEpochMilli()

        val next = RecurrenceEngine.nextOccurrence(oldDue, RecurrenceEngine.DAILY, zone, now)!!
        assertTrue(next > now)
        val nextZdt = Instant.ofEpochMilli(next).atZone(zone)
        assertEquals(LocalDate.of(2026, 11, 21), nextZdt.toLocalDate())
    }

    @Test
    fun `empty or invalid rule returns null`() {
        val now = 1_700_000_000_000L
        assertNull(RecurrenceEngine.nextOccurrence(now, "", zone, now))
        assertNull(RecurrenceEngine.nextOccurrence(now, "INVALID_RULE", zone, now))
    }
}
