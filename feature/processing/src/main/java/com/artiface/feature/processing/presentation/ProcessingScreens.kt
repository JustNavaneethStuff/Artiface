package com.artiface.feature.processing.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.artiface.core.designsystem.component.FeaturePlaceholderScreen
import com.artiface.feature.processing.R

@Composable
fun StyleSelectionRoute(
    selfieId: String,
    onStyleSelected: (styleId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    StyleSelectionScreen(modifier = modifier)
    @Suppress("UNUSED_VARIABLE")
    val phase4Args = selfieId to onStyleSelected
}

@Composable
fun StyleSelectionScreen(modifier: Modifier = Modifier) {
    FeaturePlaceholderScreen(
        title = stringResource(R.string.style_title),
        description = stringResource(R.string.style_placeholder_body),
        modifier = modifier,
    )
}

@Composable
fun ProcessingRoute(
    jobId: String,
    onCompleted: (resultId: String) -> Unit,
    onFailed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ProcessingScreen(modifier = modifier)
    @Suppress("UNUSED_VARIABLE")
    val phase4Args = Triple(jobId, onCompleted, onFailed)
}

@Composable
fun ProcessingScreen(modifier: Modifier = Modifier) {
    FeaturePlaceholderScreen(
        title = stringResource(R.string.processing_title),
        description = stringResource(R.string.processing_placeholder_body),
        modifier = modifier,
    )
}
