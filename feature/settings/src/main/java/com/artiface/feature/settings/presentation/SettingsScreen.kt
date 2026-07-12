package com.artiface.feature.settings.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.artiface.core.designsystem.component.FeaturePlaceholderScreen
import com.artiface.feature.settings.R

@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsScreen(modifier = modifier)
    @Suppress("UNUSED_VARIABLE")
    val phase2Callback = onBack
}

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    FeaturePlaceholderScreen(
        title = stringResource(R.string.settings_title),
        description = stringResource(R.string.settings_placeholder_body),
        modifier = modifier,
    )
}
