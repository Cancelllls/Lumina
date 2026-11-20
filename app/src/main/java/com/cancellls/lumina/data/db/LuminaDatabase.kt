package com.cancellls.lumina.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ReminderListEntity::class,
        ReminderEntity::class,
        SubtaskEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class LuminaDatabase : RoomDatabase() {

    abstract fun luminaDao(): LuminaDao

    suspend fun seedDefaultListsIfEmpty() {
        val dao = luminaDao()
        if (dao.getListCount() == 0) {
            dao.insertList(
                ReminderListEntity(
                    name = "Personal",
                    iconKey = "home",
                    colorHex = 0xFF14B8A6,
                    sortOrder = 0
                )
            )
            dao.insertList(
                ReminderListEntity(
                    name = "Work",
                    iconKey = "work",
                    colorHex = 0xFF3B82F6,
                    sortOrder = 1
                )
            )
            dao.insertList(
                ReminderListEntity(
                    name = "Shopping",
                    iconKey = "shopping",
                    colorHex = 0xFFF59E0B,
                    sortOrder = 2
                )
            )
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: LuminaDatabase? = null

        fun getInstance(context: Context): LuminaDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    LuminaDatabase::class.java,
                    "lumina_reminders.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
