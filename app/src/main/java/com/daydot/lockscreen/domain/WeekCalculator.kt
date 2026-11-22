package com.daydot.lockscreen.domain

import com.daydot.lockscreen.data.db.ReminderEntity
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

enum class DayCircleState {
    /** Earlier in the current Mon–Sun week, not marked important. */
    PAST,
    /** Earlier in the current Mon–Sun week, marked important (preserves important-day color). */
    PAST_IMPORTANT,
    /** Today, not marked important. */
    TODAY_NORMAL,
    /** Today, marked as an important deadline. */
    TODAY_IMPORTANT,
    /** Later in the current Mon–Sun week, not marked important. */
    UPCOMING_NORMAL,
    /** Later in the current Mon–Sun week, marked as an important deadline. */
    UPCOMING_IMPORTANT
}

data class WeekDaySlot(
    val date: LocalDate,
    val shortLetter: String,
    val state: DayCircleState,
    val reminder: ReminderEntity?,
    val isToday: Boolean = false,
    val isImportant: Boolean = false
)

object WeekCalculator {

    private val DAY_LETTERS = listOf("M", "T", "W", "T", "F", "S", "S")

    /** Returns (Monday, Sunday) for the ISO week containing [referenceDate]. */
    fun weekBounds(referenceDate: LocalDate): Pair<LocalDate, LocalDate> {
        val monday = referenceDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val sunday = monday.plusDays(6)
        return monday to sunday
    }

    /**
     * Builds the 7 Monday–Sunday slots for the local calendar week containing [today].
     * Importance is strictly matched against each slot's exact ISO calendar date ("YYYY-MM-DD"),
     * never by weekday index, so an important date keeps its status even after the day passes.
     */
    fun buildCurrentWeek(
        today: LocalDate,
        remindersInWeek: List<ReminderEntity>
    ): List<WeekDaySlot> {
        val byDate = remindersInWeek.associateBy { it.dateIso }
        val (monday, _) = weekBounds(today)

        return (0L..6L).map { offset ->
            val date = monday.plusDays(offset)
            val reminder = byDate[date.toString()]
            val isImportant = reminder?.isImportant == true
            val isToday = date.isEqual(today)

            val state = when {
                date.isBefore(today) -> {
                    if (isImportant) DayCircleState.PAST_IMPORTANT else DayCircleState.PAST
                }
                isToday -> {
                    if (isImportant) DayCircleState.TODAY_IMPORTANT else DayCircleState.TODAY_NORMAL
                }
                else -> {
                    if (isImportant) DayCircleState.UPCOMING_IMPORTANT else DayCircleState.UPCOMING_NORMAL
                }
            }

            WeekDaySlot(
                date = date,
                shortLetter = DAY_LETTERS[offset.toInt()],
                state = state,
                reminder = reminder,
                isToday = isToday,
                isImportant = isImportant
            )
        }
    }
}
