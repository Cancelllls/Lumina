package com.cancellls.lumina.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.cancellls.lumina.domain.ReminderItem

class ReminderAlarmScheduler(private val context: Context) {

    private val alarmManager: AlarmManager? =
        context.getSystemService(AlarmManager::class.java)

    fun canScheduleExactAlarms(): Boolean {
        val manager = alarmManager ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            manager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun schedule(reminder: ReminderItem) {
        if (reminder.isCompleted) {
            cancel(reminder.id)
            return
        }
        val triggerAtMillis = reminder.effectiveDueMillis ?: run {
            cancel(reminder.id)
            return
        }
        val manager = alarmManager ?: return
        val pendingIntent = buildPendingIntent(reminder.id)

        try {
            if (canScheduleExactAlarms()) {
                manager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                manager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (_: SecurityException) {
            manager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    fun cancel(reminderId: Long) {
        val manager = alarmManager ?: return
        manager.cancel(buildPendingIntent(reminderId))
    }

    private fun buildPendingIntent(reminderId: Long): PendingIntent {
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ACTION_FIRE_REMINDER
            data = Uri.parse("lumina://reminder/$reminderId")
            putExtra(EXTRA_REMINDER_ID, reminderId)
        }
        return PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val ACTION_FIRE_REMINDER = "com.cancellls.lumina.ACTION_FIRE_REMINDER"
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
    }
}
