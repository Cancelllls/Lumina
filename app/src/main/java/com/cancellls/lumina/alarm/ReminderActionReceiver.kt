package com.cancellls.lumina.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.cancellls.lumina.LuminaApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(NotificationHelper.EXTRA_REMINDER_ID, -1L)
        if (reminderId <= 0L) return

        val action = intent.action ?: return
        val pendingResult = goAsync()
        val app = context.applicationContext as LuminaApplication

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = app.container.repository
                when (action) {
                    ACTION_COMPLETE, ACTION_WIDGET_COMPLETE -> {
                        repository.toggleComplete(reminderId)
                        NotificationHelper.cancelNotification(context, reminderId)
                    }
                    ACTION_SNOZE_15 -> {
                        repository.snoozeReminder(reminderId, 15)
                        NotificationHelper.cancelNotification(context, reminderId)
                    }
                    ACTION_SNOZE_60 -> {
                        repository.snoozeReminder(reminderId, 60)
                        NotificationHelper.cancelNotification(context, reminderId)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_COMPLETE = "com.cancellls.lumina.ACTION_COMPLETE"
        const val ACTION_SNOZE_15 = "com.cancellls.lumina.ACTION_SNOZE_15"
        const val ACTION_SNOZE_60 = "com.cancellls.lumina.ACTION_SNOZE_60"
        const val ACTION_WIDGET_COMPLETE = "com.cancellls.lumina.ACTION_WIDGET_COMPLETE"
    }
}
