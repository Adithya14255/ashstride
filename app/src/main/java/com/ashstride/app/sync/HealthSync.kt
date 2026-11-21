package com.ashstride.app.sync

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.changes.DeletionChange
import androidx.health.connect.client.changes.UpsertionChange
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.request.ChangesTokenRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.ashstride.app.data.db.AshDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Reads steps and distance from Health Connect into the ledger (day_entry, source HEALTH_CONNECT).
 *
 * - Aggregate API per day, so overlapping sources are de-duplicated by Health Connect.
 * - Manual-entry records are read separately and subtracted (SPEC: manual entries excluded).
 * - A changes token is taken BEFORE reading, so nothing written during a sync is missed next time.
 * - Imported history is never overwritten (see [SyncPlanner]).
 */
class HealthSync(private val context: Context, private val db: AshDatabase) {
    private val prefs = SyncPrefs(context)

    suspend fun sync(background: Boolean = false): SyncResult = withContext(Dispatchers.IO) {
        try {
            val status = HealthConnectClient.getSdkStatus(context)
            if (status != HealthConnectClient.SDK_AVAILABLE) {
                return@withContext SyncResult.Unavailable(
                    updateRequired = status == HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED,
                )
            }
            val client = HealthConnectClient.getOrCreate(context)
            val granted = client.permissionController.getGrantedPermissions()
            if (!granted.containsAll(HealthPermissions.required)) return@withContext SyncResult.NeedsPermission
            if (background && HealthPermissions.READ_BACKGROUND !in granted) return@withContext SyncResult.Skipped

            val zone = ZoneId.systemDefault()
            val today = LocalDate.now(zone)
            val newestImported = db.dayDao().newestDateBySource(SyncPlanner.SOURCE_IMPORTED)?.let(LocalDate::parse)
            val historyGranted = HealthPermissions.READ_HISTORY in granted
            val windowStart = SyncPlanner.windowStart(today, newestImported, historyGranted)

            // Token first, data second.
            val oldToken = prefs.token()
            val newToken = client.getChangesToken(
                ChangesTokenRequest(setOf(StepsRecord::class, DistanceRecord::class)),
            )

            var fullResync = oldToken == null
            val changed = mutableSetOf<LocalDate>()
            var deletionSeen = false
            if (oldToken != null) {
                var token = oldToken
                loop@ while (true) {
                    val resp = client.getChanges(token)
                    if (resp.changesTokenExpired) { fullResync = true; break@loop }
                    for (c in resp.changes) when (c) {
                        is UpsertionChange -> when (val r = c.record) {
                            is StepsRecord -> changed += datesOf(r.startTime, r.startZoneOffset, r.endTime, r.endZoneOffset, zone)
                            is DistanceRecord -> changed += datesOf(r.startTime, r.startZoneOffset, r.endTime, r.endZoneOffset, zone)
                            else -> Unit
                        }
                        is DeletionChange -> deletionSeen = true
                    }
                    token = resp.nextChangesToken
                    if (!resp.hasMore) break@loop
                }
            }

            val lastSyncDate = prefs.lastSyncDate()?.let(LocalDate::parse)
            val days = SyncPlanner.daysToSync(today, windowStart, fullResync, lastSyncDate, changed, deletionSeen)
            var written = 0
            if (days.isNotEmpty()) {
                written = syncDays(client, days.first(), days.last(), days, zone)
            }

            val now = System.currentTimeMillis()
            prefs.saveSync(newToken, today.toString(), now)
            SyncResult.Synced(daysChecked = days.size, daysWritten = written, atEpochMs = now)
        } catch (e: Exception) {
            SyncResult.Failed(e.message ?: e.javaClass.simpleName)
        }
    }

    private suspend fun syncDays(
        client: HealthConnectClient,
        first: LocalDate,
        last: LocalDate,
        wanted: Set<LocalDate>,
        zone: ZoneId,
    ): Int {
        val startLdt = first.atStartOfDay()
        val endLdt = last.plusDays(1).atStartOfDay()

        // 1. De-duplicated daily totals.
        val aggSteps = HashMap<LocalDate, Long>()
        val aggKm = HashMap<LocalDate, Double>()
        val grouped = client.aggregateGroupByPeriod(
            AggregateGroupByPeriodRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL, DistanceRecord.DISTANCE_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(startLdt, endLdt),
                timePeriod = Period.ofDays(1),
            ),
        )
        for (g in grouped) {
            val d = g.startTime.toLocalDate()
            aggSteps[d] = g.result[StepsRecord.COUNT_TOTAL] ?: 0L
            aggKm[d] = (g.result[DistanceRecord.DISTANCE_TOTAL]?.inMeters ?: 0.0) / 1000.0
        }

        // 2. Manual-entry records, to subtract.
        val manualSteps = HashMap<LocalDate, Long>()
        val manualKm = HashMap<LocalDate, Double>()
        val range = TimeRangeFilter.between(startLdt.atZone(zone).toInstant(), endLdt.atZone(zone).toInstant())
        var page: String? = null
        do {
            val r = client.readRecords(ReadRecordsRequest(StepsRecord::class, range, pageSize = 1000, pageToken = page))
            for (rec in r.records) if (rec.metadata.recordingMethod == Metadata.RECORDING_METHOD_MANUAL_ENTRY) {
                val d = dateOf(rec.startTime, rec.startZoneOffset, zone)
                manualSteps[d] = (manualSteps[d] ?: 0L) + rec.count
            }
            page = r.pageToken
        } while (page != null)
        do {
            val r = client.readRecords(ReadRecordsRequest(DistanceRecord::class, range, pageSize = 1000, pageToken = page))
            for (rec in r.records) if (rec.metadata.recordingMethod == Metadata.RECORDING_METHOD_MANUAL_ENTRY) {
                val d = dateOf(rec.startTime, rec.startZoneOffset, zone)
                manualKm[d] = (manualKm[d] ?: 0.0) + rec.distance.inKilometers
            }
            page = r.pageToken
        } while (page != null)

        // 3. Merge into the ledger.
        val existing = db.dayDao().getRange(first.toString(), last.toString()).associateBy { it.date }
        val goal = prefs.goal()
        val rows = wanted.mapNotNull { d ->
            SyncPlanner.buildRow(
                date = d,
                existing = existing[d.toString()],
                steps = SyncPlanner.netSteps(aggSteps[d] ?: 0L, manualSteps[d] ?: 0L),
                km = SyncPlanner.netKm(aggKm[d] ?: 0.0, manualKm[d] ?: 0.0),
                currentGoal = goal,
            )
        }
        if (rows.isNotEmpty()) db.dayDao().upsertAll(rows)
        return rows.size
    }

    private fun dateOf(t: Instant, offset: ZoneOffset?, zone: ZoneId): LocalDate =
        if (offset != null) t.atOffset(offset).toLocalDate() else t.atZone(zone).toLocalDate()

    private fun datesOf(s: Instant, so: ZoneOffset?, e: Instant, eo: ZoneOffset?, zone: ZoneId): List<LocalDate> {
        val a = dateOf(s, so, zone)
        val b = dateOf(e, eo, zone)
        return if (a == b) listOf(a) else listOf(a, b)
    }
}
