package com.artiface.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.artiface.app.R
import com.artiface.core.designsystem.component.ArtifaceGradientScaffold
import com.artiface.core.designsystem.component.ArtifaceWordmark
import kotlinx.coroutines.delay

/**
 * Compose splash shown after the system splash.
 * Onboarding completion persistence arrives in Phase 2; Phase 1 always routes to onboarding.
 */
@Composable
fun SplashRoute(
    onFinished: (hasCompletedOnboarding: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(Unit) {
        delay(900)
        onFinished(false)
    }

    ArtifaceGradientScaffold(modifier = modifier) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            ArtifaceWordmark(subtitle = stringResource(R.string.splash_tagline))
        }
    }
}
