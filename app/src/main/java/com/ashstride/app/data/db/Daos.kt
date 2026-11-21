package com.ashstride.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class LedgerSummary(val days: Int, val steps: Long, val km: Double)

@Dao
interface DayDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(rows: List<DayEntry>)

    @Query("SELECT COUNT(*) FROM day_entry")
    suspend fun count(): Int

    /** Rows with date in [from, to] (yyyy-MM-dd strings sort chronologically). */
    @Query("SELECT * FROM day_entry WHERE date >= :from AND date <= :to")
    suspend fun getRange(from: String, to: String): List<DayEntry>

    @Query("SELECT * FROM day_entry")
    suspend fun getAll(): List<DayEntry>

    @Query("UPDATE day_entry SET mended = 1, mendedAtEpochMs = :epochMs WHERE date = :date")
    suspend fun markMended(date: String, epochMs: Long)

    /** Goal changes apply from [fromDate] on; earlier rows keep the goal they had. */
    @Query("UPDATE day_entry SET goal = :goal WHERE date >= :fromDate")
    suspend fun updateGoalFrom(fromDate: String, goal: Int)

    /** Newest date held with the given source, or null. Used to find where imported history ends. */
    @Query("SELECT MAX(date) FROM day_entry WHERE source = :source")
    suspend fun newestDateBySource(source: String): String?

    @Query("SELECT * FROM day_entry ORDER BY date DESC")
    fun observeAll(): Flow<List<DayEntry>>

    @Query("SELECT COUNT(*) AS days, COALESCE(SUM(steps), 0) AS steps, COALESCE(SUM(distanceKm), 0.0) AS km FROM day_entry")
    fun observeSummary(): Flow<LedgerSummary>
}

@Dao
interface InventoryDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(row: Inventory)

    @Query("SELECT * FROM inventory WHERE id = 1")
    fun observe(): Flow<Inventory?>

    @Query("SELECT * FROM inventory WHERE id = 1")
    suspend fun get(): Inventory?

    @Query("UPDATE inventory SET shards = :shards, menders = :menders WHERE id = 1")
    suspend fun set(shards: Int, menders: Int)
}

@Dao
interface XpDao {
    @Query("SELECT COALESCE(SUM(amount), 0) FROM xp_event")
    fun observeTotal(): Flow<Long>

    @Insert
    suspend fun insert(row: XpEvent)

    /** How many events of [type] already exist for [ref]. Used so a reward is only ever paid once. */
    @Query("SELECT COUNT(*) FROM xp_event WHERE type = :type AND ref = :ref")
    suspend fun countFor(type: String, ref: String): Int
}

@Dao
interface MendDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(row: MendLog)

    @Query("SELECT COUNT(*) FROM mend_log")
    fun observeCount(): Flow<Int>
}

@Dao
interface ChallengeDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(row: ChallengeWeek)

    @Query("SELECT * FROM challenge_week ORDER BY weekStart DESC")
    fun observeAll(): Flow<List<ChallengeWeek>>

    @Query("SELECT * FROM challenge_week")
    suspend fun getAll(): List<ChallengeWeek>

    @Query("SELECT * FROM challenge_week WHERE weekStart = :weekStart")
    suspend fun get(weekStart: String): ChallengeWeek?

    /** Locks the pick. Only works while the week is still OPEN, so a pick can never change. Returns rows changed. */
    @Query("UPDATE challenge_week SET picked = :picked, status = :status WHERE weekStart = :weekStart AND status = 'OPEN'")
    suspend fun pick(weekStart: String, picked: String, status: String): Int

    @Query("UPDATE challenge_week SET status = :status, resolvedAtEpochMs = :at WHERE weekStart = :weekStart")
    suspend fun resolve(weekStart: String, status: String, at: Long)
}

@Dao
interface BucketDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(rows: List<StepBucket>)

    @Query("DELETE FROM step_bucket WHERE startMs >= :fromMs")
    suspend fun deleteFrom(fromMs: Long)

    @Query("DELETE FROM step_bucket WHERE startMs < :beforeMs")
    suspend fun deleteBefore(beforeMs: Long)

    @Query("SELECT * FROM step_bucket ORDER BY startMs")
    suspend fun getAll(): List<StepBucket>

    @Query("SELECT * FROM step_bucket ORDER BY startMs")
    fun observeAll(): Flow<List<StepBucket>>

    @Query("SELECT COUNT(*) FROM step_bucket")
    suspend fun count(): Int
}
