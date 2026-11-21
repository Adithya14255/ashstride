# Ashstride - project state

Paste SPEC.md and this file at the start of every new session.

## Done
- **Session 1 (build 0.1.0):** Gradle project, version catalog, ash/ember Compose theme, Room schema (day_entry, xp_event, challenge_week, badge_unlock, inventory, mend_log), one-time seed of 505 days, placeholder streak counter on Home, fixed debug keystore, GitHub Actions workflow producing the APK.

## Verified
- Session 1 build compiles in GitHub Actions and installs on the OnePlus 12. Home shows the seeded history.
- CI fixes already applied: removed android-actions/setup-android (replaced by an sdkmanager licenses step),
  workflow uses checkout@v5 and setup-java@v5, gradle/libs.versions.toml added by hand.
- Repo: github.com/Adithya14255/ashstride its public
  
## Expected on first install
Home shows a streak of **118**, days loaded **505**, total steps **6,406,335**, total distance **4,932 km**, goal chip **Goal 10,000**.

## Next: Session 2 (rules engine + tests)
StreakEngine (pure/total, mend merge preview), XpEngine + level curve (land seeded history around Lv 80-85, Immortal uncapped), flame stage mapping, ChallengeGenerator (Sat-Fri, adaptive, goal-scaled), BadgeEvaluator (retroactive, pure/mended variants). JUnit tests against the seed data: current pure streak 118, best 132, Jul 25 2026 mend -> 251.

## Open items
None blocking.
