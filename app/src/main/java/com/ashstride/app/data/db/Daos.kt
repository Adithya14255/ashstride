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
}
