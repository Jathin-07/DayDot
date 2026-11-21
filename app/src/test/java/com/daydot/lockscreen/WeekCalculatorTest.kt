package com.daydot.lockscreen

import com.daydot.lockscreen.data.db.ReminderEntity
import com.daydot.lockscreen.domain.DayCircleState
import com.daydot.lockscreen.domain.WeekCalculator
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class WeekCalculatorTest {

    @Test
    fun weekBoundsAlwaysSpanMondayToSunday() {
        val wednesday = LocalDate.of(2026, 11, 18)
        val (monday, sunday) = WeekCalculator.weekBounds(wednesday)
        assertEquals(DayOfWeek.MONDAY, monday.dayOfWeek)
        assertEquals(DayOfWeek.SUNDAY, sunday.dayOfWeek)
        assertEquals(LocalDate.of(2026, 11, 16), monday)
        assertEquals(LocalDate.of(2026, 11, 22), sunday)
    }

    @Test
    fun mondayRolloverStartsFreshWeekWithoutTouchingPastDates() {
        val nextMonday = LocalDate.of(2026, 11, 23)
        val (monday, sunday) = WeekCalculator.weekBounds(nextMonday)
        assertEquals(LocalDate.of(2026, 11, 23), monday)
        assertEquals(LocalDate.of(2026, 11, 29), sunday)
    }

    @Test
    fun circleStatesDistinguishPastTodayUpcomingAndImportant() {
        val today = LocalDate.of(2026, 11, 19) // Thursday
        val reminders = listOf(
            ReminderEntity(dateIso = "2026-11-19", title = "Ship release", isImportant = true),
            ReminderEntity(dateIso = "2026-11-21", title = "Exam deadline", isImportant = true)
        )

        val slots = WeekCalculator.buildCurrentWeek(today, reminders)
        assertEquals(7, slots.size)
        assertEquals(DayCircleState.PAST, slots[0].state) // Mon
        assertEquals(DayCircleState.PAST, slots[1].state) // Tue
        assertEquals(DayCircleState.PAST, slots[2].state) // Wed
        assertEquals(DayCircleState.TODAY_IMPORTANT, slots[3].state) // Thu (Today + Important)
        assertEquals(DayCircleState.UPCOMING_NORMAL, slots[4].state) // Fri
        assertEquals(DayCircleState.UPCOMING_IMPORTANT, slots[5].state) // Sat (Upcoming + Important)
        assertEquals(DayCircleState.UPCOMING_NORMAL, slots[6].state) // Sun
    }
}
