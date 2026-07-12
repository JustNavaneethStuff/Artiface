package com.artiface.feature.camera.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.artiface.core.designsystem.component.FeaturePlaceholderScreen
import com.artiface.feature.camera.R

@Composable
fun CameraRoute(
    onCaptured: (selfieId: String) -> Unit,
    onOpenGallery: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CameraScreen(
        onOpenSettings = onOpenSettings,
        modifier = modifier,
    )
    @Suppress("UNUSED_VARIABLE")
    val phase3Callbacks = onCaptured to onOpenGallery
}

@Composable
fun CameraScreen(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        FeaturePlaceholderScreen(
            title = stringResource(R.string.camera_title),
            description = stringResource(R.string.camera_placeholder_body),
        )
        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(8.dp)
                .testTag("camera_settings"),
        ) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = stringResource(R.string.camera_open_settings),
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}
