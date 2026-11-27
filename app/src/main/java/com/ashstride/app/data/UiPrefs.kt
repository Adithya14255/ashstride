package com.ashstride.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.uiStore by preferencesDataStore(name = "ui_state")

/** What the celebration system has already shown. A null field means "never recorded": the first run sets it silently. */
data class SeenMarks(
    val level: Int?,
    val stage: Int?,
    val waypoints: Int?,
    val resolvedAt: Long?,
)

/** Profile choices and celebration bookkeeping. Everything stays on the device. */
class UiPrefs(private val context: Context) {
    private val nameKey = stringPreferencesKey("profile_name")
    private val titleKey = stringPreferencesKey("profile_title")
    private val equippedKey = stringPreferencesKey("equipped_badge")
    private val showcaseKey = stringPreferencesKey("showcase_badges")
    private val seenLevelKey = intPreferencesKey("seen_level")
    private val seenStageKey = intPreferencesKey("seen_stage")
    private val seenWaypointsKey = intPreferencesKey("seen_waypoints")
    private val seenResolvedKey = longPreferencesKey("seen_resolved_at")
    private val offersVersionKey = intPreferencesKey("offers_version")
    private val rewardsBaselinedKey = booleanPreferencesKey("rewards_baselined")

    val nameFlow: Flow<String> = context.uiStore.data.map { it[nameKey] ?: DEFAULT_NAME }
    val titleFlow: Flow<String?> = context.uiStore.data.map { it[titleKey] }
    val equippedFlow: Flow<String?> = context.uiStore.data.map { it[equippedKey] }
    val showcaseFlow: Flow<List<String>> = context.uiStore.data.map { prefs ->
        val raw = prefs[showcaseKey].orEmpty()
        if (raw.isEmpty()) emptyList() else raw.split(",")
    }

    suspend fun setName(name: String) { context.uiStore.edit { it[nameKey] = name } }
    suspend fun setTitle(id: String) { context.uiStore.edit { it[titleKey] = id } }
    suspend fun setEquipped(key: String?) {
        context.uiStore.edit { if (key == null) it.remove(equippedKey) else it[equippedKey] = key }
    }
    suspend fun setShowcase(keys: List<String>) {
        context.uiStore.edit { it[showcaseKey] = keys.take(SHOWCASE_SIZE).joinToString(",") }
    }

    suspend fun offersVersion(): Int = context.uiStore.data.first()[offersVersionKey] ?: 1
    suspend fun setOffersVersion(v: Int) { context.uiStore.edit { it[offersVersionKey] = v } }

    /** False until the shard-reward system has run once; that first run marks past rewards as paid without paying them. */
    suspend fun rewardsBaselined(): Boolean = context.uiStore.data.first()[rewardsBaselinedKey] ?: false
    suspend fun setRewardsBaselined(v: Boolean) { context.uiStore.edit { it[rewardsBaselinedKey] = v } }

    suspend fun seen(): SeenMarks {
        val p = context.uiStore.data.first()
        return SeenMarks(p[seenLevelKey], p[seenStageKey], p[seenWaypointsKey], p[seenResolvedKey])
    }

    suspend fun setSeenLevel(v: Int) { context.uiStore.edit { it[seenLevelKey] = v } }
    suspend fun setSeenStage(v: Int) { context.uiStore.edit { it[seenStageKey] = v } }
    suspend fun setSeenWaypoints(v: Int) { context.uiStore.edit { it[seenWaypointsKey] = v } }
    suspend fun setSeenResolved(v: Long) { context.uiStore.edit { it[seenResolvedKey] = v } }

    companion object {
        const val DEFAULT_NAME = "Wanderer"
        const val SHOWCASE_SIZE = 3
    }
}
