package com.artiface.feature.settings.presentation

import app.cash.turbine.test
import com.artiface.core.model.AppThemeMode
import com.artiface.core.preferences.FakeUserPreferencesRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun theme_selection_updates_preferences() = runTest {
        val repo = FakeUserPreferencesRepository()
        val viewModel = SettingsViewModel(repo, appVersion = "0.2.0-phase2")
        advanceUntilIdle()

        viewModel.onEvent(SettingsEvent.ThemeSelected(AppThemeMode.Light))
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.preferences.themeMode).isEqualTo(AppThemeMode.Light)
        assertThat(repo.preferences.value.themeMode).isEqualTo(AppThemeMode.Light)
    }

    @Test
    fun clear_gallery_confirmation_emits_effect() = runTest {
        val viewModel = SettingsViewModel(
            FakeUserPreferencesRepository(),
            appVersion = "0.2.0-phase2",
        )
        advanceUntilIdle()

        viewModel.effects.test {
            viewModel.onEvent(SettingsEvent.ClearGalleryClicked)
            advanceUntilIdle()
            assertThat(viewModel.uiState.value.showClearGalleryDialog).isTrue()

            viewModel.onEvent(SettingsEvent.ClearGalleryConfirmed)
            advanceUntilIdle()
            assertThat(awaitItem()).isEqualTo(SettingsEffect.GalleryCleared)
            assertThat(viewModel.uiState.value.showClearGalleryDialog).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun location_toggle_requires_personalization_path_via_repository() = runTest {
        val repo = FakeUserPreferencesRepository()
        val viewModel = SettingsViewModel(repo, appVersion = "0.2.0-phase2")
        advanceUntilIdle()

        viewModel.onEvent(SettingsEvent.LocationContextToggled(true))
        advanceUntilIdle()

        assertThat(repo.preferences.value.locationContextEnabled).isTrue()
        assertThat(repo.preferences.value.contextualPersonalizationEnabled).isTrue()
    }
}
