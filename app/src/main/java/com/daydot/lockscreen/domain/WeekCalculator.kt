package com.daydot.lockscreen.domain

import com.daydot.lockscreen.data.db.ReminderEntity
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

enum class DayCircleState {
    /** Earlier in the current Mon–Sun week (completed day). */
    PAST,
    /** Today, with either no reminder or a normal reminder. */
    TODAY_NORMAL,
    /** Today, marked as an important deadline (renders bright glowing white). */
    TODAY_IMPORTANT,
    /** Later in the current Mon–Sun week, normal or empty. */
    UPCOMING_NORMAL,
    /** Later in the current Mon–Sun week, marked as an important deadline. */
    UPCOMING_IMPORTANT
}

data class WeekDaySlot(
    val date: LocalDate,
    val shortLetter: String,
    val state: DayCircleState,
    val reminder: ReminderEntity?
)

object WeekCalculator {

    private val DAY_LETTERS = listOf("M", "T", "W", "T", "F", "S", "S")

    /** Returns (Monday, Sunday) for the ISO week containing [referenceDate]. */
    fun weekBounds(referenceDate: LocalDate): Pair<LocalDate, LocalDate> {
        val monday = referenceDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val sunday = monday.plusDays(6)
        return monday to sunday
    }

    /** Builds the 7 Monday–Sunday slots with resolved visual state for [today]. */
    fun buildCurrentWeek(
        today: LocalDate,
        remindersInWeek: List<ReminderEntity>
    ): List<WeekDaySlot> {
        val byDate = remindersInWeek.associateBy { it.dateIso }
        val (monday, _) = weekBounds(today)

        return (0L..6L).map { offset ->
            val date = monday.plusDays(offset)
            val reminder = byDate[date.toString()]
            val state = when {
                date.isBefore(today) -> DayCircleState.PAST
                date.isEqual(today) -> {
                    if (reminder?.isImportant == true) {
                        DayCircleState.TODAY_IMPORTANT
                    } else {
                        DayCircleState.TODAY_NORMAL
                    }
                }
                else -> {
                    if (reminder?.isImportant == true) {
                        DayCircleState.UPCOMING_IMPORTANT
                    } else {
                        DayCircleState.UPCOMING_NORMAL
                    }
                }
            }
            WeekDaySlot(
                date = date,
                shortLetter = DAY_LETTERS[offset.toInt()],
                state = state,
                reminder = reminder
            )
        }
    }
}
