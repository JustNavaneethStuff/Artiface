package com.artiface.feature.camera.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.artiface.core.designsystem.component.FeaturePlaceholderScreen
import com.artiface.feature.camera.R

@Composable
fun CameraRoute(
    onCaptured: (selfieId: String) -> Unit,
    onOpenGallery: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CameraScreen(modifier = modifier)
    @Suppress("UNUSED_VARIABLE")
    val phase3Callbacks = onCaptured to onOpenGallery
}

@Composable
fun CameraScreen(modifier: Modifier = Modifier) {
    FeaturePlaceholderScreen(
        title = stringResource(R.string.camera_title),
        description = stringResource(R.string.camera_placeholder_body),
        modifier = modifier,
    )
}
