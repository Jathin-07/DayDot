package com.daydot.lockscreen.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    @Query("SELECT * FROM reminders ORDER BY dateIso ASC")
    fun observeAll(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE dateIso BETWEEN :startIso AND :endIso ORDER BY dateIso ASC")
    fun observeRange(startIso: String, endIso: String): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE dateIso = :dateIso LIMIT 1")
    fun observeByDate(dateIso: String): Flow<ReminderEntity?>

    @Query("SELECT * FROM reminders WHERE dateIso = :dateIso LIMIT 1")
    suspend fun getByDate(dateIso: String): ReminderEntity?

    @Query("SELECT * FROM reminders WHERE dateIso BETWEEN :startIso AND :endIso ORDER BY dateIso ASC")
    suspend fun getRange(startIso: String, endIso: String): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE dateIso >= :fromDateIso AND notificationTimeMinutes IS NOT NULL ORDER BY dateIso ASC")
    suspend fun getUpcomingWithNotifications(fromDateIso: String): List<ReminderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(reminder: ReminderEntity)

    @Query("DELETE FROM reminders WHERE dateIso = :dateIso")
    suspend fun deleteByDate(dateIso: String)
}
