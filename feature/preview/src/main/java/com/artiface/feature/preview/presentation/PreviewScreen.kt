package com.artiface.feature.preview.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.artiface.core.designsystem.component.FeaturePlaceholderScreen
import com.artiface.feature.preview.R

@Composable
fun PreviewRoute(
    selfieId: String,
    onRetake: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PreviewScreen(modifier = modifier)
    @Suppress("UNUSED_VARIABLE")
    val phase3Args = Triple(selfieId, onRetake, onContinue)
}

@Composable
fun PreviewScreen(modifier: Modifier = Modifier) {
    FeaturePlaceholderScreen(
        title = stringResource(R.string.preview_title),
        description = stringResource(R.string.preview_placeholder_body),
        modifier = modifier,
    )
}
