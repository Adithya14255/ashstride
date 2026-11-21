# Ashstride - project state

Paste SPEC.md and this file at the start of every new session.

## Done
- **Session 1 (build 0.1.0):** Gradle project, version catalog, ash/ember Compose theme, Room schema (day_entry, xp_event, challenge_week, badge_unlock, inventory, mend_log), one-time seed of 505 days, placeholder streak counter on Home, fixed debug keystore, GitHub Actions workflow producing the APK.
- **Session 2 (build 0.2.0):** pure-Kotlin rules engine in `com.ashstride.app.domain` plus JUnit tests. No UI or database changes; Home still uses the Session 1 `SimpleStreak` and looks identical.
- **Session 3 (build 0.3.0, written, NOT yet compiled or installed):** Health Connect sync in `com.ashstride.app.sync`: aggregate-API daily steps + distance, manual entries subtracted, changes token, WorkManager every 30 min, permission flow + rationale screen, Health Connect card on Home. `SimpleStreak` still drives Home. CI unit tests are now a hard gate (continue-on-error removed). 16 new tests in `SyncPlannerTest` (expected total 88).

## Verified
- Session 1 build compiles in GitHub Actions and installs on the OnePlus 12. Home shows the seeded history.
- Session 2 build (`0.2.0`) compiles in GitHub Actions, installs on the OnePlus 12, and launches cleanly with the expected baseline on Home.
- Session 2 unit test suite (`testDebugUnitTest`, Gradle 8.9, 2026-11-21): **72/72 tests passed** (`0` failures, `0` errors, `0` skipped) across `BadgeEvaluatorTest` (9), `ChallengeGeneratorTest` (24), `FlameEngineTest` (6), `StreakEngineTest` (15), and `XpLevelTest` (18).
- CI fixes already applied: removed android-actions/setup-android (replaced by an sdkmanager licenses step),
  workflow uses checkout@v5 and setup-java@v5, gradle/libs.versions.toml added by hand.
- Repo: github.com/Adithya14255/ashstride (public).

## To verify after pushing Session 3
- CI green with 88 tests (72 old + 16 `SyncPlannerTest`). Paste red lines into the next session if not. No compiler was available when this was written, so a first-build compile error is possible.
- Install over 0.2.0 (same key, data kept). Home shows the old numbers plus a Health Connect card. Tap Allow access, grant all four, card should say Synced and today's row appears (days loaded 506, steps rise by today's count).

## Expected on first install (0.3.0)
Before granting access: same as 0.2.0 (streak 118, days 505, steps 6,406,335, 4,932 km, Goal 10,000) plus the Health Connect card. After granting: days loaded becomes 506 (today's row, source HEALTH_CONNECT) and total steps grow by today's steps. The streak on Home may read 119 once today passes 10,000 steps (SimpleStreak counts today).

## Session 2 results on the imported history (as of 2026-11-21)
- Streaks: pure 118, total 118, best pure 132, best total 132.
- Mend Jul 25 2026: total 118 -> 251 (pure stays 118). Five mends (Oct 16 2025, Feb 8, Feb 9, Mar 14, Jul 25 2026) -> 427.
- XP: steps 63,417 + streak bonus 18,016 + badges 8,700 = **90,133 XP**.
- **Level 82, Paragon** (304 / 2,093 XP into the level). Landing target was Lv 80-85.
- Badges retroactively earned (11): streak 7 (2025-09-05), 14 (2025-09-12), 30 (2025-11-15), 50 (2025-12-05), 100 (2026-01-24), all Pure; lifetime steps 100k, 250k, 500k, 1M, 2.5M, 5M.
- Flame at 118: Solar, 3 layers.

## Domain API (all in app/src/main/java/com/ashstride/app/domain/)
- `LedgerDay` (+ `DayEntry.toLedgerDay()`, `List<DayEntry>.toLedger()`): UI/DB rows to pure model.
- `StreakEngine`: `timeline`, `summary(days, today)` -> `StreakSummary(pure, total, bestPure, bestTotal, runHasMend, atRisk)`, `checkMend`, `previewMend` -> `MendPreview(check, totalBefore, totalAfter)`, `withMend`, `mendFloor(goal)`, `statusOf`.
- `MenderRules` / `MenderStash`: 3 shards -> 1 Mender, cap 2, `addShards`, `use`.
- `FlameEngine.info(total, runHasMend)` -> `FlameInfo` (stage, `FlameLook`, growth, solarLayers, hueShiftDegrees, hasMendSeam).
- `XpEngine`: `stepsXp`, `streakBonus`, `scaled`, `goalMultiplier`, `dayXp`, `ledgerXp`, `breakdown`.
- `LevelCurve.infoFor(totalXp)` -> `LevelInfo` (level, tier, gems, immortalRank, rankTitle, progress); `Tier`.
- `BadgeEvaluator`: `evaluate(days)`, `newlyEarned(days, unlockedKeys)`; `BadgeVariant` PURE / MENDED / STANDARD, `BadgeKey`.
- `ChallengeGenerator`: `weekStartFor`, `offersFor(weekStart, days, goal, hourlyAvailable)`; `ChallengeProgressEngine.evaluate` and `bestWindow` (continuous run).
- `Progress.compute(days, today, otherXp)` -> `ProgressSnapshot`: one call for everything the Home screen needs.

## Rule decisions made in Session 2 (not spelled out in SPEC.md; change here if you disagree)
- Pure counter resets on a mended day and restarts from natural days; Total keeps counting. Flame seam shows when Total != Pure.
- Today in progress never breaks a streak: if today is not done yet the streak carries from yesterday and is flagged `atRisk`. A date with no ledger row breaks a run like a missed day.
- Mend floor uses that day's own stored goal (goal / 4). Mending is allowed on a past day or today, and only on a day that is below goal, at or above floor, and not already mended.
- Streak bonus = min(20 + 2 x stage, 60), not scaled by goal. Natural days that sit on top of a mend (Pure < Total) pay 60%, rounded down; the mended day itself pays 0 XP.
- Steps XP and challenge XP scale by goal / 10,000 clamped 0.5x-2x using integer maths (rounded down).
- Level curve: level L costs 125 + 24 x L XP up to level 100 (Lv 100 starts at 131,175 XP); every level from 100 costs a flat 2,600 XP, uncapped; Immortal rank changes every 25 levels (Immortal I = 100-124, II = 125-149 ...). Gems on a tier emblem = (level - tier start) / 5, max 4.
- Badge catalog so far: streak milestones 7, 14, 30, 50, 100, 150, 200, 300, 365, 500, 730, 1000 (Pure XP 100, 200, 500, 800, 1,500, 2,000, 2,500, 3,500, 5,000, 6,500, 9,000, 12,000; Mended = 60% rounded) and lifetime step milestones 100k, 250k, 500k, 1M, 2.5M, 5M, 10M (XP 150, 250, 400, 800, 1,500, 2,500, 4,000; variant STANDARD). Challenge badges arrive with Session 5. A Mended badge and a Pure badge for the same milestone can both be earned.
- Weekly cards: week is Saturday-Friday, anchor Saturday 2025-07-05, every 4th week (index % 4 == 3) the Hard card is Legendary. Targets = spec defaults x goal scale x an adaptation factor from the last 8 full weeks (average / 85,000, clamped 0.9-1.15), rounded to 1,000. Card type rotates week by week; Continuous Run is only offered when hourly data exists. Distance target = step target x 0.00075 km.
- Offers are deterministic for the same inputs, so reopening never reshuffles; Session 5 must still store them in `challenge_week.offersJson`.

## Session 3 decisions (not in SPEC.md; change here if you disagree)
- Imported history wins: HC writes only days after the newest IMPORTED day (2026-11-21 onward today), never overwrites an IMPORTED row, never changes a row's goal or mended flag. New rows get the current goal (`SyncPrefs.goal`, default 10,000; no UI to change it yet, Session 7 settings).
- Manual entries: aggregate total minus manual-entry records, attributed to the record's start day. Approximation if a manual record overlaps a real one.
- Token taken before reading. Any deletion re-reads the trailing 30 days (deletions carry no date). Expired token or first run = full window resync. Quiet days still get a 0-step row.
- Permissions requested together: steps, distance, history, background. Only steps + distance are required; history is floored to 30 days if missing; the 30-minute WorkManager job skips unless background permission is granted, and Home syncs on every open.
- Distance: sums HC distance, so a day's km comes from whatever app writes distance.
- Hourly data is not stored yet (needed for Continuous Run in Session 5; ChallengeGenerator gets `hourlyAvailable=false` until then).
- Write-back to Health Connect not built (stays off).

## Next: Session 4 (Home, Streak, Stats)
Swap Home from `SimpleStreak` to `Progress.compute`; ring, flame, XP bar, tier emblem; Streak calendar; Stats screen. Navigation shell for all screens.

## Open items
- Tier art, emblem looks and badge art are still to come (Session 6).
- Goal selector UI not built yet; goal is stored in `SyncPrefs` only.
