package com.daydot.lockscreen.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ReminderEntity::class],
    version = 1,
    exportSchema = false
)
abstract class DayDotDatabase : RoomDatabase() {

    abstract fun reminderDao(): ReminderDao

    companion object {
        @Volatile
        private var INSTANCE: DayDotDatabase? = null

        fun getInstance(context: Context): DayDotDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    DayDotDatabase::class.java,
                    "daydot_reminders.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
