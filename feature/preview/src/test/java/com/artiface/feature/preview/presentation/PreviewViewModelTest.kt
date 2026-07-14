package com.artiface.feature.preview.presentation

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.artiface.core.common.selfie.FakeSelfieRepository
import com.artiface.core.model.CapturedSelfie
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
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class PreviewViewModelTest {

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
    fun loads_selfie_by_id() = runTest {
        val selfie = CapturedSelfie(
            id = "s1",
            localUri = "file:///s1.jpg",
            capturedAt = Instant.parse("2026-07-14T10:00:00Z"),
            width = 10,
            height = 20,
            orientation = 1,
        )
        val repo = FakeSelfieRepository(mapOf("s1" to selfie))
        val viewModel = PreviewViewModel(SavedStateHandle(mapOf("selfieId" to "s1")), repo)
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.selfie).isEqualTo(selfie)
        assertThat(viewModel.uiState.value.missing).isFalse()
    }

    @Test
    fun missing_selfie_sets_missing_flag() = runTest {
        val viewModel = PreviewViewModel(
            SavedStateHandle(mapOf("selfieId" to "missing")),
            FakeSelfieRepository(),
        )
        advanceUntilIdle()
        assertThat(viewModel.uiState.value.missing).isTrue()
    }

    @Test
    fun continue_emits_navigation_when_selfie_present() = runTest {
        val selfie = CapturedSelfie(
            id = "s1",
            localUri = "file:///s1.jpg",
            capturedAt = Instant.parse("2026-07-14T10:00:00Z"),
            width = 10,
            height = 20,
            orientation = 1,
        )
        val viewModel = PreviewViewModel(
            SavedStateHandle(mapOf("selfieId" to "s1")),
            FakeSelfieRepository(mapOf("s1" to selfie)),
        )
        advanceUntilIdle()

        viewModel.effects.test {
            viewModel.onEvent(PreviewEvent.Continue)
            advanceUntilIdle()
            assertThat(awaitItem()).isEqualTo(PreviewEffect.NavigateContinue("s1"))
            cancelAndIgnoreRemainingEvents()
        }
    }
}
