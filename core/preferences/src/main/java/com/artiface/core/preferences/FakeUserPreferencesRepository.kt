package com.artiface.core.preferences

import com.artiface.core.model.AppThemeMode
import com.artiface.core.model.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * In-memory preferences for unit tests and Compose previews.
 */
class FakeUserPreferencesRepository(
    initial: UserPreferences = UserPreferences(),
) : UserPreferencesRepository {

    private val state = MutableStateFlow(initial)
    override val preferences = state.asStateFlow()

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        state.update { it.copy(hasCompletedOnboarding = completed) }
    }

    override suspend fun setThemeMode(mode: AppThemeMode) {
        state.update { it.copy(themeMode = mode) }
    }

    override suspend fun setContextualPersonalizationEnabled(enabled: Boolean) {
        state.update {
            it.copy(
                contextualPersonalizationEnabled = enabled,
                locationContextEnabled = if (enabled) it.locationContextEnabled else false,
            )
        }
    }

    override suspend fun setLocationContextEnabled(enabled: Boolean) {
        state.update {
            it.copy(
                locationContextEnabled = enabled,
                contextualPersonalizationEnabled = if (enabled) true else it.contextualPersonalizationEnabled,
            )
        }
    }
}
