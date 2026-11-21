package com.daydot.lockscreen

import android.app.Application
import com.daydot.lockscreen.data.ReminderRepository
import com.daydot.lockscreen.data.db.DayDotDatabase
import com.daydot.lockscreen.data.prefs.AppearanceRepository
import com.daydot.lockscreen.scheduling.AlarmScheduler
import com.daydot.lockscreen.scheduling.ReminderNotificationHelper
import com.daydot.lockscreen.wallpaper.LockScreenWallpaperManager

class DayDotApplication : Application() {

    lateinit var database: DayDotDatabase
        private set

    lateinit var reminderRepository: ReminderRepository
        private set

    lateinit var appearanceRepository: AppearanceRepository
        private set

    lateinit var wallpaperManager: LockScreenWallpaperManager
        private set

    override fun onCreate() {
        super.onCreate()
        database = DayDotDatabase.getInstance(this)
        reminderRepository = ReminderRepository(database.reminderDao())
        appearanceRepository = AppearanceRepository(this)
        wallpaperManager = LockScreenWallpaperManager(
            context = this,
            reminderRepository = reminderRepository,
            appearanceRepository = appearanceRepository
        )
        ReminderNotificationHelper.ensureChannel(this)
        AlarmScheduler.scheduleMidnightRefresh(this)
    }
}
