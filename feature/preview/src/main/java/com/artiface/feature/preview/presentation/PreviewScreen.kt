package com.artiface.feature.preview.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.artiface.core.designsystem.component.ArtifacePrimaryButton
import com.artiface.core.designsystem.component.ArtifaceSecondaryButton
import com.artiface.feature.preview.R
import kotlinx.coroutines.flow.collectLatest

@Composable
fun PreviewRoute(
    selfieId: String,
    onRetake: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PreviewViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                PreviewEffect.NavigateRetake -> onRetake()
                is PreviewEffect.NavigateContinue -> onContinue()
            }
        }
    }

    PreviewScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        modifier = modifier,
    )
    // selfieId is supplied via navigation SavedStateHandle; keep parameter for call-site clarity.
    @Suppress("UNUSED_VARIABLE")
    val routeSelfieId = selfieId
}

@Composable
fun PreviewScreen(
    uiState: PreviewUiState,
    onEvent: (PreviewEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("preview_screen"),
    ) {
        Text(
            text = stringResource(R.string.preview_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            when {
                uiState.isLoading -> CircularProgressIndicator(modifier = Modifier.testTag("preview_loading"))
                uiState.missing || uiState.selfie == null -> {
                    Text(
                        text = stringResource(R.string.preview_missing),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(24.dp)
                            .testTag("preview_missing"),
                    )
                }
                else -> {
                    RepositionableSelfie(
                        imageUri = uiState.selfie.localUri,
                        contentDescription = stringResource(R.string.preview_image_cd),
                    )
                }
            }
        }

        Text(
            text = stringResource(R.string.preview_reposition_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ArtifaceSecondaryButton(
                text = stringResource(R.string.preview_retake),
                onClick = { onEvent(PreviewEvent.Retake) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("preview_retake"),
            )
            ArtifacePrimaryButton(
                text = stringResource(R.string.preview_continue),
                onClick = { onEvent(PreviewEvent.Continue) },
                enabled = uiState.selfie != null && !uiState.isLoading,
                modifier = Modifier
                    .weight(1f)
                    .testTag("preview_continue"),
            )
        }
    }
}

@Composable
private fun RepositionableSelfie(
    imageUri: String,
    contentDescription: String,
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 3f)
                    offset = if (scale == 1f) Offset.Zero else offset + pan
                }
            }
            .testTag("preview_image"),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = imageUri.toUri(),
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                },
        )
    }
}
