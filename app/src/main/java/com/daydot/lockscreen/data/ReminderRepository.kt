package com.daydot.lockscreen.data

import com.daydot.lockscreen.data.db.ReminderDao
import com.daydot.lockscreen.data.db.ReminderEntity
import com.daydot.lockscreen.domain.WeekCalculator
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class ReminderRepository(private val dao: ReminderDao) {

    fun observeAllReminders(): Flow<List<ReminderEntity>> = dao.observeAll()

    fun observeWeekReminders(referenceDate: LocalDate): Flow<List<ReminderEntity>> {
        val (monday, sunday) = WeekCalculator.weekBounds(referenceDate)
        return dao.observeRange(monday.toString(), sunday.toString())
    }

    fun observeReminderForDate(date: LocalDate): Flow<ReminderEntity?> =
        dao.observeByDate(date.toString())

    suspend fun getReminderForDate(date: LocalDate): ReminderEntity? =
        dao.getByDate(date.toString())

    suspend fun getWeekReminders(referenceDate: LocalDate): List<ReminderEntity> {
        val (monday, sunday) = WeekCalculator.weekBounds(referenceDate)
        return dao.getRange(monday.toString(), sunday.toString())
    }

    suspend fun getUpcomingWithNotifications(fromDate: LocalDate): List<ReminderEntity> =
        dao.getUpcomingWithNotifications(fromDate.toString())

    suspend fun saveReminder(
        date: LocalDate,
        title: String,
        description: String,
        isImportant: Boolean,
        notificationTimeMinutes: Int?
    ): ReminderEntity {
        val entity = ReminderEntity(
            dateIso = date.toString(),
            title = title.trim(),
            description = description.trim(),
            isImportant = isImportant,
            notificationTimeMinutes = notificationTimeMinutes,
            updatedAtEpochMs = System.currentTimeMillis()
        )
        dao.upsert(entity)
        return entity
    }

    suspend fun deleteReminder(date: LocalDate) {
        dao.deleteByDate(date.toString())
    }
}
