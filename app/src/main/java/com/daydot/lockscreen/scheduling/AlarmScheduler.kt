package com.daydot.lockscreen.scheduling

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.daydot.lockscreen.data.db.ReminderEntity
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

object AlarmScheduler {

    const val ACTION_MIDNIGHT_REFRESH = "com.daydot.lockscreen.ACTION_MIDNIGHT_REFRESH"
    const val ACTION_REMINDER_ALARM = "com.daydot.lockscreen.ACTION_REMINDER_ALARM"
    const val EXTRA_DATE_ISO = "extra_date_iso"

    private const val MIDNIGHT_REQUEST_CODE = 90001
    private const val PERIODIC_WORK_NAME = "daydot_periodic_lockscreen_refresh"

    /**
     * Schedules the next midnight rollover (00:00:05 local time) plus a lightweight
     * 6-hour WorkManager safety net to keep battery usage minimal.
     */
    fun scheduleMidnightRefresh(context: Context) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val nextMidnight = LocalDate.now()
            .plusDays(1)
            .atTime(LocalTime.of(0, 0, 5))
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ACTION_MIDNIGHT_REFRESH
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            MIDNIGHT_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            nextMidnight,
            pendingIntent
        )

        val periodicRequest = PeriodicWorkRequestBuilder<DailyRefreshWorker>(
            6, TimeUnit.HOURS
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicRequest
        )
    }

    fun scheduleReminderNotification(context: Context, reminder: ReminderEntity) {
        cancelReminderNotification(context, reminder.dateIso)

        val notifyTime = reminder.notificationLocalTime() ?: return
        val triggerDateTime = LocalDateTime.of(reminder.localDate(), notifyTime)
        val triggerMillis = triggerDateTime
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        if (triggerMillis <= System.currentTimeMillis()) return

        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ACTION_REMINDER_ALARM
            putExtra(EXTRA_DATE_ISO, reminder.dateIso)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.dateIso.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerMillis,
            pendingIntent
        )
    }

    fun cancelReminderNotification(context: Context, dateIso: String) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ACTION_REMINDER_ALARM
            putExtra(EXTRA_DATE_ISO, dateIso)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            dateIso.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }
}
