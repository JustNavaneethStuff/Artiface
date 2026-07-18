package com.artiface.feature.gallery.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.artiface.feature.gallery.R
import kotlinx.coroutines.flow.collectLatest

@Composable
fun GalleryRoute(
    onOpenResult: (resultId: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GalleryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                GalleryEffect.NavigateBack -> onBack()
                is GalleryEffect.NavigateToResult -> onOpenResult(effect.resultId)
            }
        }
    }

    GalleryScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    uiState: GalleryUiState,
    onEvent: (GalleryEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize().testTag("gallery_screen"),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.gallery_title)) },
                navigationIcon = {
                    IconButton(
                        onClick = { onEvent(GalleryEvent.Back) },
                        modifier = Modifier.testTag("gallery_back"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.gallery_back),
                        )
                    }
                },
                actions = {
                    FilterChip(
                        selected = uiState.favouritesOnly,
                        onClick = { onEvent(GalleryEvent.ToggleFavouritesFilter) },
                        label = { Text(stringResource(R.string.gallery_filter_favourites)) },
                        leadingIcon = {
                            Icon(
                                imageVector = if (uiState.favouritesOnly) {
                                    Icons.Filled.Favorite
                                } else {
                                    Icons.Filled.FavoriteBorder
                                },
                                contentDescription = null,
                            )
                        },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("gallery_favourites_filter"),
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                ),
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                            MaterialTheme.colorScheme.surface,
                        ),
                    ),
                )
                .navigationBarsPadding(),
        ) {
            if (uiState.isEmpty) {
                GalleryEmptyState(
                    favouritesOnly = uiState.favouritesOnly,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 156.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("gallery_grid"),
                ) {
                    items(uiState.items, key = { it.id }) { item ->
                        GalleryGridItem(
                            item = item,
                            onOpen = { onEvent(GalleryEvent.OpenResult(item.id)) },
                            onDelete = { onEvent(GalleryEvent.DeleteClicked(item.id)) },
                        )
                    }
                }
            }
        }
    }

    if (uiState.pendingDeleteId != null) {
        AlertDialog(
            onDismissRequest = { onEvent(GalleryEvent.DeleteDismissed) },
            title = { Text(stringResource(R.string.gallery_delete_title)) },
            text = { Text(stringResource(R.string.gallery_delete_body)) },
            confirmButton = {
                TextButton(
                    onClick = { onEvent(GalleryEvent.DeleteConfirmed) },
                    modifier = Modifier.testTag("gallery_delete_confirm"),
                ) {
                    Text(stringResource(R.string.gallery_delete_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(GalleryEvent.DeleteDismissed) }) {
                    Text(stringResource(R.string.gallery_delete_cancel))
                }
            },
        )
    }
}

@Composable
private fun GalleryEmptyState(
    favouritesOnly: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.testTag("gallery_empty"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(
                if (favouritesOnly) R.string.gallery_empty_favourites_title else R.string.gallery_empty_title,
            ),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(
                if (favouritesOnly) R.string.gallery_empty_favourites_body else R.string.gallery_empty_body,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun GalleryGridItem(
    item: GalleryRowUi,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .clickable(onClick = onOpen)
            .testTag("gallery_item_${item.id}"),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f),
        ) {
            AsyncImage(
                model = item.thumbnailUri,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            if (item.isFavourite) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = stringResource(R.string.gallery_favourite_badge),
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                )
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .testTag("gallery_delete_${item.id}"),
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.gallery_delete_content_description),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = item.styleName,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.gallery_meta_format, item.createdAtLabel, item.statusLabel),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
