package com.daydot.lockscreen

import com.daydot.lockscreen.data.db.ReminderEntity
import com.daydot.lockscreen.domain.DayCircleState
import com.daydot.lockscreen.domain.WeekCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
    fun mondayRolloverStartsFreshWeekWithoutLeakingPreviousWeek() {
        val nextMonday = LocalDate.of(2026, 11, 23)
        val reminders = listOf(
            ReminderEntity(dateIso = "2026-11-21", title = "Last week important", isImportant = true),
            ReminderEntity(dateIso = "2026-11-25", title = "Next week Wed important", isImportant = true)
        )
        val slots = WeekCalculator.buildCurrentWeek(nextMonday, reminders)
        assertEquals(LocalDate.of(2026, 11, 23), slots[0].date)
        assertEquals(LocalDate.of(2026, 11, 29), slots[6].date)
        // Nov 25 is Wednesday (index 2)
        assertTrue(slots[2].isImportant)
        assertEquals(DayCircleState.UPCOMING_IMPORTANT, slots[2].state)
        // Saturday Nov 28 is NOT important even though previous Saturday Nov 21 was
        assertFalse(slots[5].isImportant)
        assertEquals(DayCircleState.UPCOMING_NORMAL, slots[5].state)
    }

    @Test
    fun importantDateStaysOnExactCalendarDateAfterMidnightTransition() {
        // User marks Nov 21, 2026 as important; Nov 22, 2026 is NOT important
        val reminders = listOf(
            ReminderEntity(dateIso = "2026-11-21", title = "Important Saturday", isImportant = true)
        )

        // Day 1: Saturday Nov 21
        val nov21Slots = WeekCalculator.buildCurrentWeek(LocalDate.of(2026, 11, 21), reminders)
        assertEquals(DayCircleState.TODAY_IMPORTANT, nov21Slots[5].state) // Sat Nov 21
        assertTrue(nov21Slots[5].isImportant)
        assertTrue(nov21Slots[5].isToday)
        assertEquals(DayCircleState.UPCOMING_NORMAL, nov21Slots[6].state) // Sun Nov 22
        assertFalse(nov21Slots[6].isImportant)

        // Day 2: Sunday Nov 22 — Nov 21 MUST keep its important status, Nov 22 MUST remain normal
        val nov22Slots = WeekCalculator.buildCurrentWeek(LocalDate.of(2026, 11, 22), reminders)
        assertEquals(DayCircleState.PAST_IMPORTANT, nov22Slots[5].state) // Sat Nov 21 stays important
        assertTrue(nov22Slots[5].isImportant)
        assertFalse(nov22Slots[5].isToday)
        assertEquals(DayCircleState.TODAY_NORMAL, nov22Slots[6].state) // Sun Nov 22 is normal today
        assertFalse(nov22Slots[6].isImportant)
        assertTrue(nov22Slots[6].isToday)
    }

    @Test
    fun multipleImportantDatesInSameWeekResolveIndependently() {
        val today = LocalDate.of(2026, 11, 19) // Thursday
        val reminders = listOf(
            ReminderEntity(dateIso = "2026-11-17", title = "Past Tue important", isImportant = true),
            ReminderEntity(dateIso = "2026-11-19", title = "Today Thu important", isImportant = true),
            ReminderEntity(dateIso = "2026-11-21", title = "Upcoming Sat important", isImportant = true)
        )

        val slots = WeekCalculator.buildCurrentWeek(today, reminders)
        assertEquals(DayCircleState.PAST, slots[0].state)               // Mon
        assertEquals(DayCircleState.PAST_IMPORTANT, slots[1].state)     // Tue
        assertEquals(DayCircleState.PAST, slots[2].state)               // Wed
        assertEquals(DayCircleState.TODAY_IMPORTANT, slots[3].state)    // Thu
        assertEquals(DayCircleState.UPCOMING_NORMAL, slots[4].state)    // Fri
        assertEquals(DayCircleState.UPCOMING_IMPORTANT, slots[5].state) // Sat
        assertEquals(DayCircleState.UPCOMING_NORMAL, slots[6].state)    // Sun
    }
}
