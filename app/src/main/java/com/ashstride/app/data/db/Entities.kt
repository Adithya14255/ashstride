package com.ashstride.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/** One row per calendar day. [goal] is the goal that applied on that day, so changing the goal never rewrites history. */
@Entity(tableName = "day_entry")
data class DayEntry(
    @PrimaryKey val date: String, // yyyy-MM-dd
    val steps: Int,
    val distanceKm: Double,
    val source: String, // IMPORTED or HEALTH_CONNECT
    val goal: Int,
    val mended: Boolean = false,
    val mendedAtEpochMs: Long? = null,
)

@Entity(tableName = "xp_event")
data class XpEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochMs: Long,
    val type: String,
    val amount: Int,
    val ref: String? = null,
)

/** One row per Saturday-to-Friday challenge week. */
@Entity(tableName = "challenge_week")
data class ChallengeWeek(
    @PrimaryKey val weekStart: String, // the Saturday, yyyy-MM-dd
    val offersJson: String,
    val picked: String? = null,
    val status: String = "OPEN", // OPEN, ACTIVE, WON, LOST
    val resolvedAtEpochMs: Long? = null,
)

@Entity(tableName = "badge_unlock", primaryKeys = ["badgeId", "variant"])
data class BadgeUnlock(
    val badgeId: String,
    val variant: String, // PURE, MENDED or STANDARD
    val unlockedAtEpochMs: Long,
    val awardedForDate: String? = null,
)

@Entity(tableName = "inventory")
data class Inventory(
    @PrimaryKey val id: Int = 1,
    val shards: Int = 0,
    val menders: Int = 0,
)

@Entity(tableName = "mend_log")
data class MendLog(
    @PrimaryKey val date: String,
    val usedAtEpochMs: Long,
)
