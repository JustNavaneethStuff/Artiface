package com.artiface.feature.onboarding.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.artiface.core.designsystem.component.FeaturePlaceholderScreen
import com.artiface.feature.onboarding.R

@Composable
fun OnboardingRoute(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Full pager + DataStore persistence arrives in Phase 2.
    OnboardingScreen(
        onGetStarted = onFinished,
        modifier = modifier,
    )
}

@Composable
fun OnboardingScreen(
    onGetStarted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FeaturePlaceholderScreen(
        title = stringResource(R.string.onboarding_title),
        description = stringResource(R.string.onboarding_placeholder_body),
        modifier = modifier,
    )
    // Keep callback referenced so navigation wiring compiles and stays intentional.
    @Suppress("UNUSED_VARIABLE")
    val readyForPhase2 = onGetStarted
}
