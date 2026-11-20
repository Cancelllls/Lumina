package com.cancellls.lumina

import android.app.Application
import android.content.Context
import com.cancellls.lumina.alarm.NotificationHelper
import com.cancellls.lumina.alarm.ReminderAlarmScheduler
import com.cancellls.lumina.data.ReminderRepository
import com.cancellls.lumina.data.UserPreferences
import com.cancellls.lumina.data.db.LuminaDatabase
import com.cancellls.lumina.widget.LuminaWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: LuminaDatabase = LuminaDatabase.getInstance(appContext)
    val preferences: UserPreferences = UserPreferences(appContext)
    val alarmScheduler: ReminderAlarmScheduler = ReminderAlarmScheduler(appContext)
    val repository: ReminderRepository = ReminderRepository(
        database = database,
        alarmScheduler = alarmScheduler,
        onWidgetRefresh = { LuminaWidgetProvider.refreshAll(appContext) }
    )
}

class LuminaApplication : Application() {

    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        NotificationHelper.createChannel(this)
        appScope.launch {
            container.database.seedDefaultListsIfEmpty()
            container.repository.rescheduleAllActiveAlarms()
        }
    }
}
