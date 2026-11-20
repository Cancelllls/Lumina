package com.cancellls.lumina.domain

enum class Priority(val level: Int, val label: String) {
    NONE(0, "None"),
    LOW(1, "Low"),
    MEDIUM(2, "Medium"),
    HIGH(3, "High");

    companion object {
        fun fromLevel(level: Int): Priority = entries.firstOrNull { it.level == level } ?: NONE
    }
}

enum class SmartListType(val title: String) {
    TODAY("Today"),
    SCHEDULED("Scheduled"),
    FLAGGED("Flagged"),
    ALL("All"),
    COMPLETED("Completed")
}

enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    DARK("Obsidian Dark"),
    LIGHT("Crystal Light")
}

enum class GlassPalette(val label: String, val primaryHex: Long, val secondaryHex: Long) {
    AURORA_TEAL("Aurora Teal", 0xFF14B8A6, 0xFF38BDF8),
    COBALT_GLASS("Cobalt Glass", 0xFF3B82F6, 0xFF818CF8),
    AMBER_DUSK("Amber Dusk", 0xFFF59E0B, 0xFFFB7185),
    EMERALD_MIST("Emerald Mist", 0xFF10B981, 0xFF2DD4BF)
}

enum class SortOrder(val label: String) {
    DUE_DATE("Due Date"),
    PRIORITY("Priority"),
    CREATED("Created")
}

data class ReminderListItem(
    val id: Long = 0L,
    val name: String,
    val iconKey: String = "inbox",
    val colorHex: Long = 0xFF14B8A6,
    val sortOrder: Int = 0
)

data class SubtaskItem(
    val id: Long = 0L,
    val reminderId: Long = 0L,
    val title: String,
    val isCompleted: Boolean = false,
    val position: Int = 0
)

data class ReminderItem(
    val id: Long = 0L,
    val listId: Long? = null,
    val title: String,
    val notes: String = "",
    val dueAtMillis: Long? = null,
    val hasTime: Boolean = true,
    val snoozedUntilMillis: Long? = null,
    val priority: Priority = Priority.NONE,
    val isFlagged: Boolean = false,
    val recurrenceRule: String = "",
    val isCompleted: Boolean = false,
    val completedAtMillis: Long? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis(),
    val subtasks: List<SubtaskItem> = emptyList()
) {
    val effectiveDueMillis: Long?
        get() = snoozedUntilMillis ?: dueAtMillis
}

data class SmartCounts(
    val today: Int = 0,
    val scheduled: Int = 0,
    val flagged: Int = 0,
    val all: Int = 0,
    val completed: Int = 0
)
