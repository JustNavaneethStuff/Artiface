package com.artiface.core.preferences

import app.cash.turbine.test
import com.artiface.core.model.AppThemeMode
import com.artiface.core.model.UserPreferences
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class FakeUserPreferencesRepositoryTest {

    @Test
    fun defaults_match_expected_opt_in_privacy_posture() = runTest {
        val repo = FakeUserPreferencesRepository()
        repo.preferences.test {
            assertThat(awaitItem()).isEqualTo(UserPreferences())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun completing_onboarding_persists_flag() = runTest {
        val repo = FakeUserPreferencesRepository()
        repo.setOnboardingCompleted(true)
        repo.preferences.test {
            assertThat(awaitItem().hasCompletedOnboarding).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun disabling_personalization_also_disables_location() = runTest {
        val repo = FakeUserPreferencesRepository(
            UserPreferences(
                contextualPersonalizationEnabled = true,
                locationContextEnabled = true,
            ),
        )
        repo.setContextualPersonalizationEnabled(false)
        repo.preferences.test {
            val prefs = awaitItem()
            assertThat(prefs.contextualPersonalizationEnabled).isFalse()
            assertThat(prefs.locationContextEnabled).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun enabling_location_enables_personalization() = runTest {
        val repo = FakeUserPreferencesRepository()
        repo.setLocationContextEnabled(true)
        repo.preferences.test {
            val prefs = awaitItem()
            assertThat(prefs.locationContextEnabled).isTrue()
            assertThat(prefs.contextualPersonalizationEnabled).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun theme_mode_updates() = runTest {
        val repo = FakeUserPreferencesRepository()
        repo.setThemeMode(AppThemeMode.Dark)
        repo.preferences.test {
            assertThat(awaitItem().themeMode).isEqualTo(AppThemeMode.Dark)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
