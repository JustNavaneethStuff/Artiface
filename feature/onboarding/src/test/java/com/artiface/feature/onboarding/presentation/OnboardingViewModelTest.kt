package com.artiface.feature.onboarding.presentation

import app.cash.turbine.test
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
class OnboardingViewModelTest {

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
    fun next_advances_page() = runTest {
        val viewModel = OnboardingViewModel(FakeUserPreferencesRepository())
        viewModel.onEvent(OnboardingEvent.Next)
        assertThat(viewModel.uiState.value.pageIndex).isEqualTo(1)
    }

    @Test
    fun get_started_persists_and_emits_navigation() = runTest {
        val repo = FakeUserPreferencesRepository()
        val viewModel = OnboardingViewModel(repo)

        viewModel.effects.test {
            viewModel.onEvent(OnboardingEvent.GetStarted)
            advanceUntilIdle()
            assertThat(awaitItem()).isEqualTo(OnboardingEffect.NavigateToCamera)
            assertThat(repo.preferences.value.hasCompletedOnboarding).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun skip_from_first_page_completes_onboarding() = runTest {
        val repo = FakeUserPreferencesRepository()
        val viewModel = OnboardingViewModel(repo)

        viewModel.effects.test {
            viewModel.onEvent(OnboardingEvent.Skip)
            advanceUntilIdle()
            assertThat(awaitItem()).isEqualTo(OnboardingEffect.NavigateToCamera)
            assertThat(repo.preferences.value.hasCompletedOnboarding).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }
}
