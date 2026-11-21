package com.ashstride.app.ui

import androidx.room.withTransaction
import com.ashstride.app.AshstrideApp
import com.ashstride.app.data.UiPrefs
import com.ashstride.app.data.db.ChallengeWeek
import com.ashstride.app.data.db.Inventory
import com.ashstride.app.data.db.MendLog
import com.ashstride.app.data.db.XpEvent
import com.ashstride.app.data.seed.Seeder
import com.ashstride.app.domain.BadgeCatalog
import com.ashstride.app.domain.BadgeEntry
import com.ashstride.app.domain.CareerStats
import com.ashstride.app.domain.Challenge
import com.ashstride.app.domain.ChallengeCodec
import com.ashstride.app.domain.ChallengeGenerator
import com.ashstride.app.domain.ChallengeTier
import com.ashstride.app.domain.JourneyEngine
import com.ashstride.app.domain.JourneyPosition
import com.ashstride.app.domain.LedgerDay
import com.ashstride.app.domain.MendCheck
import com.ashstride.app.domain.MenderRules
import com.ashstride.app.domain.MenderStash
import com.ashstride.app.domain.Progress
import com.ashstride.app.domain.ProgressSnapshot
import com.ashstride.app.domain.ShrineEngine
import com.ashstride.app.domain.StatsEngine
import com.ashstride.app.domain.StatsSummary
import com.ashstride.app.domain.StepSample
import com.ashstride.app.domain.Story
import com.ashstride.app.domain.StoryParser
import com.ashstride.app.domain.StreakEngine
import com.ashstride.app.domain.Tier
import com.ashstride.app.domain.TitleEngine
import com.ashstride.app.domain.WeekStatus
import com.ashstride.app.domain.WonChallenge
import com.ashstride.app.domain.toLedger
import com.ashstride.app.sync.SyncPrefs
import com.ashstride.app.ui.celebrate.Celebration
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Holds app-wide state for the screens. All rules live in the domain package; this only wires data to it. */
class AppModel(private val app: AshstrideApp) {
    private val db = app.db
    private val prefs = SyncPrefs(app)
    val uiPrefs = UiPrefs(app)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val settleLock = Mutex()

    private val _today = MutableStateFlow(LocalDate.now())
    val today: StateFlow<LocalDate> = _today

    /** Parsed from assets/story.json on first use. */
    val story: Story by lazy {
        app.assets.open("story.json").bufferedReader().use { StoryParser.parse(it.readText()) }
    }

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

    // ---- Journey -------------------------------------------------------------------------------

    val journey: StateFlow<JourneyPosition?> =
        ledger.map { l ->
            if (l.isEmpty()) null else JourneyEngine.position(story, l.sumOf { it.distanceKm })
        }.flowOn(Dispatchers.Default).stateIn(scope, SharingStarted.Eagerly, null)

    // ---- Shrine --------------------------------------------------------------------------------

    private val weekRows: StateFlow<List<ChallengeWeek>> =
        db.challengeDao().observeAll().stateIn(scope, SharingStarted.Eagerly, emptyList())

    private val samples: StateFlow<List<StepSample>> =
        db.bucketDao().observeAll().map { rows -> rows.map { StepSample(it.startMs, it.steps) } }
            .flowOn(Dispatchers.Default).stateIn(scope, SharingStarted.Eagerly, emptyList())

    val shrine: StateFlow<ShrineState?> =
        combine(weekRows, ledger, samples, _today) { rows, l, s, t ->
            if (l.isEmpty()) null else ShrineState.build(rows, l, s, t, ZoneId.systemDefault())
        }.flowOn(Dispatchers.Default).stateIn(scope, SharingStarted.Eagerly, null)

    /** Weekly wins, newest first. They feed the badge list, XP and the profile. */
    val wins: StateFlow<List<WonChallenge>> =
        weekRows.map { rows ->
            val zone = ZoneId.systemDefault()
            rows.filter { it.status == WeekStatus.WON }.mapNotNull { r ->
                val c = ChallengeCodec.decode(r.offersJson).firstOrNull { it.id == r.picked }
                if (c == null) null else {
                    val date = r.resolvedAtEpochMs?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() } ?: c.weekStart
                    WonChallenge(c, date)
                }
            }
        }.flowOn(Dispatchers.Default).stateIn(scope, SharingStarted.Eagerly, emptyList())

    // ---- Badges and profile --------------------------------------------------------------------

    val badgeEntries: StateFlow<List<BadgeEntry>> =
        combine(snapshot, wins) { s, w -> BadgeCatalog.entries(s?.badges ?: emptyList(), w) }
            .flowOn(Dispatchers.Default).stateIn(scope, SharingStarted.Eagerly, emptyList())

    val mendsUsed: StateFlow<Int> =
        db.mendDao().observeCount().stateIn(scope, SharingStarted.Eagerly, 0)

    val career: StateFlow<CareerStats?> =
        combine(snapshot, stats, wins, mendsUsed, journey) { s, st, w, m, j ->
            if (s == null || st == null || j == null) null else CareerStats(
                level = s.level.level,
                tier = s.level.tier,
                bestTotalStreak = s.streak.bestTotal,
                bestPureStreak = s.streak.bestPure,
                totalSteps = st.totalSteps,
                totalKm = st.totalKm,
                challengesWon = w.size,
                legendaryWon = w.count { it.challenge.tier == ChallengeTier.LEGENDARY },
                mendersUsed = m,
                journeyFraction = j.fraction,
            )
        }.flowOn(Dispatchers.Default).stateIn(scope, SharingStarted.Eagerly, null)

    val profileName: StateFlow<String> =
        uiPrefs.nameFlow.stateIn(scope, SharingStarted.Eagerly, UiPrefs.DEFAULT_NAME)
    val profileTitleId: StateFlow<String?> =
        uiPrefs.titleFlow.stateIn(scope, SharingStarted.Eagerly, null)
    val equippedBadge: StateFlow<String?> =
        uiPrefs.equippedFlow.stateIn(scope, SharingStarted.Eagerly, null)
    val showcase: StateFlow<List<String>> =
        uiPrefs.showcaseFlow.stateIn(scope, SharingStarted.Eagerly, emptyList())

    // ---- Celebrations --------------------------------------------------------------------------

    private val _celebrations = MutableStateFlow<List<Celebration>>(emptyList())
    val celebrations: StateFlow<List<Celebration>> = _celebrations

    fun dismissCelebration() {
        _celebrations.value = _celebrations.value.drop(1)
    }

    init {
        scope.launch(Dispatchers.IO) { Seeder.seedIfEmpty(app, db) }
        scope.launch {
            while (true) {
                delay(60_000)
                refreshToday()
            }
        }
        // Settle finished weeks and open this week's cards whenever the ledger, the step buckets or the date change.
        scope.launch {
            combine(ledger, samples, _today) { l, s, t -> Triple(l.size, s.size, t) }.collect {
                try {
                    settleWeeks()
                } catch (e: Exception) {
                    // Best effort: the next change tries again.
                }
            }
        }
        scope.launch {
            combine(snapshot, weekRows, journey) { s, w, j -> Triple(s, w, j) }.collect { (s, w, j) ->
                if (s != null && j != null) {
                    try {
                        checkCelebrations(s, w, j)
                    } catch (e: Exception) {
                        // Celebrations are decoration; never let one break the app.
                    }
                }
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

    // ---- Shrine actions ------------------------------------------------------------------------

    /**
     * Locks in [challengeId] for the week starting [weekStart]. Only works while the week is OPEN and still
     * inside the pick window, and the database update itself refuses a second pick. A card that is already
     * complete is settled straight away.
     */
    suspend fun pickChallenge(weekStart: LocalDate, challengeId: String): Boolean {
        val ok = withContext(Dispatchers.IO) {
            db.withTransaction {
                val row = db.challengeDao().get(weekStart.toString())
                if (row == null) {
                    false
                } else {
                    val offers = ChallengeCodec.decode(row.offersJson)
                    val valid = offers.any { it.id == challengeId } &&
                        ShrineEngine.canPick(row.status, weekStart, LocalDate.now())
                    valid && db.challengeDao().pick(row.weekStart, challengeId, WeekStatus.ACTIVE) > 0
                }
            }
        }
        if (ok) settleWeeks()
        return ok
    }

    /**
     * Idempotent. Settles every unfinished week (a complete card wins at once, an incomplete one loses only
     * when its week is over, an unpicked week is skipped), pays rewards exactly once inside the same
     * transaction, then opens this week's cards if they do not exist yet.
     */
    private suspend fun settleWeeks() = withContext(Dispatchers.IO) {
        settleLock.withLock {
            val ledgerNow = db.dayDao().getAll().toLedger()
            if (ledgerNow.isNotEmpty()) {
                val today = LocalDate.now()
                val zone = ZoneId.systemDefault()
                val buckets = db.bucketDao().getAll().map { StepSample(it.startMs, it.steps) }
                val goalNow = prefs.goal()
                db.withTransaction {
                    db.inventoryDao().insertIfAbsent(Inventory())
                    for (row in db.challengeDao().getAll()) {
                        if (WeekStatus.isSettled(row.status)) continue
                        val offers = ChallengeCodec.decode(row.offersJson)
                        val picked = offers.firstOrNull { it.id == row.picked }
                        val weekStart = LocalDate.parse(row.weekStart)
                        val over = ShrineEngine.isOver(weekStart, today)
                        val progress = if (picked == null) null else ShrineEngine.progressOf(picked, ledgerNow, buckets, zone)
                        val next = ShrineEngine.nextStatus(row.status, picked, progress, over)
                        if (WeekStatus.isSettled(next)) {
                            val now = System.currentTimeMillis()
                            db.challengeDao().resolve(row.weekStart, next, now)
                            if (next == WeekStatus.WON && picked != null) award(picked, now)
                        }
                    }
                    val currentStart = ChallengeGenerator.weekStartFor(today)
                    if (db.challengeDao().get(currentStart.toString()) == null) {
                        val offers = ChallengeGenerator.offersFor(currentStart, ledgerNow, goalNow, hourlyAvailable = buckets.isNotEmpty())
                        db.challengeDao().insert(ChallengeWeek(currentStart.toString(), ChallengeCodec.encode(offers)))
                    }
                }
            }
        }
    }

    /** Pays a won challenge: XP event (once per challenge id) and Mender Shards (3 shards = 1 Mender, cap 2). */
    private suspend fun award(c: Challenge, now: Long) {
        if (db.xpDao().countFor(XP_TYPE_CHALLENGE, c.id) > 0) return
        db.xpDao().insert(XpEvent(epochMs = now, type = XP_TYPE_CHALLENGE, amount = c.xp, ref = c.id))
        if (c.shards > 0) {
            val inv = db.inventoryDao().get() ?: Inventory()
            val stash = MenderRules.addShards(MenderStash(inv.shards, inv.menders), c.shards)
            db.inventoryDao().set(stash.shards, stash.menders)
        }
    }

    // ---- Profile actions -----------------------------------------------------------------------

    suspend fun setProfileName(name: String) = uiPrefs.setName(name.trim().ifEmpty { UiPrefs.DEFAULT_NAME }.take(20))
    suspend fun setProfileTitle(id: String) = uiPrefs.setTitle(id)
    suspend fun setEquipped(key: String?) = uiPrefs.setEquipped(key)
    suspend fun setShowcase(keys: List<String>) = uiPrefs.setShowcase(keys)

    // ---- Celebration detection -----------------------------------------------------------------

    /**
     * Compares the current state with what was last shown. The first run records the current state silently,
     * so installing this build over an old one does not replay your whole history.
     */
    private suspend fun checkCelebrations(s: ProgressSnapshot, rows: List<ChallengeWeek>, j: JourneyPosition) {
        val seen = uiPrefs.seen()
        val out = ArrayList<Celebration>()

        val level = s.level.level
        val seenLevel = seen.level
        if (seenLevel == null) {
            uiPrefs.setSeenLevel(level)
        } else if (level > seenLevel) {
            out.add(Celebration.LevelUp(s.level, Tier.forLevel(seenLevel) != s.level.tier))
            uiPrefs.setSeenLevel(level)
        }

        val stage = s.flame.stage
        val seenStage = seen.stage
        if (seenStage == null) {
            uiPrefs.setSeenStage(stage)
        } else if (stage != seenStage) {
            if (stage > seenStage) out.add(Celebration.FlameStage(s.flame, s.streak.total))
            uiPrefs.setSeenStage(stage)
        }

        val reached = j.reached.size
        val seenWaypoints = seen.waypoints
        if (seenWaypoints == null) {
            uiPrefs.setSeenWaypoints(reached)
        } else if (reached > seenWaypoints) {
            val wp = j.reached.last()
            val chapter = story.chapters.firstOrNull { it.id == wp.chapterId }
            out.add(Celebration.WaypointReached(wp, chapter?.title ?: ""))
            uiPrefs.setSeenWaypoints(reached)
        }

        val seenResolved = seen.resolvedAt
        if (seenResolved == null) {
            uiPrefs.setSeenResolved(System.currentTimeMillis())
        } else {
            val fresh = rows
                .filter { it.status == WeekStatus.WON && (it.resolvedAtEpochMs ?: 0L) > seenResolved }
                .sortedBy { it.resolvedAtEpochMs ?: 0L }
            for (r in fresh) {
                val c = ChallengeCodec.decode(r.offersJson).firstOrNull { it.id == r.picked }
                if (c != null) out.add(Celebration.ChallengeWon(c))
            }
            if (fresh.isNotEmpty()) uiPrefs.setSeenResolved(fresh.maxOf { it.resolvedAtEpochMs ?: 0L })
        }

        if (out.isNotEmpty()) _celebrations.value = _celebrations.value + out
    }

    companion object {
        const val XP_TYPE_CHALLENGE = "CHALLENGE"
    }
}
