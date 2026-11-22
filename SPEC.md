# Ashstride - product spec (v1.0, locked)

Android step-tracking game for one person (OnePlus 12, OxygenOS). Offline, no account, no ads, no cloud.
A traveler walks out of the ashes toward a far destination; every step feeds the fire.

## 1. Stack
Kotlin, Jetpack Compose (Material 3), Room, WorkManager, DataStore, Health Connect client, home-screen widget (plain RemoteViews; Glance was dropped in Session 6, see PROJECT_STATE).
minSdk 29, targetSdk 35. Debug-signed APK built by GitHub Actions. All art (flame, emblems, badges) is drawn in code: no image assets.

## 2. Data and architecture
- **Ledger** (`day_entry`): one row per date: steps, distanceKm, source (IMPORTED | HEALTH_CONNECT), goal that applied that day, mended flag.
- Seeded once from `assets/seed_steps.json` (505 days, 2025-07-04 to 2026-11-20, 6,406,335 steps, 4,932 km). Seeded rows use goal 10,000.
- Streaks, XP, levels, badges are **computed from the ledger + event tables**, never kept as counters, so mends and imports can never leave them inconsistent. Badge evaluation is idempotent.
- Pure-Kotlin `domain` package (StreakEngine, XpEngine, ChallengeGenerator, BadgeEvaluator) with unit tests. UI never contains rules.
- Health Connect: read steps + distance via aggregate API (it de-duplicates across sources); manual-entry records are excluded. Sync via WorkManager + change tokens. Needs history permission for data older than 30 days and a permission-rationale screen.
- Write-back of imported history to Health Connect: optional, **off by default** (double-count risk).
- Imported history is daily-only. Hourly challenges only count data recorded after install.

## 3. Goal
Daily goal defaults to **10,000**, selectable. Shown on every screen header and on the ring.
Each ledger day stores the goal active that day; changing it never rewrites history.
XP and rewards scale by goal / 10,000, clamped 0.5x to 2x. Weekly challenge targets scale with the goal. Mend floor = 25% of goal (2,500 at 10k).

## 4. Levels, tiers, XP
- XP sources: steps (1 XP per 100, max 30,000 steps counted per day, times goal multiplier); daily streak bonus; challenge XP (Easy 300, Medium 700, Hard 1,500, Legendary 3,000, times goal multiplier); badge XP.
- Streak bonus per day = min(20 + 2 x flame stage, 60). Pure-streak days pay 100%, days that exist only because of a mend pay 60%.
- Levels 1-100 then **Immortal: uncapped**, flat XP per level, new rank (aura) every 25 levels (Immortal I, II, III...).
- Curve tuned in Session 2 so the imported history lands around **Lv 80-85** (Paragon). The exact landing level is printed before install.
- Tiers: Wanderer 1-9, Pathfinder 10-19, Ranger 20-34, Trailblazer 35-49, Vanguard 50-64, Warden 65-79, Paragon 80-99, Immortal 100+.
- Each tier has a drawn emblem that gains gems within the tier. Any earned badge can be equipped as the profile emblem.

## 5. Streak and flame
- Two counters: **Pure** (natural days only) and **Total** (including mends). A day counts if steps >= that day's goal, or it is mended.
- Flame is ten crystal tiers (see PROJECT_STATE Session 7): tier = stage / 2, a step every 5 days inside a tier.
- Flame stage = Total streak / 5. New look every 5 days, plus slight daily growth so something changes every day.
- Stages: 0-4 ash-grey smolder; 5-9 red; 10-14 deep orange; 15-19 orange; 20-24 amber; 25-29 gold; 30-34 yellow; 35-39 white-hot; 40-44 pale blue; 45-49 electric blue; 50-54 cyan; 55-59 teal; 60-64 emerald; 65-69 lime; 70-74 violet; 75-79 purple; 80-84 magenta; 85-89 rose; 90-94 crimson-gold; 95-99 golden-white; **100+ Solar** (sparks, +1 ring or layer every 5 days); 200+ prismatic cycle.
- A run containing a mend shows a thin gold-thread seam on the flame.
- Calendar heatmap with distinct marks for met, missed, mended. "At risk" evening alert (configurable time). Streak history and best run.
- Milestone badges (7, 14, 30, 50, 100, 150, 200, 365...) in two variants: **Pure** (gold, full XP) and **Mended** (silver-stitched, about 60% XP). Existing milestones are granted retroactively at import with their real dates.

## 6. Streak Mender
- Resource chain: **3 Mender Shards -> 1 Mender**. Inventory cap: 2 Menders.
- Shards come only from Hard (1) and Legendary (2) weekly challenges.
- A Mender fixes **one** day: it must be below goal and at or above the floor, in the past (**look-back unlimited**) or today. Two-day gaps need two Menders.
- Before confirming, the app previews the result (e.g. "Total streak 118 -> 251"). Using a Mender is irreversible.
- A mended day counts for streak and badges only: no steps, no XP.
- Reference on the imported data: Jul 25 2026 mend -> 251; all five gaps back to Sep 20 2025 (5 Menders) -> 427.

## 7. Weekly challenges (story: the weekend shrine)
- Week runs **Saturday to Friday**. The traveler reaches the shrine on Saturday: last week's result is settled, three sealed cards appear, you **pick one** and it locks. Progress counts from the start of the cycle.
- Cards: Easy, Medium, Hard. Every 4th week Hard becomes **Legendary**.
- Targets adapt to the last 8 weeks and the current goal. At goal 10k, roughly (re-balanced in Session 6 so every card of a tier is about as hard as its siblings, measured against the imported history): Easy 66k week, or 5 days at goal, or a 20k weekend; Medium 80k, or 5 days of 11k+, or 62 km; Hard 88k, or 3 days of 13k+, or one 18k day; Legendary 102k or a 23k day. Each card shows how often it would have been cleared in your last 26 weeks.
- Challenge types: weekly total, days above a threshold, single-day peak, distance, weekend-only, and **Continuous Run** (most steps in a sliding window): Easy 4k/1h, Medium 5k/1h, Hard 10k/2h, Legendary 15k/3h.
- Rewards: Easy/Medium = badge + XP. Hard = badge + XP + 1 shard. Legendary = prismatic badge + XP + 2 shards. Badge rim: bronze, silver, gold, prismatic.

## 8. Story layer
- `story.json` (data-driven): chapters, waypoints by cumulative km, shrine scenes. Lore is written later with no code changes.
- Placeholder premise: a wanderer walks out of the ashes toward a far destination. Route length is tuned so the imported 4,932 km lands partway along, not at the end.

## 9. Screens
Home (ring, flame, XP bar, tier emblem), Streak (calendar + mend flow), Shrine (weekly cards), Journey, Badges, Stats (day-of-week, monthly, records, distance), Profile, Settings.
Profile (on-device only): name, generated avatar, earned titles, equipped emblem and aura, 3-badge showcase, career stats, optional export of a profile card image.

## 10. Notifications, widget, data
At-risk alert, shrine-open (Saturday), milestone alerts, quiet hours. Home-screen widget (steps, goal ring, flame). CSV/JSON export and import, local backup. OxygenOS battery-optimization guide for reliable background sync.

## 11. Decisions log
10k default goal, selectable | one challenge per week | mend floor 25% of goal | write-back off by default | unlimited mend look-back | name Ashstride | flame changes every 5 days | pure pays more than mended | no real-world comparison or percentile ranking | story framing with weekend shrine.

## 12. Build plan
1 skeleton + seed + CI (done) | 2 rules engine + tests (done) | 3 Health Connect (done) |
4 navigation shell, Home (ring, flame, XP bar, tier emblem), Streak calendar + Mend flow, Stats, Badges list, goal selector |
5 Shrine + weekly challenges, Journey + story.json, Profile, emblem/tier/badge art, animations |
6 notifications, widget, settings, export/import, OxygenOS battery guide, polish + release.
