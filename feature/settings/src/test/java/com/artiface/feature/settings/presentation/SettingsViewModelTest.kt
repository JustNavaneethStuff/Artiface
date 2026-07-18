package com.artiface.feature.settings.presentation

import app.cash.turbine.test
import com.artiface.core.common.generation.GenerationRepository
import com.artiface.core.model.AppThemeMode
import com.artiface.core.model.CaricatureResult
import com.artiface.core.model.GalleryItem
import com.artiface.core.model.GenerationJob
import com.artiface.core.model.StyleId
import com.artiface.core.preferences.FakeUserPreferencesRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
        val prefs = FakeUserPreferencesRepository()
        val generation = FakeSettingsGenerationRepository()
        val viewModel = SettingsViewModel(prefs, generation, appVersion = "0.5.0-phase5")
        advanceUntilIdle()

        viewModel.onEvent(SettingsEvent.ThemeSelected(AppThemeMode.Light))
        advanceUntilIdle()

        assertThat(viewModel.uiState.value.preferences.themeMode).isEqualTo(AppThemeMode.Light)
        assertThat(prefs.preferences.value.themeMode).isEqualTo(AppThemeMode.Light)
    }

    @Test
    fun clear_gallery_confirmation_clears_repository_and_emits_effect() = runTest {
        val generation = FakeSettingsGenerationRepository()
        val viewModel = SettingsViewModel(
            FakeUserPreferencesRepository(),
            generation,
            appVersion = "0.5.0-phase5",
        )
        advanceUntilIdle()

        viewModel.effects.test {
            viewModel.onEvent(SettingsEvent.ClearGalleryClicked)
            advanceUntilIdle()
            assertThat(viewModel.uiState.value.showClearGalleryDialog).isTrue()

            viewModel.onEvent(SettingsEvent.ClearGalleryConfirmed)
            advanceUntilIdle()
            assertThat(generation.clearCalls).isEqualTo(1)
            assertThat(awaitItem()).isEqualTo(SettingsEffect.GalleryCleared)
            assertThat(viewModel.uiState.value.showClearGalleryDialog).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun location_toggle_requires_personalization_path_via_repository() = runTest {
        val prefs = FakeUserPreferencesRepository()
        val viewModel = SettingsViewModel(
            prefs,
            FakeSettingsGenerationRepository(),
            appVersion = "0.5.0-phase5",
        )
        advanceUntilIdle()

        viewModel.onEvent(SettingsEvent.LocationContextToggled(true))
        advanceUntilIdle()

        assertThat(prefs.preferences.value.locationContextEnabled).isTrue()
        assertThat(prefs.preferences.value.contextualPersonalizationEnabled).isTrue()
    }
}

private class FakeSettingsGenerationRepository : GenerationRepository {
    var clearCalls: Int = 0

    override suspend fun clearGallery() {
        clearCalls += 1
    }

    override suspend fun startGeneration(selfieId: String, styleId: StyleId): GenerationJob =
        error("unused")
    override fun observeJob(jobId: String): Flow<GenerationJob?> = MutableStateFlow(null)
    override suspend fun getJob(jobId: String): GenerationJob? = null
    override suspend fun getResult(resultId: String): CaricatureResult? = null
    override fun observeResult(resultId: String): Flow<CaricatureResult?> = MutableStateFlow(null)
    override suspend fun getResultIdForJob(jobId: String): String? = null
    override fun getSelfieIdForResult(resultId: String): String? = null
    override suspend fun setFavourite(resultId: String, favourite: Boolean) = Unit
    override suspend fun retry(jobId: String): GenerationJob = error("unused")
    override fun observeGalleryItems(): Flow<List<GalleryItem>> = MutableStateFlow(emptyList())
    override suspend fun deleteResult(resultId: String) = Unit
}
