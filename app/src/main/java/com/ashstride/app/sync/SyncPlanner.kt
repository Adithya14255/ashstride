package com.ashstride.app.sync

import com.ashstride.app.data.db.DayEntry
import java.time.LocalDate
import java.util.SortedSet

/**
 * Pure sync rules (no Android types), unit-tested.
 *
 * Policy: imported history is the source of truth for the days it covers. Health Connect only
 * writes days after the newest imported day, never overwrites an IMPORTED row, and never touches
 * a row's goal or mended flag.
 */
object SyncPlanner {
    const val SOURCE_IMPORTED = "IMPORTED"
    const val SOURCE_HC = "HEALTH_CONNECT"

    /** Health Connect only serves data from 30 days before permission was first granted unless the history permission is held. */
    const val NO_HISTORY_LOOKBACK_DAYS = 29L

    /** Deletions carry no date, so after one we re-read this many trailing days. */
    const val DELETION_RESYNC_DAYS = 30L

    /** First day Health Connect is allowed to write. */
    fun windowStart(today: LocalDate, newestImported: LocalDate?, historyGranted: Boolean): LocalDate {
        val afterImport = newestImported?.plusDays(1)
        val noHistoryFloor = today.minusDays(NO_HISTORY_LOOKBACK_DAYS)
        val start = afterImport ?: noHistoryFloor
        // Without the history permission nothing older than the floor is readable anyway.
        return if (historyGranted) start else maxOf(start, noHistoryFloor)
    }

    /**
     * Days to (re)read from Health Connect.
     * @param fullResync true on first run or when the change token expired: read the whole window.
     * @param lastSyncDate last day a sync completed; days from there to today are refilled so quiet days still get a row.
     * @param changedDates days touched by upserted records since the last token.
     * @param deletionSeen a record was deleted somewhere: re-read the trailing [DELETION_RESYNC_DAYS] days.
     */
    fun daysToSync(
        today: LocalDate,
        windowStart: LocalDate,
        fullResync: Boolean,
        lastSyncDate: LocalDate?,
        changedDates: Set<LocalDate>,
        deletionSeen: Boolean,
    ): SortedSet<LocalDate> {
        val out = sortedSetOf<LocalDate>()
        if (windowStart.isAfter(today)) return out
        fun addRange(from: LocalDate, to: LocalDate) {
            var d = maxOf(from, windowStart)
            while (!d.isAfter(to)) { out += d; d = d.plusDays(1) }
        }
        if (fullResync) {
            addRange(windowStart, today)
        } else {
            out.add(today)
            if (lastSyncDate != null) addRange(lastSyncDate, today)
            if (deletionSeen) addRange(today.minusDays(DELETION_RESYNC_DAYS), today)
            changedDates.filter { !it.isBefore(windowStart) && !it.isAfter(today) }.forEach { out += it }
        }
        return out
    }

    /** Length of one captured step bucket, in minutes. A 60-minute Continuous Run window spans four of them. */
    const val BUCKET_MINUTES = 15L

    /** Buckets older than the previous challenge week plus this many days are dropped. */
    const val BUCKET_KEEP_DAYS = 14L

    /** Start (epoch ms) of the bucket that contains [atMs], for buckets laid out from [fromMs]. */
    fun bucketStart(fromMs: Long, atMs: Long, sliceMs: Long): Long {
        if (sliceMs <= 0L) return fromMs
        val offset = if (atMs < fromMs) 0L else atMs - fromMs
        return fromMs + (offset / sliceMs) * sliceMs
    }

    /** Real ratio of the imported history, used when the primary source writes steps but no distance. */
    const val KM_PER_STEP = 0.00077

    /** Distance for a day: the source's own figure when it has one, otherwise an estimate from the steps. */
    fun kmOrEstimate(steps: Int, km: Double): Double =
        if (km > 0.0 || steps <= 0) km else steps * KM_PER_STEP

    /** Aggregate minus manual entries. The aggregate API de-duplicates sources but cannot exclude manual records itself. */
    fun netSteps(aggregate: Long, manual: Long): Int = (aggregate - manual).coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
    fun netKm(aggregateKm: Double, manualKm: Double): Double = (aggregateKm - manualKm).coerceAtLeast(0.0)

    /**
     * The row to write for [date], or null to leave the ledger alone.
     * Existing HC rows keep their stored goal and mended flag; new rows get [currentGoal].
     */
    fun buildRow(date: LocalDate, existing: DayEntry?, steps: Int, km: Double, currentGoal: Int): DayEntry? {
        if (existing != null && existing.source == SOURCE_IMPORTED) return null
        return if (existing == null) {
            DayEntry(date = date.toString(), steps = steps, distanceKm = km, source = SOURCE_HC, goal = currentGoal)
        } else {
            if (existing.steps == steps && existing.distanceKm == km) null
            else existing.copy(steps = steps, distanceKm = km)
        }
    }
}
