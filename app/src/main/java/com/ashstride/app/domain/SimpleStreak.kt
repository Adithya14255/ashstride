package com.ashstride.app.domain

import com.ashstride.app.data.db.DayEntry
import java.time.LocalDate

/**
 * Session 1 placeholder, replaced by the full StreakEngine in Session 2.
 * Counts consecutive days, newest first, that met their own goal or were mended.
 */
object SimpleStreak {
    fun currentRun(daysNewestFirst: List<DayEntry>): Int {
        var run = 0
        var expected: LocalDate? = null
        for (d in daysNewestFirst) {
            val date = LocalDate.parse(d.date)
            if (expected != null && date != expected) break
            if (!(d.steps >= d.goal || d.mended)) break
            run++
            expected = date.minusDays(1)
        }
        return run
    }
}
