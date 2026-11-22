package com.ashstride.app.notify

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.ashstride.app.AshstrideApp
import com.ashstride.app.MainActivity
import com.ashstride.app.R
import com.ashstride.app.data.NotifyPrefs
import com.ashstride.app.data.db.BadgeUnlock
import com.ashstride.app.domain.BadgeCatalog
import com.ashstride.app.domain.BadgeEvaluator
import com.ashstride.app.domain.BadgeKey
import com.ashstride.app.domain.BadgeVariant
import com.ashstride.app.domain.ChallengeGenerator
import com.ashstride.app.domain.NotificationRules
import com.ashstride.app.domain.Progress
import com.ashstride.app.domain.toLedger
import com.ashstride.app.sync.SyncPrefs
import com.ashstride.app.ui.Fmt
import java.time.LocalDateTime

/**
 * Decides which alerts are due and posts them. Safe to call as often as you like: every alert has an
 * "already sent" marker, and the rules themselves live in the tested [NotificationRules].
 */
object NotificationEngine {
    const val CH_RISK = "streak_risk"
    const val CH_SHRINE = "shrine_open"
    const val CH_MILESTONE = "milestones"
    private const val ID_RISK = 1001
    private const val ID_SHRINE = 1002
    private const val ID_MILESTONE = 1003

    fun ensureChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(
            NotificationChannel(CH_RISK, "Streak at risk", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "An evening nudge when today is still below your goal."
            },
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_SHRINE, "The shrine opens", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Saturday: this week's three trial cards are ready."
            },
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_MILESTONE, "Milestones", NotificationManager.IMPORTANCE_LOW).apply {
                description = "New badges for streaks and lifetime steps."
            },
        )
    }

    fun canPost(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return false
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    @SuppressLint("MissingPermission")
    private fun post(context: Context, channel: String, id: Int, title: String, text: String): Boolean {
        if (!canPost(context)) return false
        val open = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(context, id, open, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val n = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_stat_flame)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setColor(0xFFFF6B2C.toInt())
            .build()
        NotificationManagerCompat.from(context).notify(id, n)
        return true
    }

    /** A test notification from Settings. Returns false when notifications are blocked. */
    fun sendTest(context: Context): Boolean {
        ensureChannels(context)
        return post(context, CH_MILESTONE, ID_MILESTONE, "Ashstride", "Alerts are working. The fire is fed.")
    }

    suspend fun run(app: AshstrideApp) {
        val db = app.db
        val prefs = NotifyPrefs(app)
        val s = prefs.settings()
        val ledger = db.dayDao().getAll().toLedger()
        if (ledger.isEmpty()) return
        ensureChannels(app)

        val now = LocalDateTime.now()
        val today = now.toLocalDate()
        val minute = now.hour * 60 + now.minute
        val goalNow = SyncPrefs(app).goal()
        val snap = Progress.compute(ledger, today, 0L)

        // Milestones. The first run records the existing history silently so it never floods you.
        val awards = BadgeEvaluator.evaluate(ledger)
        val known = db.badgeDao().getAll().map { BadgeKey(it.badgeId, BadgeVariant.valueOf(it.variant)) }.toSet()
        val stamp = System.currentTimeMillis()
        if (known.isEmpty()) {
            db.badgeDao().insertAll(awards.map { BadgeUnlock(it.badgeId, it.variant.name, stamp, it.date.toString()) })
        } else {
            val fresh = awards.filter { it.key !in known }
            if (fresh.isNotEmpty() && (!s.milestonesEnabled || !NotificationRules.inQuietHours(s, minute))) {
                if (s.milestonesEnabled) {
                    val titles = fresh.map {
                        BadgeCatalog.titleForId(it.badgeId) + if (it.variant == BadgeVariant.MENDED) " (Mended)" else ""
                    }
                    post(app, CH_MILESTONE, ID_MILESTONE, "New badge", NotificationRules.milestoneText(titles))
                }
                db.badgeDao().insertAll(fresh.map { BadgeUnlock(it.badgeId, it.variant.name, stamp, it.date.toString()) })
            }
        }

        // Streak at risk.
        val row = ledger.firstOrNull { it.date == today }
        val steps = row?.steps ?: 0
        val goal = row?.goal ?: goalNow
        if (NotificationRules.shouldAlertAtRisk(s, minute, steps, goal, snap.streak.total, prefs.sentRiskDate(), today)) {
            val left = (goal - steps).coerceAtLeast(0)
            val text = "${Fmt.n(left)} more steps keeps your ${Fmt.n(snap.streak.total)}-day streak alive."
            if (post(app, CH_RISK, ID_RISK, "Your flame needs feeding", text)) prefs.setSentRiskDate(today.toString())
        }

        // The shrine opens on Saturday.
        val weekStart = ChallengeGenerator.weekStartFor(today)
        val week = db.challengeDao().get(weekStart.toString())
        if (NotificationRules.shouldAlertShrine(s, minute, today, week?.status, prefs.sentShrineWeek())) {
            if (post(app, CH_SHRINE, ID_SHRINE, "The shrine is open", "Three sealed cards wait. Choose this week's trial.")) {
                prefs.setSentShrineWeek(weekStart.toString())
            }
        }
    }
}
