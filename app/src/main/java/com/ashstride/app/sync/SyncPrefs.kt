package com.ashstride.app.sync

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.syncStore by preferencesDataStore(name = "sync_state")

/** Sync bookkeeping and the current daily goal (SPEC section 3: default 10,000). */
class SyncPrefs(private val context: Context) {
    private val tokenKey = stringPreferencesKey("changes_token")
    private val lastSyncDateKey = stringPreferencesKey("last_sync_date")
    private val lastSyncEpochKey = longPreferencesKey("last_sync_epoch_ms")
    private val goalKey = intPreferencesKey("goal")
    private val primaryKey = stringPreferencesKey("primary_source")
    private val autoSourceKey = stringPreferencesKey("auto_source")

    suspend fun token(): String? = context.syncStore.data.first()[tokenKey]
    suspend fun lastSyncDate(): String? = context.syncStore.data.first()[lastSyncDateKey]
    suspend fun goal(): Int = context.syncStore.data.first()[goalKey] ?: DEFAULT_GOAL

    val goalFlow: Flow<Int> = context.syncStore.data.map { it[goalKey] ?: DEFAULT_GOAL }

    val lastSyncEpochMs: Flow<Long?> = context.syncStore.data.map { it[lastSyncEpochKey] }

    suspend fun saveSync(token: String, date: String, epochMs: Long) {
        context.syncStore.edit {
            it[tokenKey] = token
            it[lastSyncDateKey] = date
            it[lastSyncEpochKey] = epochMs
        }
    }

    /** Package the person chose as the only source of steps, or null for Auto. */
    suspend fun primarySource(): String? = context.syncStore.data.first()[primaryKey]
    val primarySourceFlow: Flow<String?> = context.syncStore.data.map { it[primaryKey] }

    /** Package Auto picked (the source with the most steps), kept until the person chooses or resets. */
    suspend fun autoSource(): String? = context.syncStore.data.first()[autoSourceKey]
    val autoSourceFlow: Flow<String?> = context.syncStore.data.map { it[autoSourceKey] }

    suspend fun setAutoSource(pkg: String) {
        context.syncStore.edit { it[autoSourceKey] = pkg }
    }

    /**
     * Chooses the single source steps are read from; null goes back to Auto. The change token is dropped so the
     * next sync re-reads every Health Connect day from this source alone.
     */
    suspend fun setPrimarySource(pkg: String?) {
        context.syncStore.edit {
            if (pkg == null) {
                it.remove(primaryKey)
                it.remove(autoSourceKey)
            } else {
                it[primaryKey] = pkg
            }
            it.remove(tokenKey)
        }
    }

    suspend fun clearToken() {
        context.syncStore.edit { it.remove(tokenKey) }
    }

    suspend fun setGoal(goal: Int) {
        context.syncStore.edit { it[goalKey] = goal }
    }

    companion object { const val DEFAULT_GOAL = 10_000 }
}
