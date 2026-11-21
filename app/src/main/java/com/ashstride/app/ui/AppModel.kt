package com.ashstride.app.ui

import androidx.room.withTransaction
import com.ashstride.app.AshstrideApp
import com.ashstride.app.data.db.Inventory
import com.ashstride.app.data.db.MendLog
import com.ashstride.app.data.seed.Seeder
import com.ashstride.app.domain.LedgerDay
import com.ashstride.app.domain.MendCheck
import com.ashstride.app.domain.MenderRules
import com.ashstride.app.domain.MenderStash
import com.ashstride.app.domain.Progress
import com.ashstride.app.domain.ProgressSnapshot
import com.ashstride.app.domain.StatsEngine
import com.ashstride.app.domain.StatsSummary
import com.ashstride.app.domain.StreakEngine
import com.ashstride.app.domain.toLedger
import com.ashstride.app.sync.SyncPrefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

/** Holds app-wide state for the screens. All rules live in the domain package; this only wires data to it. */
class AppModel(private val app: AshstrideApp) {
    private val db = app.db
    private val prefs = SyncPrefs(app)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _today = MutableStateFlow(LocalDate.now())
    val today: StateFlow<LocalDate> = _today

    val ledger: StateFlow<List<LedgerDay>> =
        db.dayDao().observeAll().map { rows -> rows.toLedger() }.flowOn(Dispatchers.Default)
            .stateIn(scope, SharingStarted.Eagerly, emptyList())

    val goal: StateFlow<Int> = prefs.goalFlow.stateIn(scope, SharingStarted.Eagerly, SyncPrefs.DEFAULT_GOAL)

    val inventory: StateFlow<Inventory> =
        db.inventoryDao().observe().map { it ?: Inventory() }
            .stateIn(scope, SharingStarted.Eagerly, Inventory())

    val snapshot: StateFlow<ProgressSnapshot?> =
        combine(ledger, _today, db.xpDao().observeTotal()) { l, t, x ->
            if (l.isEmpty()) null else Progress.compute(l, t, x)
        }.flowOn(Dispatchers.Default).stateIn(scope, SharingStarted.Eagerly, null)

    val stats: StateFlow<StatsSummary?> =
        combine(ledger, _today) { l, t -> if (l.isEmpty()) null else StatsEngine.summarize(l, t) }
            .flowOn(Dispatchers.Default).stateIn(scope, SharingStarted.Eagerly, null)

    init {
        scope.launch(Dispatchers.IO) { Seeder.seedIfEmpty(app, db) }
        scope.launch {
            while (true) {
                delay(60_000)
                refreshToday()
            }
        }
    }

    fun refreshToday() {
        _today.value = LocalDate.now()
    }

    /** New goal applies from today on; earlier days keep the goal they had. */
    suspend fun setGoal(goal: Int) = withContext(Dispatchers.IO) {
        prefs.setGoal(goal)
        db.dayDao().updateGoalFrom(LocalDate.now().toString(), goal)
    }

    /** Spends one Mender on [date]. Re-checks every rule against fresh data inside one transaction. Returns true if applied. */
    suspend fun mend(date: LocalDate): Boolean = withContext(Dispatchers.IO) {
        db.withTransaction {
            val inv = db.inventoryDao().get() ?: Inventory()
            val ledgerNow = db.dayDao().getAll().toLedger()
            val check = StreakEngine.checkMend(ledgerNow, date, LocalDate.now())
            if (inv.menders <= 0 || check != MendCheck.OK) {
                false
            } else {
                val now = System.currentTimeMillis()
                val stash = MenderRules.use(MenderStash(inv.shards, inv.menders))
                db.dayDao().markMended(date.toString(), now)
                db.mendDao().insert(MendLog(date.toString(), now))
                db.inventoryDao().set(stash.shards, stash.menders)
                true
            }
        }
    }
}
