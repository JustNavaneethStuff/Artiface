package com.artiface.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.artiface.app.R
import com.artiface.core.designsystem.component.ArtifaceGradientScaffold
import com.artiface.core.designsystem.component.ArtifaceWordmark
import kotlinx.coroutines.flow.collectLatest

@Composable
fun SplashRoute(
    onFinished: (hasCompletedOnboarding: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SplashViewModel = hiltViewModel(),
) {
    LaunchedEffect(viewModel) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                is SplashEffect.NavigateForward -> onFinished(effect.hasCompletedOnboarding)
            }
        }
    }

    ArtifaceGradientScaffold(
        modifier = modifier.testTag("splash_screen"),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            ArtifaceWordmark(subtitle = stringResource(R.string.splash_tagline))
        }
    }
}
