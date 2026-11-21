package com.ashstride.app.sync

sealed interface SyncResult {
    data class Synced(val daysChecked: Int, val daysWritten: Int, val atEpochMs: Long) : SyncResult
    /** Health Connect missing or needs an update. [updateRequired] distinguishes the two. */
    data class Unavailable(val updateRequired: Boolean) : SyncResult
    data object NeedsPermission : SyncResult
    /** Background run without the background-read permission: skipped, foreground sync will do it. */
    data object Skipped : SyncResult
    data class Failed(val message: String) : SyncResult
}
