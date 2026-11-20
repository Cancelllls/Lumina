package com.cancellls.lumina.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class NaturalDateParserTest {

    private val zone = ZoneId.of("UTC")
    // Friday, Nov 20, 2026 at 10:00 UTC
    private val fixedNow = ZonedDateTime.of(2026, 11, 20, 10, 0, 0, 0, zone)

    @Test
    fun `parses tomorrow with explicit 12h time`() {
        val result = NaturalDateParser.parse("Pay rent tomorrow 9am", fixedNow)
        assertEquals("Pay rent", result.cleanTitle)
        assertTrue(result.hasTime)
        val zdt = Instant.ofEpochMilli(result.dueAtMillis!!).atZone(zone)
        assertEquals(LocalDate.of(2026, 11, 21), zdt.toLocalDate())
        assertEquals(9, zdt.hour)
        assertEquals(0, zdt.minute)
    }

    @Test
    fun `parses relative minutes and hours`() {
        val mins = NaturalDateParser.parse("Check oven in 30m", fixedNow)
        assertEquals("Check oven", mins.cleanTitle)
        assertTrue(mins.hasTime)
        assertEquals(fixedNow.plusMinutes(30).toInstant().toEpochMilli(), mins.dueAtMillis)

        val hours = NaturalDateParser.parse("Take medicine in 2 hours", fixedNow)
        assertEquals("Take medicine", hours.cleanTitle)
        assertTrue(hours.hasTime)
        assertEquals(fixedNow.plusHours(2).toInstant().toEpochMilli(), hours.dueAtMillis)
    }

    @Test
    fun `parses today 24h time and tonight`() {
        val today = NaturalDateParser.parse("Standup today at 14:30", fixedNow)
        assertEquals("Standup", today.cleanTitle)
        assertTrue(today.hasTime)
        val todayZdt = Instant.ofEpochMilli(today.dueAtMillis!!).atZone(zone)
        assertEquals(LocalDate.of(2026, 11, 20), todayZdt.toLocalDate())
        assertEquals(14, todayZdt.hour)
        assertEquals(30, todayZdt.minute)

        val tonight = NaturalDateParser.parse("Read book tonight", fixedNow)
        assertEquals("Read book", tonight.cleanTitle)
        assertTrue(tonight.hasTime)
        val tonightZdt = Instant.ofEpochMilli(tonight.dueAtMillis!!).atZone(zone)
        assertEquals(LocalDate.of(2026, 11, 20), tonightZdt.toLocalDate())
        assertEquals(20, tonightZdt.hour)
    }

    @Test
    fun `parses next weekday without time defaulting to 9am`() {
        val result = NaturalDateParser.parse("Submit report next monday", fixedNow)
        assertEquals("Submit report", result.cleanTitle)
        assertFalse(result.hasTime)
        val zdt = Instant.ofEpochMilli(result.dueAtMillis!!).atZone(zone)
        assertEquals(DayOfWeek.MONDAY, zdt.dayOfWeek)
        assertEquals(LocalDate.of(2026, 11, 23), zdt.toLocalDate())
        assertEquals(9, zdt.hour)
    }

    @Test
    fun `parses recurrence rules and custom intervals`() {
        val weekdays = NaturalDateParser.parse("Morning stretch every weekday 8am", fixedNow)
        assertEquals("Morning stretch", weekdays.cleanTitle)
        assertEquals(RecurrenceEngine.WEEKDAYS, weekdays.recurrenceRule)
        assertTrue(weekdays.hasTime)
        assertEquals(8, Instant.ofEpochMilli(weekdays.dueAtMillis!!).atZone(zone).hour)

        val every3 = NaturalDateParser.parse("Water plants every 3 days", fixedNow)
        assertEquals("Water plants", every3.cleanTitle)
        assertEquals("EVERY_N_DAYS:3", every3.recurrenceRule)
        assertNotNull(every3.dueAtMillis)
    }

    @Test
    fun `parses iso date priority markers and flag`() {
        val high = NaturalDateParser.parse("Renew passport 2026-12-01 !!! #flag", fixedNow)
        assertEquals("Renew passport", high.cleanTitle)
        assertEquals(Priority.HIGH, high.priority)
        assertTrue(high.isFlagged)
        val zdt = Instant.ofEpochMilli(high.dueAtMillis!!).atZone(zone)
        assertEquals(LocalDate.of(2026, 12, 1), zdt.toLocalDate())

        val med = NaturalDateParser.parse("Email invoice !!", fixedNow)
        assertEquals("Email invoice", med.cleanTitle)
        assertEquals(Priority.MEDIUM, med.priority)

        val low = NaturalDateParser.parse("Clean desk !", fixedNow)
        assertEquals("Clean desk", low.cleanTitle)
        assertEquals(Priority.LOW, low.priority)
    }

    @Test
    fun `preserves plain title and never produces empty title on token-only input`() {
        val plain = NaturalDateParser.parse("Buy fresh mint and lemons", fixedNow)
        assertEquals("Buy fresh mint and lemons", plain.cleanTitle)
        assertNull(plain.dueAtMillis)
        assertEquals(Priority.NONE, plain.priority)
        assertFalse(plain.isFlagged)

        val tokenOnly = NaturalDateParser.parse("tomorrow 9am", fixedNow)
        assertEquals("tomorrow 9am", tokenOnly.cleanTitle)
        assertNotNull(tokenOnly.dueAtMillis)
    }
}
