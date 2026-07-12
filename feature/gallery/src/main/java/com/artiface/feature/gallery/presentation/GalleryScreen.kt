package com.artiface.feature.gallery.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.artiface.core.designsystem.component.FeaturePlaceholderScreen
import com.artiface.feature.gallery.R

@Composable
fun GalleryRoute(
    onOpenResult: (resultId: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GalleryScreen(modifier = modifier)
    @Suppress("UNUSED_VARIABLE")
    val phase5Args = onOpenResult to onBack
}

@Composable
fun GalleryScreen(modifier: Modifier = Modifier) {
    FeaturePlaceholderScreen(
        title = stringResource(R.string.gallery_title),
        description = stringResource(R.string.gallery_placeholder_body),
        modifier = modifier,
    )
}
