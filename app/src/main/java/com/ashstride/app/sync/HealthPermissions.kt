package com.ashstride.app.sync

/** Permission strings written out so they cannot drift with library versions. */
object HealthPermissions {
    const val READ_STEPS = "android.permission.health.READ_STEPS"
    const val READ_DISTANCE = "android.permission.health.READ_DISTANCE"
    const val READ_HISTORY = "android.permission.health.READ_HEALTH_DATA_HISTORY"
    const val READ_BACKGROUND = "android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND"

    /** Core data permissions: sync cannot run without these. */
    val required: Set<String> = setOf(READ_STEPS, READ_DISTANCE)

    /** Everything we ask for in one dialog. History and background are optional extras. */
    val all: Set<String> = required + READ_HISTORY + READ_BACKGROUND
}
