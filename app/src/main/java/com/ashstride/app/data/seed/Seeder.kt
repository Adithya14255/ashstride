package com.ashstride.app.data.seed

import android.content.Context
import com.ashstride.app.data.db.AshDatabase
import com.ashstride.app.data.db.DayEntry
import com.ashstride.app.data.db.Inventory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

/** Loads the bundled history (assets/seed_steps.json) into an empty ledger exactly once. */
object Seeder {
    const val DEFAULT_GOAL = 10_000

    suspend fun seedIfEmpty(context: Context, db: AshDatabase) = withContext(Dispatchers.IO) {
        db.inventoryDao().insertIfAbsent(Inventory())
        if (db.dayDao().newestDateBySource("IMPORTED") != null) return@withContext

        val text = context.assets.open("seed_steps.json").bufferedReader().use { it.readText() }
        val arr = JSONArray(text)
        val rows = ArrayList<DayEntry>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            rows += DayEntry(
                date = o.getString("d"),
                steps = o.getInt("s"),
                distanceKm = o.getDouble("km"),
                source = "IMPORTED",
                goal = DEFAULT_GOAL,
            )
        }
        db.dayDao().upsertAll(rows)
    }
}
