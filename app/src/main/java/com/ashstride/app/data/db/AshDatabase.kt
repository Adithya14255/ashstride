package com.ashstride.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        DayEntry::class,
        XpEvent::class,
        ChallengeWeek::class,
        BadgeUnlock::class,
        Inventory::class,
        MendLog::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AshDatabase : RoomDatabase() {
    abstract fun dayDao(): DayDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun xpDao(): XpDao
    abstract fun mendDao(): MendDao
}
