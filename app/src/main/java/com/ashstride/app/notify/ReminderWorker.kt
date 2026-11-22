package com.ashstride.app.notify

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.ashstride.app.AshstrideApp
import com.ashstride.app.widget.AshWidgetProvider
import java.util.concurrent.TimeUnit

/** Every 15 minutes (the WorkManager minimum): check what alerts are due and refresh the widget. */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as AshstrideApp
        try {
            NotificationEngine.run(app)
            AshWidgetProvider.refresh(app)
        } catch (e: Exception) {
            return Result.retry()
        }
        return Result.success()
    }
}

object ReminderScheduler {
    private const val NAME = "ashstride_reminders"

    fun schedule(context: Context) {
        val req = PeriodicWorkRequestBuilder<ReminderWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.KEEP, req)
    }
}
