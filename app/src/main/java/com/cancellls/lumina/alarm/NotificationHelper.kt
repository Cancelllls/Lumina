package com.cancellls.lumina.alarm

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.cancellls.lumina.MainActivity
import com.cancellls.lumina.R
import com.cancellls.lumina.domain.Priority
import com.cancellls.lumina.domain.RecurrenceEngine
import com.cancellls.lumina.domain.ReminderItem

object NotificationHelper {

    const val CHANNEL_ID = "lumina_reminders"
    const val EXTRA_REMINDER_ID = "extra_reminder_id"
    const val EXTRA_OPEN_QUICK_ADD = "extra_open_quick_add"

    fun createChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val existing = manager.getNotificationChannel(CHANNEL_ID)
        if (existing != null) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.channel_reminders_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.channel_reminders_desc)
            enableVibration(true)
            enableLights(true)
        }
        manager.createNotificationChannel(channel)
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun showReminderNotification(context: Context, reminder: ReminderItem) {
        createChannel(context)
        if (!hasNotificationPermission(context)) return

        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_REMINDER_ID, reminder.id)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            reminder.id.toInt(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val subtitleParts = buildList {
            if (reminder.priority != Priority.NONE) add("${reminder.priority.label} priority")
            if (reminder.recurrenceRule.isNotEmpty()) add(RecurrenceEngine.formatLabel(reminder.recurrenceRule))
            if (reminder.notes.isNotBlank()) add(reminder.notes)
        }
        val contentText = if (subtitleParts.isNotEmpty()) {
            subtitleParts.joinToString(" • ")
        } else {
            "Scheduled reminder"
        }

        val doneIntent = buildActionPendingIntent(
            context,
            reminder.id,
            ReminderActionReceiver.ACTION_COMPLETE,
            reqOffset = 10_000
        )
        val snooze15Intent = buildActionPendingIntent(
            context,
            reminder.id,
            ReminderActionReceiver.ACTION_SNOZE_15,
            reqOffset = 20_000
        )
        val snooze60Intent = buildActionPendingIntent(
            context,
            reminder.id,
            ReminderActionReceiver.ACTION_SNOZE_60,
            reqOffset = 30_000
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(reminder.title)
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(0, "Done", doneIntent)
            .addAction(0, "Snooze 15m", snooze15Intent)
            .addAction(0, "Snooze 1h", snooze60Intent)

        if (reminder.notes.isNotBlank()) {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
        }

        manager.notify(reminder.id.toInt(), builder.build())
    }

    fun cancelNotification(context: Context, reminderId: Long) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.cancel(reminderId.toInt())
    }

    private fun buildActionPendingIntent(
        context: Context,
        reminderId: Long,
        action: String,
        reqOffset: Int
    ): PendingIntent {
        val intent = Intent(context, ReminderActionReceiver::class.java).apply {
            this.action = action
            data = Uri.parse("lumina://action/$action/$reminderId")
            putExtra(EXTRA_REMINDER_ID, reminderId)
        }
        return PendingIntent.getBroadcast(
            context,
            reminderId.toInt() + reqOffset,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
