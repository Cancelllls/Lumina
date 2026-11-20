package com.cancellls.lumina.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.Month
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters

data class ParsedReminderInput(
    val cleanTitle: String,
    val dueAtMillis: Long? = null,
    val hasTime: Boolean = false,
    val recurrenceRule: String = RecurrenceEngine.NONE,
    val priority: Priority = Priority.NONE,
    val isFlagged: Boolean = false,
    val matchedTokens: List<String> = emptyList()
)

object NaturalDateParser {

    private val priorityHighRegex = Regex("""(?:^|\s)(!!!)(?=\s|$)""")
    private val priorityMedRegex = Regex("""(?:^|\s)(!!)(?=\s|$)""")
    private val priorityLowRegex = Regex("""(?:^|\s)(!)(?=\s|$)""")
    private val flagRegex = Regex("""(?:^|\s)([#*]flag)(?=\s|$)""", RegexOption.IGNORE_CASE)

    private val everyNRegex = Regex(
        """\bevery\s+(\d{1,3})\s+(days?|weeks?|months?)\b""",
        RegexOption.IGNORE_CASE
    )
    private val everyWeekdayNameRegex = Regex(
        """\bevery\s+(monday|mon|tuesday|tue|tues|wednesday|wed|thursday|thu|thur|thurs|friday|fri|saturday|sat|sunday|sun)\b""",
        RegexOption.IGNORE_CASE
    )
    private val recurrenceKeywordRegex = Regex(
        """\b(every\s+weekday|weekdays|every\s+day|daily|every\s+week|weekly|every\s+month|monthly|every\s+year|yearly)\b""",
        RegexOption.IGNORE_CASE
    )

    private val relativeDurationRegex = Regex(
        """\bin\s+(\d{1,3})\s*(minutes?|mins?|m|hours?|hrs?|hr|h|days?|d)\b""",
        RegexOption.IGNORE_CASE
    )

    private val isoDateRegex = Regex("""\b(20\d{2})-(\d{1,2})-(\d{1,2})\b""")
    private val slashDateRegex = Regex("""\b(\d{1,2})/(\d{1,2})\b""")
    private val monthDayRegex = Regex(
        """\b(january|jan|february|feb|march|mar|april|apr|may|june|jun|july|jul|august|aug|september|sep|sept|october|oct|november|nov|december|dec)\s+(\d{1,2})\b""",
        RegexOption.IGNORE_CASE
    )
    private val dayMonthRegex = Regex(
        """\b(\d{1,2})\s+(january|jan|february|feb|march|mar|april|apr|may|june|jun|july|jul|august|aug|september|sep|sept|october|oct|november|nov|december|dec)\b""",
        RegexOption.IGNORE_CASE
    )
    private val nextWeekdayRegex = Regex(
        """\bnext\s+(monday|mon|tuesday|tue|tues|wednesday|wed|thursday|thu|thur|thurs|friday|fri|saturday|sat|sunday|sun)\b""",
        RegexOption.IGNORE_CASE
    )
    private val relativeDayRegex = Regex(
        """\b(today|tonight|tomorrow|tmr)\b""",
        RegexOption.IGNORE_CASE
    )
    private val weekdayRegex = Regex(
        """\b(monday|mon|tuesday|tue|tues|wednesday|wed|thursday|thu|thur|thurs|friday|fri|saturday|sat|sunday|sun)\b""",
        RegexOption.IGNORE_CASE
    )

    private val amPmTimeRegex = Regex(
        """\b(?:at\s+)?(\d{1,2})(?::(\d{2}))?\s*(am|pm)\b""",
        RegexOption.IGNORE_CASE
    )
    private val twentyFourHourRegex = Regex(
        """\b(?:at\s+)?([01]?\d|2[0-3]):([0-5]\d)\b""",
        RegexOption.IGNORE_CASE
    )
    private val timeOfDayRegex = Regex(
        """\b(?:this\s+|in\s+the\s+)?(morning|noon|afternoon|evening)\b""",
        RegexOption.IGNORE_CASE
    )

    fun parse(rawInput: String, now: ZonedDateTime = ZonedDateTime.now()): ParsedReminderInput {
        var working = rawInput.trim()
        if (working.isEmpty()) return ParsedReminderInput(cleanTitle = "")

        val tokens = mutableListOf<String>()
        var priority = Priority.NONE
        var isFlagged = false
        var recurrenceRule = RecurrenceEngine.NONE
        var targetDate: LocalDate? = null
        var targetTime: LocalTime? = null
        var hasExplicitTime = false
        var relativeExactDateTime: ZonedDateTime? = null
        var requestedWeekday: DayOfWeek? = null

        fun stripFirst(regex: Regex, action: (MatchResult) -> Unit) {
            val match = regex.find(working) ?: return
            action(match)
            tokens += match.value.trim()
            working = (working.substring(0, match.range.first) + " " + working.substring(match.range.last + 1))
                .replace(Regex("\\s{2,}"), " ")
                .trim()
        }

        // 1. Priority & Flag markers
        stripFirst(priorityHighRegex) { priority = Priority.HIGH }
        if (priority == Priority.NONE) stripFirst(priorityMedRegex) { priority = Priority.MEDIUM }
        if (priority == Priority.NONE) stripFirst(priorityLowRegex) { priority = Priority.LOW }
        stripFirst(flagRegex) { isFlagged = true }

        // 2. Recurrence rules
        stripFirst(everyNRegex) { m ->
            val n = m.groupValues[1].toIntOrNull()?.coerceAtLeast(1) ?: 1
            val unit = m.groupValues[2].lowercase()
            recurrenceRule = when {
                unit.startsWith("day") -> RecurrenceEngine.everyNDays(n)
                unit.startsWith("week") -> RecurrenceEngine.everyNWeeks(n)
                else -> RecurrenceEngine.everyNMonths(n)
            }
        }
        if (recurrenceRule.isEmpty()) {
            stripFirst(everyWeekdayNameRegex) { m ->
                recurrenceRule = RecurrenceEngine.WEEKLY
                requestedWeekday = parseWeekday(m.groupValues[1])
            }
        }
        if (recurrenceRule.isEmpty()) {
            stripFirst(recurrenceKeywordRegex) { m ->
                val kw = m.groupValues[1].lowercase().replace(Regex("\\s+"), " ")
                recurrenceRule = when {
                    kw.contains("weekday") -> RecurrenceEngine.WEEKDAYS
                    kw.contains("day") || kw == "daily" -> RecurrenceEngine.DAILY
                    kw.contains("week") || kw == "weekly" -> RecurrenceEngine.WEEKLY
                    kw.contains("month") || kw == "monthly" -> RecurrenceEngine.MONTHLY
                    kw.contains("year") || kw == "yearly" -> RecurrenceEngine.YEARLY
                    else -> RecurrenceEngine.NONE
                }
            }
        }

        // 3. Relative durations ("in 30m", "in 2 hours", "in 3 days")
        stripFirst(relativeDurationRegex) { m ->
            val amount = m.groupValues[1].toLongOrNull()?.coerceAtLeast(1L) ?: 1L
            val unit = m.groupValues[2].lowercase()
            when {
                unit.startsWith("m") -> {
                    relativeExactDateTime = now.plusMinutes(amount).withSecond(0).withNano(0)
                    hasExplicitTime = true
                }
                unit.startsWith("h") -> {
                    relativeExactDateTime = now.plusHours(amount).withSecond(0).withNano(0)
                    hasExplicitTime = true
                }
                unit.startsWith("d") -> {
                    targetDate = now.toLocalDate().plusDays(amount)
                }
            }
        }

        // 4. Dates
        if (relativeExactDateTime == null && targetDate == null) {
            stripFirst(isoDateRegex) { m ->
                runCatching {
                    LocalDate.of(
                        m.groupValues[1].toInt(),
                        m.groupValues[2].toInt(),
                        m.groupValues[3].toInt()
                    )
                }.getOrNull()?.let { targetDate = it }
            }
        }
        if (relativeExactDateTime == null && targetDate == null) {
            stripFirst(monthDayRegex) { m ->
                resolveMonthDay(parseMonth(m.groupValues[1]), m.groupValues[2].toInt(), now.toLocalDate())
                    ?.let { targetDate = it }
            }
        }
        if (relativeExactDateTime == null && targetDate == null) {
            stripFirst(dayMonthRegex) { m ->
                resolveMonthDay(parseMonth(m.groupValues[2]), m.groupValues[1].toInt(), now.toLocalDate())
                    ?.let { targetDate = it }
            }
        }
        if (relativeExactDateTime == null && targetDate == null) {
            stripFirst(slashDateRegex) { m ->
                resolveMonthDay(
                    Month.of(m.groupValues[1].toInt().coerceIn(1, 12)),
                    m.groupValues[2].toInt(),
                    now.toLocalDate()
                )?.let { targetDate = it }
            }
        }
        if (relativeExactDateTime == null && targetDate == null) {
            stripFirst(relativeDayRegex) { m ->
                when (m.groupValues[1].lowercase()) {
                    "today" -> targetDate = now.toLocalDate()
                    "tonight" -> {
                        targetDate = now.toLocalDate()
                        targetTime = LocalTime.of(20, 0)
                        hasExplicitTime = true
                    }
                    "tomorrow", "tmr" -> targetDate = now.toLocalDate().plusDays(1)
                }
            }
        }
        if (relativeExactDateTime == null && targetDate == null) {
            stripFirst(nextWeekdayRegex) { m ->
                parseWeekday(m.groupValues[1])?.let { dow ->
                    targetDate = now.toLocalDate().with(TemporalAdjusters.next(dow))
                }
            }
        }
        if (relativeExactDateTime == null && targetDate == null) {
            if (requestedWeekday != null) {
                targetDate = now.toLocalDate().with(TemporalAdjusters.nextOrSame(requestedWeekday))
            } else {
                stripFirst(weekdayRegex) { m ->
                    parseWeekday(m.groupValues[1])?.let { dow ->
                        requestedWeekday = dow
                        targetDate = now.toLocalDate().with(TemporalAdjusters.nextOrSame(dow))
                    }
                }
            }
        }

        // 5. Time of day
        if (relativeExactDateTime == null) {
            stripFirst(amPmTimeRegex) { m ->
                val rawHour = m.groupValues[1].toIntOrNull() ?: return@stripFirst
                val minute = m.groupValues[2].toIntOrNull() ?: 0
                val isPm = m.groupValues[3].equals("pm", ignoreCase = true)
                if (rawHour in 1..12 && minute in 0..59) {
                    val hour24 = when {
                        isPm && rawHour != 12 -> rawHour + 12
                        !isPm && rawHour == 12 -> 0
                        else -> rawHour
                    }
                    targetTime = LocalTime.of(hour24, minute)
                    hasExplicitTime = true
                }
            }
            if (!hasExplicitTime || targetTime == null) {
                stripFirst(twentyFourHourRegex) { m ->
                    val hour = m.groupValues[1].toIntOrNull() ?: return@stripFirst
                    val minute = m.groupValues[2].toIntOrNull() ?: return@stripFirst
                    targetTime = LocalTime.of(hour, minute)
                    hasExplicitTime = true
                }
            }
            if (targetTime == null) {
                stripFirst(timeOfDayRegex) { m ->
                    targetTime = when (m.groupValues[1].lowercase()) {
                        "morning" -> LocalTime.of(9, 0)
                        "noon" -> LocalTime.of(12, 0)
                        "afternoon" -> LocalTime.of(15, 0)
                        "evening" -> LocalTime.of(18, 0)
                        else -> LocalTime.of(9, 0)
                    }
                    hasExplicitTime = true
                }
            }
        }

        // 6. Compute final timestamp
        val dueAtMillis: Long? = when {
            relativeExactDateTime != null -> relativeExactDateTime!!.toInstant().toEpochMilli()
            targetDate != null || targetTime != null || recurrenceRule.isNotEmpty() -> {
                val resolvedTime = targetTime ?: LocalTime.of(9, 0)
                val resolvedDate = when {
                    targetDate != null -> {
                        val d = targetDate!!
                        if (requestedWeekday != null && d == now.toLocalDate() &&
                            ZonedDateTime.of(d, resolvedTime, now.zone).isBefore(now)
                        ) {
                            d.plusWeeks(1)
                        } else {
                            d
                        }
                    }
                    targetTime != null -> {
                        if (resolvedTime.isAfter(now.toLocalTime())) {
                            now.toLocalDate()
                        } else {
                            now.toLocalDate().plusDays(1)
                        }
                    }
                    else -> {
                        // Recurrence-only input ("Pay rent monthly")
                        if (now.toLocalTime().isBefore(resolvedTime)) {
                            now.toLocalDate()
                        } else {
                            now.toLocalDate().plusDays(1)
                        }
                    }
                }
                ZonedDateTime.of(resolvedDate, resolvedTime, now.zone).toInstant().toEpochMilli()
            }
            else -> null
        }

        val cleanTitle = working.replace(Regex("\\s{2,}"), " ").trim()
            .ifBlank { rawInput.trim() }

        return ParsedReminderInput(
            cleanTitle = cleanTitle,
            dueAtMillis = dueAtMillis,
            hasTime = hasExplicitTime,
            recurrenceRule = recurrenceRule,
            priority = priority,
            isFlagged = isFlagged,
            matchedTokens = tokens
        )
    }

    private fun resolveMonthDay(month: Month?, day: Int, today: LocalDate): LocalDate? {
        if (month == null || day !in 1..31) return null
        return runCatching {
            val candidate = LocalDate.of(today.year, month, day)
            if (candidate.isBefore(today)) candidate.plusYears(1) else candidate
        }.getOrNull()
    }

    private fun parseMonth(token: String): Month? = when (token.lowercase()) {
        "january", "jan" -> Month.JANUARY
        "february", "feb" -> Month.FEBRUARY
        "march", "mar" -> Month.MARCH
        "april", "apr" -> Month.APRIL
        "may" -> Month.MAY
        "june", "jun" -> Month.JUNE
        "july", "jul" -> Month.JULY
        "august", "aug" -> Month.AUGUST
        "september", "sep", "sept" -> Month.SEPTEMBER
        "october", "oct" -> Month.OCTOBER
        "november", "nov" -> Month.NOVEMBER
        "december", "dec" -> Month.DECEMBER
        else -> null
    }

    private fun parseWeekday(token: String): DayOfWeek? = when (token.lowercase()) {
        "monday", "mon" -> DayOfWeek.MONDAY
        "tuesday", "tue", "tues" -> DayOfWeek.TUESDAY
        "wednesday", "wed" -> DayOfWeek.WEDNESDAY
        "thursday", "thu", "thur", "thurs" -> DayOfWeek.THURSDAY
        "friday", "fri" -> DayOfWeek.FRIDAY
        "saturday", "sat" -> DayOfWeek.SATURDAY
        "sunday", "sun" -> DayOfWeek.SUNDAY
        else -> null
    }
}
