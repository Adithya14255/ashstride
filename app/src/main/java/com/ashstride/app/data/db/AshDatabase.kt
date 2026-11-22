package com.ashstride.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        DayEntry::class,
        XpEvent::class,
        ChallengeWeek::class,
        BadgeUnlock::class,
        Inventory::class,
        MendLog::class,
        StepBucket::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class AshDatabase : RoomDatabase() {
    abstract fun dayDao(): DayDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun xpDao(): XpDao
    abstract fun mendDao(): MendDao
    abstract fun challengeDao(): ChallengeDao
    abstract fun bucketDao(): BucketDao
    abstract fun badgeDao(): BadgeDao

    companion object {
        /** Session 5: adds the 15-minute step buckets. Nothing existing is touched, so history and streaks survive. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `step_bucket` " +
                        "(`startMs` INTEGER NOT NULL, `steps` INTEGER NOT NULL, PRIMARY KEY(`startMs`))",
                )
            }
        }
    }
}
