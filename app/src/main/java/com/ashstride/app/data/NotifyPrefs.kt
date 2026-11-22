package com.ashstride.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ashstride.app.domain.NotifySettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.notifyStore by preferencesDataStore(name = "notify_state")

/** Alert settings and the "already sent" markers that keep each alert to once per day or week. */
class NotifyPrefs(private val context: Context) {
    private val atRiskOn = booleanPreferencesKey("at_risk_on")
    private val atRiskAt = intPreferencesKey("at_risk_at")
    private val shrineOn = booleanPreferencesKey("shrine_on")
    private val shrineAt = intPreferencesKey("shrine_at")
    private val milestoneOn = booleanPreferencesKey("milestone_on")
    private val quietOn = booleanPreferencesKey("quiet_on")
    private val quietFrom = intPreferencesKey("quiet_from")
    private val quietTo = intPreferencesKey("quiet_to")
    private val sentRisk = stringPreferencesKey("sent_risk_date")
    private val sentShrine = stringPreferencesKey("sent_shrine_week")

    val settingsFlow: Flow<NotifySettings> = context.notifyStore.data.map { it.toSettings() }

    suspend fun settings(): NotifySettings = context.notifyStore.data.first().toSettings()

    private fun androidx.datastore.preferences.core.Preferences.toSettings(): NotifySettings {
        val d = NotifySettings()
        return NotifySettings(
            atRiskEnabled = this[atRiskOn] ?: d.atRiskEnabled,
            atRiskMinutes = this[atRiskAt] ?: d.atRiskMinutes,
            shrineEnabled = this[shrineOn] ?: d.shrineEnabled,
            shrineMinutes = this[shrineAt] ?: d.shrineMinutes,
            milestonesEnabled = this[milestoneOn] ?: d.milestonesEnabled,
            quietEnabled = this[quietOn] ?: d.quietEnabled,
            quietStartMinutes = this[quietFrom] ?: d.quietStartMinutes,
            quietEndMinutes = this[quietTo] ?: d.quietEndMinutes,
        )
    }

    suspend fun save(s: NotifySettings) {
        context.notifyStore.edit {
            it[atRiskOn] = s.atRiskEnabled
            it[atRiskAt] = s.atRiskMinutes
            it[shrineOn] = s.shrineEnabled
            it[shrineAt] = s.shrineMinutes
            it[milestoneOn] = s.milestonesEnabled
            it[quietOn] = s.quietEnabled
            it[quietFrom] = s.quietStartMinutes
            it[quietTo] = s.quietEndMinutes
        }
    }

    suspend fun sentRiskDate(): String? = context.notifyStore.data.first()[sentRisk]
    suspend fun setSentRiskDate(v: String) { context.notifyStore.edit { it[sentRisk] = v } }
    suspend fun sentShrineWeek(): String? = context.notifyStore.data.first()[sentShrine]
    suspend fun setSentShrineWeek(v: String) { context.notifyStore.edit { it[sentShrine] = v } }
}
