package com.ashstride.app.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

data class MonthTotal(val month: YearMonth, val steps: Long, val km: Double, val daysMet: Int, val days: Int)

/** A challenge week (same week as the weekly challenges: Monday to Sunday, Saturday to Friday before 30 Nov 2026). */
data class WeekTotal(val start: LocalDate, val steps: Long)

data class StatsSummary(
    val days: Int,
    val totalSteps: Long,
    val totalKm: Double,
    /** Average over completed days (today is excluded because it is still in progress). */
    val averageSteps: Int,
    val daysMet: Int,
    /** Index 0 = Monday ... 6 = Sunday; average steps over completed days. */
    val dayOfWeekAverage: List<Int>,
    /** Last N months ending with the current month, oldest first (months with no data are zero). */
    val monthly: List<MonthTotal>,
    val bestDay: LedgerDay?,
    val bestWeek: WeekTotal?,
    val bestMonth: MonthTotal?,
    val longestDay: LedgerDay?,
)

object StatsEngine {
    fun weekStart(date: LocalDate): LocalDate = ChallengeGenerator.weekStartFor(date)

    fun summarize(days: List<LedgerDay>, today: LocalDate, monthsShown: Int = 12): StatsSummary {
        val past = days.filter { !it.date.isAfter(today) }
        val completed = past.filter { it.date.isBefore(today) }

        val dowSum = LongArray(7)
        val dowCount = IntArray(7)
        for (d in completed) {
            val i = d.date.dayOfWeek.value - 1
            dowSum[i] += d.steps.toLong()
            dowCount[i] += 1
        }
        val dowAvg = List(7) { i -> if (dowCount[i] == 0) 0 else (dowSum[i] / dowCount[i]).toInt() }

        val byMonth = past.groupBy { YearMonth.from(it.date) }
        fun totalOf(ym: YearMonth): MonthTotal {
            val rows = byMonth[ym].orEmpty()
            return MonthTotal(
                month = ym,
                steps = rows.sumOf { it.steps.toLong() },
                km = rows.sumOf { it.distanceKm },
                daysMet = rows.count { it.met },
                days = rows.size,
            )
        }
        val current = YearMonth.from(today)
        val monthly = (monthsShown - 1 downTo 0).map { totalOf(current.minusMonths(it.toLong())) }
        val bestMonth = byMonth.keys.map { totalOf(it) }.maxByOrNull { it.steps }

        val bestWeek = past.groupBy { weekStart(it.date) }
            .map { (start, rows) -> WeekTotal(start, rows.sumOf { it.steps.toLong() }) }
            .maxByOrNull { it.steps }

        val avg = if (completed.isEmpty()) 0 else (completed.sumOf { it.steps.toLong() } / completed.size).toInt()
        return StatsSummary(
            days = past.size,
            totalSteps = past.sumOf { it.steps.toLong() },
            totalKm = past.sumOf { it.distanceKm },
            averageSteps = avg,
            daysMet = past.count { it.met },
            dayOfWeekAverage = dowAvg,
            monthly = monthly,
            bestDay = past.maxByOrNull { it.steps },
            bestWeek = bestWeek,
            bestMonth = bestMonth,
            longestDay = past.maxByOrNull { it.distanceKm },
        )
    }
}

object CalendarGrid {
    /** Monday-first weeks for [month]; days outside the month are null. */
    fun weeks(month: YearMonth): List<List<LocalDate?>> {
        val cells = ArrayList<LocalDate?>()
        repeat(month.atDay(1).dayOfWeek.value - 1) { cells.add(null) }
        for (d in 1..month.lengthOfMonth()) cells.add(month.atDay(d))
        while (cells.size % 7 != 0) cells.add(null)
        return cells.chunked(7)
    }
}
