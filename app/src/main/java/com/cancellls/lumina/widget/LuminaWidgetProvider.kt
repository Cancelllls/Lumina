package com.cancellls.lumina.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.cancellls.lumina.LuminaApplication
import com.cancellls.lumina.MainActivity
import com.cancellls.lumina.R
import com.cancellls.lumina.alarm.NotificationHelper
import com.cancellls.lumina.alarm.ReminderActionReceiver
import com.cancellls.lumina.domain.ReminderItem
import com.cancellls.lumina.domain.SmartListFilter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class LuminaWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        if (appWidgetIds.isEmpty()) return

        val pendingResult = goAsync()
        val app = context.applicationContext as LuminaApplication

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val activeReminders = app.container.repository.getActiveRemindersSnapshot()
                val now = System.currentTimeMillis()
                val sorted = SmartListFilter.sortReminders(
                    activeReminders,
                    com.cancellls.lumina.domain.SortOrder.DUE_DATE
                )
                val views = buildRemoteViews(context, sorted, now)
                for (widgetId in appWidgetIds) {
                    appWidgetManager.updateAppWidget(widgetId, views)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun buildRemoteViews(
        context: Context,
        activeReminders: List<ReminderItem>,
        nowMillis: Long
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_lumina)

        // Header badge & Quick Add
        val countLabel = if (activeReminders.isEmpty()) "0" else "${activeReminders.size}"
        views.setTextViewText(R.id.widget_count_badge, countLabel)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPending = PendingIntent.getActivity(
            context,
            101,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_title, openAppPending)

        val quickAddIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(NotificationHelper.EXTRA_OPEN_QUICK_ADD, true)
        }
        val quickAddPending = PendingIntent.getActivity(
            context,
            102,
            quickAddIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_add_button, quickAddPending)

        val displayItems = activeReminders.take(ROW_IDS.size)
        if (displayItems.isEmpty()) {
            views.setViewVisibility(R.id.widget_empty_text, View.VISIBLE)
        } else {
            views.setViewVisibility(R.id.widget_empty_text, View.GONE)
        }

        val overdueColor = ContextCompat.getColor(context, R.color.widget_overdue_color)
        val normalSubColor = ContextCompat.getColor(context, R.color.widget_subtitle_color)

        for (i in ROW_IDS.indices) {
            if (i < displayItems.size) {
                val item = displayItems[i]
                views.setViewVisibility(ROW_IDS[i], View.VISIBLE)
                views.setTextViewText(TITLE_IDS[i], item.title)

                val dueMillis = item.effectiveDueMillis
                val dueLabel = formatShortDue(dueMillis, item.hasTime)
                views.setTextViewText(TIME_IDS[i], dueLabel)
                val isOverdue = SmartListFilter.isOverdue(item, nowMillis)
                views.setTextColor(TIME_IDS[i], if (isOverdue) overdueColor else normalSubColor)

                val completeIntent = Intent(context, ReminderActionReceiver::class.java).apply {
                    action = ReminderActionReceiver.ACTION_WIDGET_COMPLETE
                    data = Uri.parse("lumina://widget/complete/${item.id}")
                    putExtra(NotificationHelper.EXTRA_REMINDER_ID, item.id)
                }
                val completePending = PendingIntent.getBroadcast(
                    context,
                    40_000 + item.id.toInt(),
                    completeIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(CHECK_IDS[i], completePending)

                val rowOpenIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    data = Uri.parse("lumina://widget/open/${item.id}")
                    putExtra(NotificationHelper.EXTRA_REMINDER_ID, item.id)
                }
                val rowOpenPending = PendingIntent.getActivity(
                    context,
                    50_000 + item.id.toInt(),
                    rowOpenIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(ROW_IDS[i], rowOpenPending)
            } else {
                views.setViewVisibility(ROW_IDS[i], View.GONE)
            }
        }

        return views
    }

    private fun formatShortDue(dueMillis: Long?, hasTime: Boolean): String {
        if (dueMillis == null) return ""
        val zone = ZoneId.systemDefault()
        val zdt = Instant.ofEpochMilli(dueMillis).atZone(zone)
        val today = LocalDate.now(zone)
        val date = zdt.toLocalDate()
        val dayStr = when (date) {
            today -> "Today"
            today.plusDays(1) -> "Tomorrow"
            today.minusDays(1) -> "Yesterday"
            else -> date.format(DATE_FMT)
        }
        return if (hasTime) {
            if (date == today) zdt.format(TIME_FMT) else "$dayStr ${zdt.format(TIME_FMT)}"
        } else {
            dayStr
        }
    }

    companion object {
        private val TIME_FMT = DateTimeFormatter.ofPattern("h:mm a")
        private val DATE_FMT = DateTimeFormatter.ofPattern("MMM d")

        private val ROW_IDS = intArrayOf(
            R.id.row_0, R.id.row_1, R.id.row_2, R.id.row_3, R.id.row_4
        )
        private val CHECK_IDS = intArrayOf(
            R.id.row_check_0, R.id.row_check_1, R.id.row_check_2, R.id.row_check_3, R.id.row_check_4
        )
        private val TITLE_IDS = intArrayOf(
            R.id.row_title_0, R.id.row_title_1, R.id.row_title_2, R.id.row_title_3, R.id.row_title_4
        )
        private val TIME_IDS = intArrayOf(
            R.id.row_time_0, R.id.row_time_1, R.id.row_time_2, R.id.row_time_3, R.id.row_time_4
        )

        fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, LuminaWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            if (ids.isEmpty()) return

            val intent = Intent(context, LuminaWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(intent)
        }
    }
}
