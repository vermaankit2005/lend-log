package com.lendlog.app.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_prefs")

@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val IS_UNLOCKED            = booleanPreferencesKey("is_unlocked")
        val ONBOARDING_DONE        = booleanPreferencesKey("onboarding_done")
        val LAST_BACKUP_TIMESTAMP  = longPreferencesKey("last_backup_timestamp")
        val THEME_MODE             = stringPreferencesKey("theme_mode")
        val NOTIFICATIONS_ENABLED  = booleanPreferencesKey("notifications_enabled")
        val REMINDER_DAYS          = intPreferencesKey("reminder_days")
    }

    val isUnlocked: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.IS_UNLOCKED] ?: false
    }

    val onboardingDone: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.ONBOARDING_DONE] ?: false
    }

    val lastBackupTimestamp: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[Keys.LAST_BACKUP_TIMESTAMP] ?: 0L
    }

    val themeMode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[Keys.THEME_MODE] ?: "SYSTEM"
    }

    val notificationsEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.NOTIFICATIONS_ENABLED] ?: true
    }

    val reminderDays: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.REMINDER_DAYS] ?: 3
    }

    suspend fun setUnlocked(unlocked: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.IS_UNLOCKED] = unlocked }
    }

    suspend fun setOnboardingDone(done: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.ONBOARDING_DONE] = done }
    }

    suspend fun setLastBackupTimestamp(timestamp: Long) {
        context.dataStore.edit { prefs -> prefs[Keys.LAST_BACKUP_TIMESTAMP] = timestamp }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { prefs -> prefs[Keys.THEME_MODE] = mode }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setReminderDays(days: Int) {
        context.dataStore.edit { prefs -> prefs[Keys.REMINDER_DAYS] = days }
    }
}
