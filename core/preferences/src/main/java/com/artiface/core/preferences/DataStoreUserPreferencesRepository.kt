package com.artiface.core.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.artiface.core.model.AppThemeMode
import com.artiface.core.model.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataStoreUserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : UserPreferencesRepository {

    override val preferences: Flow<UserPreferences> = dataStore.data.map { prefs ->
        UserPreferences(
            hasCompletedOnboarding = prefs[Keys.OnboardingCompleted] ?: false,
            themeMode = prefs[Keys.ThemeMode].toThemeMode(),
            contextualPersonalizationEnabled = prefs[Keys.ContextualPersonalization] ?: false,
            locationContextEnabled = prefs[Keys.LocationContext] ?: false,
        )
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { it[Keys.OnboardingCompleted] = completed }
    }

    override suspend fun setThemeMode(mode: AppThemeMode) {
        dataStore.edit { it[Keys.ThemeMode] = mode.name }
    }

    override suspend fun setContextualPersonalizationEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.ContextualPersonalization] = enabled
            if (!enabled) {
                // Location context is meaningless without personalization.
                prefs[Keys.LocationContext] = false
            }
        }
    }

    override suspend fun setLocationContextEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.LocationContext] = enabled
            if (enabled) {
                prefs[Keys.ContextualPersonalization] = true
            }
        }
    }

    private object Keys {
        val OnboardingCompleted = booleanPreferencesKey("onboarding_completed")
        val ThemeMode = stringPreferencesKey("theme_mode")
        val ContextualPersonalization = booleanPreferencesKey("contextual_personalization")
        val LocationContext = booleanPreferencesKey("location_context")
    }

    private fun String?.toThemeMode(): AppThemeMode =
        runCatching { AppThemeMode.valueOf(this ?: AppThemeMode.System.name) }
            .getOrDefault(AppThemeMode.System)
}
