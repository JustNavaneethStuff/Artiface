package com.artiface.feature.result.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.artiface.core.designsystem.component.FeaturePlaceholderScreen
import com.artiface.feature.result.R

@Composable
fun ResultRoute(
    resultId: String,
    onTryAnotherStyle: () -> Unit,
    onCreateAnother: () -> Unit,
    onOpenGallery: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ResultScreen(modifier = modifier)
    @Suppress("UNUSED_VARIABLE")
    val phase4Args = listOf(resultId, onTryAnotherStyle, onCreateAnother, onOpenGallery)
}

@Composable
fun ResultScreen(modifier: Modifier = Modifier) {
    FeaturePlaceholderScreen(
        title = stringResource(R.string.result_title),
        description = stringResource(R.string.result_placeholder_body),
        modifier = modifier,
    )
}
