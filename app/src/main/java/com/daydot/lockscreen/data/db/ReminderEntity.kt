package com.daydot.lockscreen.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalTime

/**
 * Stores strictly ONE primary reminder per calendar day.
 * Using ISO date ("YYYY-MM-DD") as the primary key enforces the single-reminder-per-day
 * rule directly at the database level.
 */
@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey
    val dateIso: String,
    val title: String,
    val description: String = "",
    val isImportant: Boolean = false,
    /** Minutes since midnight (0..1439), or null if no notification is scheduled. */
    val notificationTimeMinutes: Int? = null,
    val updatedAtEpochMs: Long = System.currentTimeMillis()
) {
    fun localDate(): LocalDate = LocalDate.parse(dateIso)

    fun notificationLocalTime(): LocalTime? =
        notificationTimeMinutes?.let { LocalTime.of(it / 60, it % 60) }
}
