package com.artiface.feature.result.presentation

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.artiface.core.designsystem.component.ArtifacePrimaryButton
import com.artiface.core.designsystem.component.ArtifaceSecondaryButton
import com.artiface.core.designsystem.component.ArtifaceTextButton
import com.artiface.feature.result.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream

@Composable
fun ResultRoute(
    resultId: String,
    onTryAnotherStyle: (selfieId: String) -> Unit,
    onCreateAnother: () -> Unit,
    onOpenGallery: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ResultViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(viewModel) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                is ResultEffect.ShareImage -> shareImage(context, effect.imageUri, effect.title)
                is ResultEffect.SaveImage -> scope.launch {
                    val ok = saveImageToGallery(context, effect.imageUri, effect.title)
                    Toast.makeText(
                        context,
                        context.getString(
                            if (ok) R.string.result_save_success else R.string.result_save_failed,
                        ),
                        Toast.LENGTH_SHORT,
                    ).show()
                }
                is ResultEffect.NavigateTryAnotherStyle -> onTryAnotherStyle(effect.selfieId)
                ResultEffect.NavigateCreateAnother -> onCreateAnother()
                ResultEffect.NavigateGallery -> onOpenGallery()
                is ResultEffect.ShowMessage -> {
                    Toast.makeText(context, effect.messageRes, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    ResultScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        modifier = modifier,
    )
    @Suppress("UNUSED_VARIABLE")
    val routeResultId = resultId
}

@Composable
fun ResultScreen(
    uiState: ResultUiState,
    onEvent: (ResultEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(uiState.result?.id) {
        if (uiState.result != null) {
            reveal.snapTo(0f)
            reveal.animateTo(1f, animationSpec = tween(900))
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF120E0D))
            .testTag("result_screen"),
    ) {
        when {
            uiState.isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White,
                )
            }
            uiState.missing || uiState.result == null -> {
                Text(
                    text = stringResource(R.string.result_missing),
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    textAlign = TextAlign.Center,
                )
            }
            else -> {
                val result = uiState.result
                AsyncImage(
                    model = result.generatedImageUri.toUri(),
                    contentDescription = stringResource(R.string.result_image_cd),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = 0.35f + (0.65f * reveal.value)
                            scaleX = 1.08f - (0.08f * reveal.value)
                            scaleY = 1.08f - (0.08f * reveal.value)
                        }
                        .testTag("result_image"),
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Transparent,
                                    Color(0xCC120E0D),
                                    Color(0xF2120E0D),
                                ),
                            ),
                        ),
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Bottom,
                ) {
                    Text(
                        text = uiState.styleName,
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFFFFC857),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = result.title,
                        style = MaterialTheme.typography.displaySmall,
                        color = Color.White,
                        modifier = Modifier
                            .graphicsLayer { alpha = reveal.value }
                            .testTag("result_title"),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = uiState.contextSummary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFFFE8DC),
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ArtifacePrimaryButton(
                            text = stringResource(R.string.result_share),
                            onClick = { onEvent(ResultEvent.Share) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("result_share"),
                        )
                        ArtifaceSecondaryButton(
                            text = stringResource(R.string.result_save),
                            onClick = { onEvent(ResultEvent.Save) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("result_save"),
                        )
                        IconButton(
                            onClick = { onEvent(ResultEvent.ToggleFavourite) },
                            modifier = Modifier.testTag("result_favourite"),
                        ) {
                            Icon(
                                imageVector = if (result.isFavourite) {
                                    Icons.Filled.Favorite
                                } else {
                                    Icons.Filled.FavoriteBorder
                                },
                                contentDescription = stringResource(R.string.result_favourite),
                                tint = if (result.isFavourite) Color(0xFFFF5A3C) else Color.White,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    ArtifaceTextButton(
                        text = stringResource(R.string.result_try_another_style),
                        onClick = { onEvent(ResultEvent.TryAnotherStyle) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("result_try_another"),
                    )
                    ArtifaceTextButton(
                        text = stringResource(R.string.result_create_another),
                        onClick = { onEvent(ResultEvent.CreateAnother) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    ArtifaceTextButton(
                        text = stringResource(R.string.result_open_gallery),
                        onClick = { onEvent(ResultEvent.OpenGallery) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

private fun shareImage(context: Context, imageUri: String, title: String) {
    val uri = imageUri.toUri()
    val shareUri: Uri = when (uri.scheme) {
        "content" -> uri
        "file" -> {
            val file = File(uri.path ?: return)
            val cacheShare = File(context.cacheDir, "shared").also { it.mkdirs() }
            val shared = File(cacheShare, file.name)
            file.copyTo(shared, overwrite = true)
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", shared)
        }
        else -> return
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/jpeg"
        putExtra(Intent.EXTRA_STREAM, shareUri)
        putExtra(Intent.EXTRA_SUBJECT, title)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, title))
}

private suspend fun saveImageToGallery(
    context: Context,
    imageUri: String,
    title: String,
): Boolean = withContext(Dispatchers.IO) {
    runCatching {
        val sourceUri = imageUri.toUri()
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "${title.replace(' ', '_')}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/ARTIFACE")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val item = resolver.insert(collection, values) ?: return@runCatching false
        resolver.openOutputStream(item)?.use { output ->
            when (sourceUri.scheme) {
                "file" -> FileInputStream(File(sourceUri.path!!)).use { it.copyTo(output) }
                else -> resolver.openInputStream(sourceUri)?.use { it.copyTo(output) }
                    ?: return@runCatching false
            }
        } ?: return@runCatching false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(item, values, null, null)
        }
        true
    }.getOrDefault(false)
}
