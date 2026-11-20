package com.cancellls.lumina.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.cancellls.lumina.LuminaApplication
import com.cancellls.lumina.widget.LuminaWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(ReminderAlarmScheduler.EXTRA_REMINDER_ID, -1L)
        if (reminderId <= 0L) return

        val pendingResult = goAsync()
        val app = context.applicationContext as LuminaApplication

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val reminder = app.container.repository.getReminderById(reminderId)
                if (reminder != null && !reminder.isCompleted) {
                    NotificationHelper.showReminderNotification(context, reminder)
                }
                LuminaWidgetProvider.refreshAll(context)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
