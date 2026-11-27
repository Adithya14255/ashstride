package com.ashstride.app.sync

import android.content.Context

/** Readable names for the apps that write steps to Health Connect. */
object SourceLabels {
    private val known: Map<String, String> = mapOf(
        "com.google.android.apps.fitness" to "Google Fit",
        "com.google.android.gms" to "Google Play services (phone sensor)",
        "com.sec.android.app.shealth" to "Samsung Health",
        "com.fitbit.FitbitMobile" to "Fitbit",
        "com.garmin.android.apps.connectmobile" to "Garmin Connect",
        "com.strava" to "Strava",
        "com.mi.health" to "Mi Fitness",
        "com.xiaomi.wearable" to "Mi Fitness",
    )

    fun label(context: Context, packageName: String): String {
        known[packageName]?.let { return it }
        return try {
            val pm = context.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
        } catch (e: Exception) {
            packageName
        }
    }
}
