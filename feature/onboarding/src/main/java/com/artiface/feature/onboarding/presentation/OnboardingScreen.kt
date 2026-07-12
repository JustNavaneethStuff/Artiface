package com.artiface.feature.onboarding.presentation

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.artiface.core.designsystem.component.ArtifacePageIndicator
import com.artiface.core.designsystem.component.ArtifacePrimaryButton
import com.artiface.core.designsystem.component.ArtifaceTextButton
import com.artiface.core.designsystem.component.ArtifaceWidePrimaryButton
import com.artiface.core.designsystem.theme.ArtifaceThemeExtras
import com.artiface.feature.onboarding.R
import kotlinx.coroutines.flow.collectLatest

private data class OnboardingPageContent(
    @StringRes val titleRes: Int,
    @StringRes val bodyRes: Int,
)

private val pages = listOf(
    OnboardingPageContent(R.string.onboarding_page1_title, R.string.onboarding_page1_body),
    OnboardingPageContent(R.string.onboarding_page2_title, R.string.onboarding_page2_body),
    OnboardingPageContent(R.string.onboarding_page3_title, R.string.onboarding_page3_body),
)

@Composable
fun OnboardingRoute(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                OnboardingEffect.NavigateToCamera -> onFinished()
            }
        }
    }

    OnboardingScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        modifier = modifier,
    )
}

@Composable
fun OnboardingScreen(
    uiState: OnboardingUiState,
    onEvent: (OnboardingEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(
        initialPage = uiState.pageIndex,
        pageCount = { uiState.pageCount },
    )

    LaunchedEffect(uiState.pageIndex) {
        if (pagerState.currentPage != uiState.pageIndex) {
            pagerState.animateScrollToPage(uiState.pageIndex)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != uiState.pageIndex) {
            onEvent(OnboardingEvent.PageSelected(pagerState.currentPage))
        }
    }

    val isLastPage = uiState.pageIndex >= uiState.pageCount - 1

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ArtifaceThemeExtras.gradients.hero)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("onboarding_screen"),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                ArtifaceTextButton(
                    text = stringResource(R.string.onboarding_skip),
                    onClick = { onEvent(OnboardingEvent.Skip) },
                    enabled = !uiState.isSaving,
                    modifier = Modifier.testTag("onboarding_skip"),
                )
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) { page ->
                OnboardingPage(page = pages[page])
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ArtifacePageIndicator(
                    pageCount = uiState.pageCount,
                    currentPage = uiState.pageIndex,
                    contentDescription = stringResource(
                        R.string.onboarding_page_indicator,
                        uiState.pageIndex + 1,
                        uiState.pageCount,
                    ),
                    modifier = Modifier.testTag("onboarding_indicators"),
                )
                Spacer(modifier = Modifier.height(24.dp))
                AnimatedContent(
                    targetState = isLastPage,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "onboardingCta",
                ) { lastPage ->
                    if (lastPage) {
                        ArtifaceWidePrimaryButton(
                            text = stringResource(R.string.onboarding_get_started),
                            onClick = { onEvent(OnboardingEvent.GetStarted) },
                            enabled = !uiState.isSaving,
                            modifier = Modifier.testTag("onboarding_get_started"),
                        )
                    } else {
                        ArtifacePrimaryButton(
                            text = stringResource(R.string.onboarding_next),
                            onClick = { onEvent(OnboardingEvent.Next) },
                            enabled = !uiState.isSaving,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("onboarding_next"),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingPage(
    page: OnboardingPageContent,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(148.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
            )
        }
        Spacer(modifier = Modifier.height(36.dp))
        Text(
            text = stringResource(page.titleRes),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = stringResource(page.bodyRes),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
