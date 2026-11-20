package com.cancellls.lumina.domain

import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

object RecurrenceEngine {

    const val NONE = ""
    const val DAILY = "DAILY"
    const val WEEKDAYS = "WEEKDAYS"
    const val WEEKLY = "WEEKLY"
    const val MONTHLY = "MONTHLY"
    const val YEARLY = "YEARLY"

    private val customPattern = Regex("^EVERY_N_(DAYS|WEEKS|MONTHS):(\\d+)$")

    fun everyNDays(n: Int): String = "EVERY_N_DAYS:${n.coerceAtLeast(1)}"
    fun everyNWeeks(n: Int): String = "EVERY_N_WEEKS:${n.coerceAtLeast(1)}"
    fun everyNMonths(n: Int): String = "EVERY_N_MONTHS:${n.coerceAtLeast(1)}"

    fun isValidRule(rule: String): Boolean {
        val trimmed = rule.trim()
        return when (trimmed) {
            NONE, DAILY, WEEKDAYS, WEEKLY, MONTHLY, YEARLY -> true
            else -> customPattern.matchEntire(trimmed)?.let {
                (it.groupValues[2].toIntOrNull() ?: 0) >= 1
            } ?: false
        }
    }

    /**
     * Advances [currentDueMillis] according to [rule] while preserving wall-clock time
     * across daylight saving transitions. Guarantees the returned timestamp is strictly
     * greater than both [currentDueMillis] and [nowMillis].
     */
    fun nextOccurrence(
        currentDueMillis: Long,
        rule: String,
        zoneId: ZoneId = ZoneId.systemDefault(),
        nowMillis: Long = System.currentTimeMillis()
    ): Long? {
        val normalized = rule.trim()
        if (normalized.isEmpty() || !isValidRule(normalized)) return null

        var candidate = ZonedDateTime.ofInstant(Instant.ofEpochMilli(currentDueMillis), zoneId)
        val anchorDayOfMonth = candidate.dayOfMonth
        var steps = 0

        do {
            steps += 1
            candidate = stepForward(candidate, normalized, steps, anchorDayOfMonth)
            val candidateMillis = candidate.toInstant().toEpochMilli()
            if (candidateMillis > currentDueMillis && candidateMillis > nowMillis) {
                return candidateMillis
            }
        } while (steps < 2000)

        return null
    }

    private fun stepForward(
        current: ZonedDateTime,
        rule: String,
        stepCount: Int,
        anchorDayOfMonth: Int
    ): ZonedDateTime {
        return when (rule) {
            DAILY -> current.plusDays(1)
            WEEKDAYS -> {
                var next = current.plusDays(1)
                while (next.dayOfWeek == DayOfWeek.SATURDAY || next.dayOfWeek == DayOfWeek.SUNDAY) {
                    next = next.plusDays(1)
                }
                next
            }
            WEEKLY -> current.plusWeeks(1)
            MONTHLY -> {
                val baseMonth = current.plusMonths(1)
                val clampedDay = anchorDayOfMonth.coerceAtMost(baseMonth.toLocalDate().lengthOfMonth())
                baseMonth.withDayOfMonth(clampedDay)
            }
            YEARLY -> current.plusYears(1)
            else -> {
                val match = customPattern.matchEntire(rule) ?: return current.plusDays(1)
                val unit = match.groupValues[1]
                val count = match.groupValues[2].toLong().coerceAtLeast(1L)
                when (unit) {
                    "DAYS" -> current.plusDays(count)
                    "WEEKS" -> current.plusWeeks(count)
                    "MONTHS" -> {
                        val baseMonth = current.plusMonths(count)
                        val clampedDay = anchorDayOfMonth.coerceAtMost(baseMonth.toLocalDate().lengthOfMonth())
                        baseMonth.withDayOfMonth(clampedDay)
                    }
                    else -> current.plusDays(count)
                }
            }
        }
    }

    fun formatLabel(rule: String): String {
        val trimmed = rule.trim()
        return when (trimmed) {
            NONE -> "None"
            DAILY -> "Daily"
            WEEKDAYS -> "Weekdays"
            WEEKLY -> "Weekly"
            MONTHLY -> "Monthly"
            YEARLY -> "Yearly"
            else -> {
                val match = customPattern.matchEntire(trimmed) ?: return "Custom"
                val n = match.groupValues[2].toIntOrNull() ?: 1
                when (match.groupValues[1]) {
                    "DAYS" -> if (n == 1) "Daily" else "Every $n days"
                    "WEEKS" -> if (n == 1) "Weekly" else "Every $n weeks"
                    "MONTHS" -> if (n == 1) "Monthly" else "Every $n months"
                    else -> "Custom"
                }
            }
        }
    }
}
