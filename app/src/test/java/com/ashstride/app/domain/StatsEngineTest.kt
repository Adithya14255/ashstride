package com.ashstride.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class StatsEngineTest {
    private fun day(date: String, steps: Int, goal: Int = 10_000) =
        LedgerDay(LocalDate.parse(date), steps, steps * 0.001, goal)

    private val today = LocalDate.of(2026, 11, 30)
    private val ledger = listOf(
        day("2026-11-16", 10_000), // Monday
        day("2026-11-17", 20_000), // Tuesday
        day("2026-11-23", 6_000),  // Monday
    )

    @Test fun totalsAndAverages() {
        val s = StatsEngine.summarize(ledger, today)
        assertEquals(3, s.days)
        assertEquals(36_000L, s.totalSteps)
        assertEquals(12_000, s.averageSteps)
        assertEquals(2, s.daysMet)
    }

    @Test fun dayOfWeekAverageIsMondayFirst() {
        val s = StatsEngine.summarize(ledger, today)
        assertEquals(8_000, s.dayOfWeekAverage[0])
        assertEquals(20_000, s.dayOfWeekAverage[1])
        assertEquals(0, s.dayOfWeekAverage[2])
    }

    @Test fun recordsAreFound() {
        val s = StatsEngine.summarize(ledger, today)
        assertEquals(20_000, s.bestDay?.steps)
        assertEquals(30_000L, s.bestWeek?.steps)
        assertEquals(LocalDate.of(2026, 11, 14), s.bestWeek?.start)
        assertEquals(36_000L, s.bestMonth?.steps)
    }

    @Test fun monthlyHasTwelveEntriesEndingThisMonth() {
        val s = StatsEngine.summarize(ledger, today)
        assertEquals(12, s.monthly.size)
        assertEquals(YearMonth.of(2026, 11), s.monthly.last().month)
        assertEquals(36_000L, s.monthly.last().steps)
        assertEquals(0L, s.monthly.first().steps)
    }

    @Test fun todayIsExcludedFromAveragesButCountedInTotals() {
        val withToday = ledger + day("2026-11-30", 4_000)
        val s = StatsEngine.summarize(withToday, today)
        assertEquals(40_000L, s.totalSteps)
        assertEquals(12_000, s.averageSteps)
    }

    @Test fun emptyLedgerIsSafe() {
        val s = StatsEngine.summarize(emptyList(), today)
        assertEquals(0, s.days)
        assertEquals(0, s.averageSteps)
        assertNull(s.bestDay)
        assertNull(s.bestWeek)
    }

    @Test fun calendarStartsOnMonday() {
        val nov = CalendarGrid.weeks(YearMonth.of(2026, 11)) // 1 Nov 2026 is a Sunday
        assertEquals(6, nov.size)
        assertNull(nov[0][5])
        assertEquals(LocalDate.of(2026, 11, 1), nov[0][6])
        val feb = CalendarGrid.weeks(YearMonth.of(2027, 2)) // 1 Feb 2027 is a Monday, 28 days
        assertEquals(4, feb.size)
        assertEquals(LocalDate.of(2027, 2, 1), feb[0][0])
    }
}
