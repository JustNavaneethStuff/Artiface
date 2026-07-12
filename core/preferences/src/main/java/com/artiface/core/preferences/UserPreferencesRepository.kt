package com.artiface.core.preferences

import com.artiface.core.model.AppThemeMode
import com.artiface.core.model.UserPreferences
import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    val preferences: Flow<UserPreferences>

    suspend fun setOnboardingCompleted(completed: Boolean)

    suspend fun setThemeMode(mode: AppThemeMode)

    suspend fun setContextualPersonalizationEnabled(enabled: Boolean)

    suspend fun setLocationContextEnabled(enabled: Boolean)
}
