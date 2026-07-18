package com.artiface.feature.processing.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.artiface.core.designsystem.component.ArtifacePrimaryButton
import com.artiface.core.designsystem.component.ArtifaceSecondaryButton
import com.artiface.core.designsystem.component.ArtifaceWidePrimaryButton
import com.artiface.core.designsystem.theme.ArtifaceThemeExtras
import com.artiface.core.model.CaricatureStyle
import com.artiface.core.model.GenerationStatus
import com.artiface.core.model.StyleId
import com.artiface.feature.processing.R
import kotlinx.coroutines.flow.collectLatest

@Composable
fun StyleSelectionRoute(
    selfieId: String,
    onJobStarted: (jobId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StyleSelectionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                is StyleSelectionEffect.NavigateToProcessing -> onJobStarted(effect.jobId)
            }
        }
    }

    StyleSelectionScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        modifier = modifier,
    )
    @Suppress("UNUSED_VARIABLE")
    val routeSelfieId = selfieId
}

@Composable
fun StyleSelectionScreen(
    uiState: StyleSelectionUiState,
    onEvent: (StyleSelectionEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ArtifaceThemeExtras.gradients.hero)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("style_screen"),
    ) {
        Text(
            text = stringResource(R.string.style_title),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
        )
        Text(
            text = stringResource(R.string.style_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Spacer(modifier = Modifier.height(12.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f),
        ) {
            items(uiState.styles, key = { it.id.value }) { style ->
                StyleCard(
                    style = style,
                    selected = uiState.selectedStyleId == style.id,
                    onClick = { onEvent(StyleSelectionEvent.StyleClicked(style.id)) },
                )
            }
        }
        uiState.errorMessage?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
        }
        ArtifaceWidePrimaryButton(
            text = stringResource(R.string.style_continue),
            onClick = { onEvent(StyleSelectionEvent.Continue) },
            enabled = uiState.selectedStyleId != null && !uiState.isStarting,
            modifier = Modifier
                .padding(24.dp)
                .testTag("style_continue"),
        )
    }
}

@Composable
private fun StyleCard(
    style: CaricatureStyle,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val border = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
            .border(2.dp, border, RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
            .semantics { this.selected = selected }
            .testTag("style_card_${style.id.value}"),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.1f)
                .clip(RoundedCornerShape(18.dp))
                .background(styleBrush(style.id)),
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = style.name,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = style.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun styleBrush(styleId: StyleId): Brush = when (styleId.value) {
    "comic_burst" -> Brush.linearGradient(listOf(Color(0xFFFF5A3C), Color(0xFFFFC857)))
    "royal_absurdity" -> Brush.linearGradient(listOf(Color(0xFF5C2A4D), Color(0xFFFFC857)))
    "neon_mischief" -> Brush.linearGradient(listOf(Color(0xFF1FA6A0), Color(0xFFFF5A3C)))
    "storybook_chaos" -> Brush.linearGradient(listOf(Color(0xFFFFC857), Color(0xFFFFE8DC)))
    "retro_poster" -> Brush.linearGradient(listOf(Color(0xFFC45C2A), Color(0xFF1A1210)))
    else -> Brush.linearGradient(listOf(Color(0xFFFF5A3C), Color(0xFF1FA6A0), Color(0xFFFFC857)))
}

@Composable
fun ProcessingRoute(
    jobId: String,
    onCompleted: (resultId: String) -> Unit,
    onFailed: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProcessingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                is ProcessingEffect.NavigateToResult -> onCompleted(effect.resultId)
                ProcessingEffect.NavigateBack -> onFailed()
            }
        }
    }

    ProcessingScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        modifier = modifier,
    )
    @Suppress("UNUSED_VARIABLE")
    val routeJobId = jobId
}

@Composable
fun ProcessingScreen(
    uiState: ProcessingUiState,
    onEvent: (ProcessingEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "processingSpin")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "orbRotation",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ArtifaceThemeExtras.gradients.processing)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("processing_screen"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(148.dp)
                    .rotate(rotation)
                    .clip(CircleShape)
                    .background(
                        Brush.sweepGradient(
                            listOf(
                                Color(0xFFFF5A3C),
                                Color(0xFFFFC857),
                                Color(0xFF1FA6A0),
                                Color(0xFF5C2A4D),
                                Color(0xFFFF5A3C),
                            ),
                        ),
                    )
                    .padding(18.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1A1210)),
            )
            Spacer(modifier = Modifier.height(36.dp))
            Text(
                text = stringResource(R.string.processing_title),
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(16.dp))
            AnimatedContent(
                targetState = uiState.humorousMessage,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "processingMessage",
            ) { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFFFFE8DC),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("processing_message"),
                )
            }
            Spacer(modifier = Modifier.height(28.dp))
            LinearProgressIndicator(
                progress = { uiState.job?.progress ?: 0f },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("processing_progress"),
                color = Color(0xFFFFC857),
                trackColor = Color.White.copy(alpha = 0.2f),
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = statusLabel(uiState.job?.status),
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.8f),
            )

            if (uiState.isFailed) {
                Spacer(modifier = Modifier.height(28.dp))
                Text(
                    text = uiState.errorMessage ?: stringResource(R.string.processing_failed_body),
                    color = Color(0xFFFFB4AB),
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(16.dp))
                ArtifacePrimaryButton(
                    text = stringResource(R.string.processing_retry),
                    onClick = { onEvent(ProcessingEvent.Retry) },
                    modifier = Modifier.testTag("processing_retry"),
                )
                Spacer(modifier = Modifier.height(8.dp))
                ArtifaceSecondaryButton(
                    text = stringResource(R.string.processing_back),
                    onClick = { onEvent(ProcessingEvent.GoBack) },
                )
            }
        }
    }
}

@Composable
private fun statusLabel(status: GenerationStatus?): String = when (status) {
    GenerationStatus.PreparingImage -> stringResource(R.string.processing_status_preparing)
    GenerationStatus.Uploading -> stringResource(R.string.processing_status_uploading)
    GenerationStatus.WaitingForProcessing -> stringResource(R.string.processing_status_waiting)
    GenerationStatus.DownloadingResult -> stringResource(R.string.processing_status_downloading)
    GenerationStatus.Completed -> stringResource(R.string.processing_status_completed)
    GenerationStatus.Failed -> stringResource(R.string.processing_status_failed)
    null -> stringResource(R.string.processing_status_preparing)
}
