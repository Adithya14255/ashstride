package com.ashstride.app.data

import androidx.room.withTransaction
import com.ashstride.app.AshstrideApp
import com.ashstride.app.data.db.BadgeUnlock
import com.ashstride.app.data.db.ChallengeWeek
import com.ashstride.app.data.db.DayEntry
import com.ashstride.app.data.db.Inventory
import com.ashstride.app.data.db.MendLog
import com.ashstride.app.data.db.XpEvent
import com.ashstride.app.domain.BackupBadge
import com.ashstride.app.domain.BackupCodec
import com.ashstride.app.domain.BackupData
import com.ashstride.app.domain.BackupDay
import com.ashstride.app.domain.BackupMend
import com.ashstride.app.domain.BackupWeek
import com.ashstride.app.domain.BackupXp
import com.ashstride.app.domain.CsvResult
import com.ashstride.app.sync.SyncPrefs
import java.io.File

/** Moves everything the app stores in and out of backup files. The format itself lives in the tested [BackupCodec]. */
class BackupRepository(private val app: AshstrideApp) {
    private val db = app.db
    private val syncPrefs = SyncPrefs(app)

    suspend fun snapshot(): BackupData {
        val inv = db.inventoryDao().get() ?: Inventory()
        return BackupData(
            exportedAt = System.currentTimeMillis(),
            goal = syncPrefs.goal(),
            shards = inv.shards,
            menders = inv.menders,
            days = db.dayDao().getAll().map { BackupDay(it.date, it.steps, it.distanceKm, it.source, it.goal, it.mended, it.mendedAtEpochMs) },
            xp = db.xpDao().getAll().map { BackupXp(it.epochMs, it.type, it.amount, it.ref) },
            weeks = db.challengeDao().getAll().map { BackupWeek(it.weekStart, it.offersJson, it.picked, it.status, it.resolvedAtEpochMs) },
            mends = db.mendDao().getAll().map { BackupMend(it.date, it.usedAtEpochMs) },
            badges = db.badgeDao().getAll().map { BackupBadge(it.badgeId, it.variant, it.unlockedAtEpochMs, it.awardedForDate) },
        )
    }

    suspend fun exportJson(): String = BackupCodec.encode(snapshot())

    suspend fun exportCsv(): String = BackupCodec.toCsv(snapshot().days)

    /** Writes a safety copy into the app's private folder before anything is overwritten. Keeps the newest three. */
    private suspend fun safetyCopy() {
        val dir = File(app.filesDir, "backups")
        dir.mkdirs()
        File(dir, "before-restore-" + System.currentTimeMillis() + ".json").writeText(exportJson())
        val old = dir.listFiles()?.sortedByDescending { it.name } ?: return
        for (f in old.drop(3)) f.delete()
    }

    /** Replaces everything with the backup's contents, all or nothing. Returns the number of days restored. */
    suspend fun restore(b: BackupData): Int {
        safetyCopy()
        db.withTransaction {
            db.dayDao().deleteAll()
            db.xpDao().deleteAll()
            db.challengeDao().deleteAll()
            db.mendDao().deleteAll()
            db.badgeDao().deleteAll()
            db.dayDao().upsertAll(b.days.map { DayEntry(it.date, it.steps, it.km, it.source, it.goal, it.mended, it.mendedAt) })
            db.xpDao().insertAll(b.xp.map { XpEvent(epochMs = it.epochMs, type = it.type, amount = it.amount, ref = it.ref) })
            db.challengeDao().insertAll(b.weeks.map { ChallengeWeek(it.weekStart, it.offersJson, it.picked, it.status, it.resolvedAt) })
            db.mendDao().insertAll(b.mends.map { MendLog(it.date, it.usedAt) })
            db.badgeDao().insertAll(b.badges.map { BadgeUnlock(it.badgeId, it.variant, it.unlockedAt, it.awardedFor) })
            db.inventoryDao().insertIfAbsent(Inventory())
            db.inventoryDao().set(b.shards, b.menders)
        }
        syncPrefs.setGoal(b.goal)
        return b.days.size
    }

    /** Merges step rows from a CSV: rows with a date already stored replace that day. Returns days written. */
    suspend fun importCsv(r: CsvResult): Int {
        safetyCopy()
        db.dayDao().upsertAll(r.days.map { DayEntry(it.date, it.steps, it.km, it.source, it.goal, it.mended, it.mendedAt) })
        return r.days.size
    }
}
