package com.cancellls.lumina.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.cancellls.lumina.LuminaApplication
import com.cancellls.lumina.widget.LuminaWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val app = context.applicationContext as LuminaApplication

        CoroutineScope(Dispatchers.IO).launch {
            try {
                app.container.repository.rescheduleAllActiveAlarms()
                LuminaWidgetProvider.refreshAll(context)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
