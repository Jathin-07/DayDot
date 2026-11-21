package com.daydot.lockscreen.scheduling

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.daydot.lockscreen.DayDotApplication
import java.time.LocalDate

class DailyRefreshWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? DayDotApplication ?: return Result.success()
        val settings = app.appearanceRepository.getCurrentSettings()
        if (settings.autoApplyLockScreen) {
            app.wallpaperManager.renderAndApplyLockScreen(LocalDate.now())
        }
        AlarmScheduler.scheduleMidnightRefresh(applicationContext)
        return Result.success()
    }
}
