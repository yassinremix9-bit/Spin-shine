package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AppThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "spin_shine_settings")

data class AppSettings(
    val isOnlineMode: Boolean = true,
    val pushNotificationsEnabled: Boolean = true,
    val cycleCompleteAlert: Boolean = true,
    val endingSoonAlert: Boolean = true,
    val soundAndVibration: Boolean = true,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM
)

class SettingsDataStore(private val context: Context) {
    private object Keys {
        val IS_ONLINE_MODE = booleanPreferencesKey("is_online_mode")
        val PUSH_NOTIFICATIONS = booleanPreferencesKey("push_notifications")
        val CYCLE_COMPLETE_ALERT = booleanPreferencesKey("cycle_complete_alert")
        val ENDING_SOON_ALERT = booleanPreferencesKey("ending_soon_alert")
        val SOUND_VIBRATION = booleanPreferencesKey("sound_vibration")
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            isOnlineMode = prefs[Keys.IS_ONLINE_MODE] ?: true,
            pushNotificationsEnabled = prefs[Keys.PUSH_NOTIFICATIONS] ?: true,
            cycleCompleteAlert = prefs[Keys.CYCLE_COMPLETE_ALERT] ?: true,
            endingSoonAlert = prefs[Keys.ENDING_SOON_ALERT] ?: true,
            soundAndVibration = prefs[Keys.SOUND_VIBRATION] ?: true,
            themeMode = AppThemeMode.fromKey(prefs[Keys.THEME_MODE] ?: AppThemeMode.SYSTEM.storageKey)
        )
    }

    suspend fun setOnlineMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.IS_ONLINE_MODE] = enabled }
    }

    suspend fun setPushNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.PUSH_NOTIFICATIONS] = enabled }
    }

    suspend fun setCycleCompleteAlert(enabled: Boolean) {
        context.dataStore.edit { it[Keys.CYCLE_COMPLETE_ALERT] = enabled }
    }

    suspend fun setEndingSoonAlert(enabled: Boolean) {
        context.dataStore.edit { it[Keys.ENDING_SOON_ALERT] = enabled }
    }

    suspend fun setSoundAndVibration(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SOUND_VIBRATION] = enabled }
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.storageKey }
    }
}
