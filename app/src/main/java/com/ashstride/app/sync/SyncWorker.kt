package com.ashstride.app.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.ashstride.app.AshstrideApp
import com.ashstride.app.notify.NotificationEngine
import com.ashstride.app.widget.AshWidgetProvider
import java.util.concurrent.TimeUnit

class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as AshstrideApp
        val result = app.healthSync.sync(background = true)
        // Fresh steps may cross the goal or earn a badge, so check alerts and redraw the widget right away.
        try {
            NotificationEngine.run(app)
            AshWidgetProvider.refresh(app)
        } catch (e: Exception) {
            // Alerts are best effort; the 15-minute reminder job tries again.
        }
        return when (result) {
            is SyncResult.Failed -> Result.retry()
            else -> Result.success()
        }
    }
}

object SyncScheduler {
    private const val NAME = "health_connect_sync"

    /** Every 30 minutes; WorkManager may stretch this under battery restrictions (see the OxygenOS guide, Session 7). */
    fun schedule(context: Context) {
        val req = PeriodicWorkRequestBuilder<SyncWorker>(30, TimeUnit.MINUTES).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.KEEP, req)
    }
}
