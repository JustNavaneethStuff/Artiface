package com.artiface.app.ui

import app.cash.turbine.test
import com.artiface.core.model.UserPreferences
import com.artiface.core.preferences.FakeUserPreferencesRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SplashViewModelTest {

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
    fun routes_to_onboarding_when_not_completed() = runTest {
        val viewModel = SplashViewModel(FakeUserPreferencesRepository())
        viewModel.effects.test {
            advanceTimeBy(900)
            advanceUntilIdle()
            assertThat(awaitItem()).isEqualTo(SplashEffect.NavigateForward(false))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun routes_to_camera_when_onboarding_completed() = runTest {
        val repo = FakeUserPreferencesRepository(
            UserPreferences(hasCompletedOnboarding = true),
        )
        val viewModel = SplashViewModel(repo)
        viewModel.effects.test {
            advanceTimeBy(900)
            advanceUntilIdle()
            assertThat(awaitItem()).isEqualTo(SplashEffect.NavigateForward(true))
            cancelAndIgnoreRemainingEvents()
        }
    }
}
