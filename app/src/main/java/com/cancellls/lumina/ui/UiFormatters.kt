package com.cancellls.lumina.ui

import com.cancellls.lumina.domain.ReminderItem
import com.cancellls.lumina.domain.SmartListFilter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object UiFormatters {
    private val timeFmt = DateTimeFormatter.ofPattern("h:mm a")
    private val shortDateFmt = DateTimeFormatter.ofPattern("MMM d")
    private val fullDateFmt = DateTimeFormatter.ofPattern("EEE, MMM d")
    private val headerDateFmt = DateTimeFormatter.ofPattern("EEEE, MMM d")

    fun formatHeaderDate(zoneId: ZoneId = ZoneId.systemDefault()): String =
        LocalDate.now(zoneId).format(headerDateFmt)

    fun formatDueChip(
        dueMillis: Long?,
        hasTime: Boolean,
        isSnoozed: Boolean = false,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): String {
        if (dueMillis == null) return ""
        val zdt = Instant.ofEpochMilli(dueMillis).atZone(zoneId)
        val today = LocalDate.now(zoneId)
        val date = zdt.toLocalDate()
        val prefix = if (isSnoozed) "Snoozed " else ""
        val dayPart = when (date) {
            today -> "Today"
            today.plusDays(1) -> "Tomorrow"
            today.minusDays(1) -> "Yesterday"
            else -> date.format(shortDateFmt)
        }
        return if (hasTime) {
            if (date == today && !isSnoozed) {
                zdt.format(timeFmt)
            } else {
                "$prefix$dayPart, ${zdt.format(timeFmt)}"
            }
        } else {
            "$prefix$dayPart"
        }
    }

    fun formatDateOnly(millis: Long, zoneId: ZoneId = ZoneId.systemDefault()): String =
        Instant.ofEpochMilli(millis).atZone(zoneId).toLocalDate().format(fullDateFmt)

    fun formatTimeOnly(millis: Long, zoneId: ZoneId = ZoneId.systemDefault()): String =
        Instant.ofEpochMilli(millis).atZone(zoneId).toLocalTime().format(timeFmt)

    fun isOverdue(item: ReminderItem, nowMillis: Long = System.currentTimeMillis()): Boolean =
        SmartListFilter.isOverdue(item, nowMillis)
}
