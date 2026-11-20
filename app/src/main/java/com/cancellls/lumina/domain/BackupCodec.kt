package com.cancellls.lumina.domain

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

data class BackupPayload(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val exportedAtMillis: Long,
    val lists: List<ReminderListItem>,
    val reminders: List<ReminderItem>
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
    }
}

object BackupCodec {

    fun encode(payload: BackupPayload): String {
        val root = JSONObject()
        root.put("schemaVersion", payload.schemaVersion)
        root.put("exportedAtMillis", payload.exportedAtMillis)

        val listsArr = JSONArray()
        for (list in payload.lists) {
            listsArr.put(
                JSONObject().apply {
                    put("id", list.id)
                    put("name", list.name)
                    put("iconKey", list.iconKey)
                    put("colorHex", list.colorHex)
                    put("sortOrder", list.sortOrder)
                }
            )
        }
        root.put("lists", listsArr)

        val remindersArr = JSONArray()
        for (item in payload.reminders) {
            val subtasksArr = JSONArray()
            for (sub in item.subtasks) {
                subtasksArr.put(
                    JSONObject().apply {
                        put("id", sub.id)
                        put("title", sub.title)
                        put("isCompleted", sub.isCompleted)
                        put("position", sub.position)
                    }
                )
            }
            remindersArr.put(
                JSONObject().apply {
                    put("id", item.id)
                    if (item.listId != null) put("listId", item.listId) else put("listId", JSONObject.NULL)
                    put("title", item.title)
                    put("notes", item.notes)
                    if (item.dueAtMillis != null) put("dueAtMillis", item.dueAtMillis) else put("dueAtMillis", JSONObject.NULL)
                    put("hasTime", item.hasTime)
                    if (item.snoozedUntilMillis != null) put("snoozedUntilMillis", item.snoozedUntilMillis) else put("snoozedUntilMillis", JSONObject.NULL)
                    put("priority", item.priority.level)
                    put("isFlagged", item.isFlagged)
                    put("recurrenceRule", item.recurrenceRule)
                    put("isCompleted", item.isCompleted)
                    if (item.completedAtMillis != null) put("completedAtMillis", item.completedAtMillis) else put("completedAtMillis", JSONObject.NULL)
                    put("createdAtMillis", item.createdAtMillis)
                    put("updatedAtMillis", item.updatedAtMillis)
                    put("subtasks", subtasksArr)
                }
            )
        }
        root.put("reminders", remindersArr)
        return root.toString(2)
    }

    fun decode(json: String): BackupPayload {
        val root = try {
            JSONObject(json)
        } catch (e: JSONException) {
            throw IllegalArgumentException("Invalid JSON backup file.", e)
        }

        val version = root.optInt("schemaVersion", -1)
        if (version !in 1..BackupPayload.CURRENT_SCHEMA_VERSION) {
            throw IllegalArgumentException("Unsupported backup schema version: $version")
        }

        val exportedAt = root.optLong("exportedAtMillis", System.currentTimeMillis())

        val lists = mutableListOf<ReminderListItem>()
        val listsArr = root.optJSONArray("lists") ?: JSONArray()
        for (i in 0 until listsArr.length()) {
            val obj = listsArr.optJSONObject(i) ?: continue
            val name = obj.optString("name", "").trim()
            if (name.isEmpty()) continue
            lists += ReminderListItem(
                id = obj.optLong("id", (i + 1).toLong()),
                name = name,
                iconKey = obj.optString("iconKey", "inbox"),
                colorHex = obj.optLong("colorHex", 0xFF14B8A6),
                sortOrder = obj.optInt("sortOrder", i)
            )
        }

        val reminders = mutableListOf<ReminderItem>()
        val remindersArr = root.optJSONArray("reminders") ?: JSONArray()
        for (i in 0 until remindersArr.length()) {
            val obj = remindersArr.optJSONObject(i) ?: continue
            val title = obj.optString("title", "").trim()
            if (title.isEmpty()) continue

            val subtasks = mutableListOf<SubtaskItem>()
            val subArr = obj.optJSONArray("subtasks") ?: JSONArray()
            for (j in 0 until subArr.length()) {
                val subObj = subArr.optJSONObject(j) ?: continue
                val subTitle = subObj.optString("title", "").trim()
                if (subTitle.isEmpty()) continue
                subtasks += SubtaskItem(
                    id = subObj.optLong("id", 0L),
                    title = subTitle,
                    isCompleted = subObj.optBoolean("isCompleted", false),
                    position = subObj.optInt("position", j)
                )
            }

            reminders += ReminderItem(
                id = obj.optLong("id", 0L),
                listId = obj.optNullableLong("listId"),
                title = title,
                notes = obj.optString("notes", ""),
                dueAtMillis = obj.optNullableLong("dueAtMillis"),
                hasTime = obj.optBoolean("hasTime", true),
                snoozedUntilMillis = obj.optNullableLong("snoozedUntilMillis"),
                priority = Priority.fromLevel(obj.optInt("priority", 0)),
                isFlagged = obj.optBoolean("isFlagged", false),
                recurrenceRule = obj.optString("recurrenceRule", "")
                    .takeIf { RecurrenceEngine.isValidRule(it) } ?: "",
                isCompleted = obj.optBoolean("isCompleted", false),
                completedAtMillis = obj.optNullableLong("completedAtMillis"),
                createdAtMillis = obj.optLong("createdAtMillis", System.currentTimeMillis()),
                updatedAtMillis = obj.optLong("updatedAtMillis", System.currentTimeMillis()),
                subtasks = subtasks
            )
        }

        return BackupPayload(
            schemaVersion = version,
            exportedAtMillis = exportedAt,
            lists = lists,
            reminders = reminders
        )
    }

    private fun JSONObject.optNullableLong(key: String): Long? =
        if (!has(key) || isNull(key)) null else optLong(key)
}
