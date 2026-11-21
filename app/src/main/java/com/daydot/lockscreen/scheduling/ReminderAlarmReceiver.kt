package com.daydot.lockscreen.scheduling

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.daydot.lockscreen.DayDotApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

class ReminderAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as? DayDotApplication ?: return
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    AlarmScheduler.ACTION_MIDNIGHT_REFRESH -> {
                        val settings = app.appearanceRepository.getCurrentSettings()
                        if (settings.autoApplyLockScreen) {
                            app.wallpaperManager.renderAndApplyLockScreen(LocalDate.now())
                        }
                        AlarmScheduler.scheduleMidnightRefresh(context)
                    }
                    AlarmScheduler.ACTION_REMINDER_ALARM -> {
                        val dateIso = intent.getStringExtra(AlarmScheduler.EXTRA_DATE_ISO)
                        if (!dateIso.isNullOrBlank()) {
                            val reminder = app.reminderRepository.getReminderForDate(
                                LocalDate.parse(dateIso)
                            )
                            if (reminder != null) {
                                ReminderNotificationHelper.showReminderNotification(context, reminder)
                            }
                        }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
