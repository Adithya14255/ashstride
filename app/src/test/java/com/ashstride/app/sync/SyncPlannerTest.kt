package com.ashstride.app.sync

import com.ashstride.app.data.db.DayEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SyncPlannerTest {
    private val today = LocalDate.of(2026, 11, 21)
    private val imported = LocalDate.of(2026, 11, 20)

    private fun row(date: String, steps: Int, source: String, goal: Int = 10_000, mended: Boolean = false) =
        DayEntry(date = date, steps = steps, distanceKm = steps * 0.00075, source = source, goal = goal, mended = mended)

    @Test fun windowStartsDayAfterImport() {
        assertEquals(LocalDate.of(2026, 11, 21), SyncPlanner.windowStart(today, imported, historyGranted = true))
    }

    @Test fun windowWithoutHistoryPermissionIsFlooredTo30Days() {
        val oldImport = LocalDate.of(2026, 1, 1)
        assertEquals(today.minusDays(29), SyncPlanner.windowStart(today, oldImport, historyGranted = false))
        assertEquals(oldImport.plusDays(1), SyncPlanner.windowStart(today, oldImport, historyGranted = true))
    }

    @Test fun windowWithNoImportUsesThirtyDays() {
        assertEquals(today.minusDays(29), SyncPlanner.windowStart(today, null, historyGranted = true))
    }

    @Test fun fullResyncCoversWholeWindow() {
        val start = LocalDate.of(2026, 11, 18)
        val days = SyncPlanner.daysToSync(today, start, true, null, emptySet(), false)
        assertEquals(listOf(18, 19, 20, 21), days.map { it.dayOfMonth })
    }

    @Test fun incrementalAlwaysIncludesToday() {
        val days = SyncPlanner.daysToSync(today, today, false, today, emptySet(), false)
        assertEquals(listOf(today), days.toList())
    }

    @Test fun incrementalFillsGapSinceLastSync() {
        val start = LocalDate.of(2026, 11, 10)
        val days = SyncPlanner.daysToSync(today, start, false, LocalDate.of(2026, 11, 18), emptySet(), false)
        assertEquals(listOf(18, 19, 20, 21), days.map { it.dayOfMonth })
    }

    @Test fun changedDatesBeforeWindowAreDropped() {
        val start = LocalDate.of(2026, 11, 21)
        val days = SyncPlanner.daysToSync(today, start, false, today, setOf(LocalDate.of(2026, 11, 5)), false)
        assertEquals(listOf(today), days.toList())
    }

    @Test fun changedDateInsideWindowIsIncluded() {
        val start = LocalDate.of(2026, 11, 15)
        val days = SyncPlanner.daysToSync(today, start, false, today, setOf(LocalDate.of(2026, 11, 16)), false)
        assertTrue(LocalDate.of(2026, 11, 16) in days)
    }

    @Test fun deletionResyncsTrailingDaysButNotBeforeWindow() {
        val start = LocalDate.of(2026, 11, 19)
        val days = SyncPlanner.daysToSync(today, start, false, today, emptySet(), true)
        assertEquals(listOf(19, 20, 21), days.map { it.dayOfMonth })
    }

    @Test fun windowInTheFutureYieldsNothing() {
        assertTrue(SyncPlanner.daysToSync(today, today.plusDays(1), true, null, emptySet(), false).isEmpty())
    }

    @Test fun manualStepsAreSubtractedAndClamped() {
        assertEquals(7_000, SyncPlanner.netSteps(10_000, 3_000))
        assertEquals(0, SyncPlanner.netSteps(1_000, 5_000))
        assertEquals(0.0, SyncPlanner.netKm(1.0, 2.0), 0.0)
        assertEquals(1.5, SyncPlanner.netKm(2.0, 0.5), 1e-9)
    }

    @Test fun neverOverwritesImportedRows() {
        assertNull(SyncPlanner.buildRow(imported, row("2026-11-20", 8_000, "IMPORTED"), 12_000, 9.0, 10_000))
    }

    @Test fun newRowUsesCurrentGoalAndHcSource() {
        val r = SyncPlanner.buildRow(today, null, 4_321, 3.2, 12_000)!!
        assertEquals("2026-11-21", r.date)
        assertEquals(4_321, r.steps)
        assertEquals("HEALTH_CONNECT", r.source)
        assertEquals(12_000, r.goal)
        assertEquals(false, r.mended)
    }

    @Test fun existingHcRowKeepsGoalAndMendedFlagWhenStepsChange() {
        val old = row("2026-11-21", 3_000, "HEALTH_CONNECT", goal = 10_000, mended = true)
        val r = SyncPlanner.buildRow(today, old, 6_000, 4.5, 15_000)!!
        assertEquals(6_000, r.steps)
        assertEquals(10_000, r.goal)
        assertTrue(r.mended)
    }

    @Test fun unchangedHcRowProducesNoWrite() {
        val old = row("2026-11-21", 3_000, "HEALTH_CONNECT")
        assertNull(SyncPlanner.buildRow(today, old, old.steps, old.distanceKm, 10_000))
    }

    @Test fun zeroStepDayStillGetsARow() {
        val r = SyncPlanner.buildRow(LocalDate.of(2026, 11, 22), null, 0, 0.0, 10_000)!!
        assertEquals(0, r.steps)
    }
}
