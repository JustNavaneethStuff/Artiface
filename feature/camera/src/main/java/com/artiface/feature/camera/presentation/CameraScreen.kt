package com.artiface.feature.camera.presentation

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.artiface.core.designsystem.component.ArtifacePrimaryButton
import com.artiface.core.designsystem.component.ArtifaceSecondaryButton
import com.artiface.core.designsystem.component.ArtifaceWidePrimaryButton
import com.artiface.feature.camera.R
import kotlinx.coroutines.flow.collectLatest
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Composable
fun CameraRoute(
    onCaptured: (selfieId: String) -> Unit,
    onOpenGallery: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CameraViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    val captureExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose { captureExecutor.shutdown() }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            viewModel.onEvent(CameraEvent.PermissionResultGranted)
        } else {
            val activity = context.findActivity()
            val showRationale = activity?.let {
                ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.CAMERA)
            } ?: false
            val permanently = uiState.hasRequestedPermission && !showRationale
            viewModel.onEvent(CameraEvent.PermissionResultDenied(permanently = permanently))
        }
    }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) {
            viewModel.onEvent(CameraEvent.PermissionResultGranted)
        } else {
            viewModel.onEvent(CameraEvent.PermissionResultDenied(permanently = false))
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                CameraEffect.RequestCameraPermission ->
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                CameraEffect.OpenAppSettings -> {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null),
                        ),
                    )
                }
                CameraEffect.OpenGallery -> onOpenGallery()
                CameraEffect.OpenSettings -> onOpenSettings()
                is CameraEffect.NavigateToPreview -> onCaptured(effect.selfieId)
                is CameraEffect.PerformCapture -> {
                    val capture = imageCapture
                    if (capture == null) {
                        viewModel.onEvent(
                            CameraEvent.CaptureFailed(
                                context.getString(R.string.camera_error_not_ready),
                            ),
                        )
                        return@collectLatest
                    }
                    capture.flashMode = effect.flashMode
                    val tempFile = viewModel.createTempCaptureFile()
                    val outputOptions = ImageCapture.OutputFileOptions.Builder(tempFile)
                        .setMetadata(
                            ImageCapture.Metadata().apply {
                                isReversedHorizontal =
                                    effect.lensFacing == CameraSelector.LENS_FACING_FRONT
                            },
                        )
                        .build()
                    capture.takePicture(
                        outputOptions,
                        captureExecutor,
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                viewModel.onImageSaved(tempFile)
                            }

                            override fun onError(exception: ImageCaptureException) {
                                tempFile.delete()
                                viewModel.onEvent(
                                    CameraEvent.CaptureFailed(
                                        exception.message
                                            ?: context.getString(R.string.camera_error_capture_failed),
                                    ),
                                )
                            }
                        },
                    )
                }
            }
        }
    }

    CameraScreen(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onImageCaptureReady = { imageCapture = it },
        onCameraUnavailable = viewModel::onCameraHardwareUnavailable,
        modifier = modifier,
    )
}

@Composable
fun CameraScreen(
    uiState: CameraUiState,
    onEvent: (CameraEvent) -> Unit,
    onImageCaptureReady: (ImageCapture?) -> Unit,
    onCameraUnavailable: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("camera_screen"),
    ) {
        when {
            !uiState.cameraAvailable -> CameraMessagePane(
                title = stringResource(R.string.camera_unavailable_title),
                body = stringResource(R.string.camera_unavailable_body),
                primaryLabel = stringResource(R.string.camera_open_settings),
                onPrimary = { onEvent(CameraEvent.OpenSettings) },
            )
            uiState.permissionStatus == CameraPermissionStatus.Granted -> {
                CameraPreviewHost(
                    lensFacing = uiState.lensFacing,
                    onImageCaptureReady = onImageCaptureReady,
                    onFlashSupported = { supported ->
                        onEvent(CameraEvent.FlashAvailabilityChanged(supported))
                    },
                    onCameraUnavailable = onCameraUnavailable,
                    modifier = Modifier.fillMaxSize(),
                )
                CameraControls(
                    uiState = uiState,
                    onEvent = onEvent,
                )
                if (uiState.isCapturing) {
                    CaptureProgressOverlay()
                }
                uiState.errorMessage?.let { message ->
                    ErrorBanner(
                        message = message,
                        onDismiss = { onEvent(CameraEvent.DismissError) },
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .statusBarsPadding()
                            .padding(16.dp),
                    )
                }
            }
            uiState.permissionStatus == CameraPermissionStatus.PermanentlyDenied -> {
                CameraMessagePane(
                    title = stringResource(R.string.camera_permission_permanent_title),
                    body = stringResource(R.string.camera_permission_permanent_body),
                    primaryLabel = stringResource(R.string.camera_open_system_settings),
                    onPrimary = { onEvent(CameraEvent.OpenAppSettings) },
                    secondaryLabel = stringResource(R.string.camera_open_settings),
                    onSecondary = { onEvent(CameraEvent.OpenSettings) },
                )
            }
            else -> {
                CameraMessagePane(
                    title = stringResource(R.string.camera_permission_title),
                    body = stringResource(R.string.camera_permission_body),
                    primaryLabel = stringResource(R.string.camera_permission_allow),
                    onPrimary = { onEvent(CameraEvent.RequestPermission) },
                    secondaryLabel = stringResource(R.string.camera_open_settings),
                    onSecondary = { onEvent(CameraEvent.OpenSettings) },
                )
            }
        }
    }
}

@Composable
private fun CameraControls(
    uiState: CameraUiState,
    onEvent: (CameraEvent) -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val landscape = maxWidth > maxHeight

        IconButton(
            onClick = { onEvent(CameraEvent.OpenSettings) },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(8.dp)
                .testTag("camera_settings"),
        ) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = stringResource(R.string.camera_open_settings),
                tint = Color.White,
            )
        }

        if (landscape) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .navigationBarsPadding()
                    .padding(end = 20.dp),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CameraSideButton(
                    icon = Icons.Filled.PhotoLibrary,
                    contentDescription = stringResource(R.string.camera_open_gallery),
                    onClick = { onEvent(CameraEvent.OpenGallery) },
                    testTag = "camera_gallery",
                )
                ShutterButton(
                    enabled = !uiState.isCapturing,
                    onClick = { onEvent(CameraEvent.Capture) },
                )
                CameraSideButton(
                    icon = Icons.Filled.Cameraswitch,
                    contentDescription = stringResource(R.string.camera_switch),
                    onClick = { onEvent(CameraEvent.SwitchCamera) },
                    testTag = "camera_switch",
                )
                if (uiState.flashSupported) {
                    FlashButton(
                        mode = uiState.flashMode,
                        enabled = !uiState.isCapturing,
                        onClick = { onEvent(CameraEvent.CycleFlash) },
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 28.dp, vertical = 28.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CameraSideButton(
                    icon = Icons.Filled.PhotoLibrary,
                    contentDescription = stringResource(R.string.camera_open_gallery),
                    onClick = { onEvent(CameraEvent.OpenGallery) },
                    testTag = "camera_gallery",
                )
                ShutterButton(
                    enabled = !uiState.isCapturing,
                    onClick = { onEvent(CameraEvent.Capture) },
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CameraSideButton(
                        icon = Icons.Filled.Cameraswitch,
                        contentDescription = stringResource(R.string.camera_switch),
                        onClick = { onEvent(CameraEvent.SwitchCamera) },
                        testTag = "camera_switch",
                    )
                    if (uiState.flashSupported) {
                        Spacer(modifier = Modifier.height(8.dp))
                        FlashButton(
                            mode = uiState.flashMode,
                            enabled = !uiState.isCapturing,
                            onClick = { onEvent(CameraEvent.CycleFlash) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ShutterButton(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val description = stringResource(R.string.camera_shutter)
    Box(
        modifier = Modifier
            .size(84.dp)
            .clip(CircleShape)
            .border(4.dp, Color.White, CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(6.dp)
            .testTag("camera_shutter")
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(if (enabled) Color.White else Color.White.copy(alpha = 0.4f)),
        )
    }
}

@Composable
private fun CameraSideButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    testTag: String,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(52.dp)
            .testTag(testTag),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(28.dp),
        )
    }
}

@Composable
private fun FlashButton(
    mode: FlashModeOption,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val icon = when (mode) {
        FlashModeOption.Off -> Icons.Filled.FlashOff
        FlashModeOption.On -> Icons.Filled.FlashOn
        FlashModeOption.Auto -> Icons.Filled.FlashAuto
    }
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(52.dp)
            .testTag("camera_flash"),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = stringResource(R.string.camera_flash),
            tint = Color.White,
            modifier = Modifier.size(28.dp),
        )
    }
}

@Composable
private fun CaptureProgressOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f))
            .testTag("camera_capturing"),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Color.White)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.camera_capturing),
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
private fun ErrorBanner(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.errorContainer)
            .clickable(onClick = onDismiss)
            .padding(16.dp)
            .testTag("camera_error"),
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onErrorContainer,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun CameraMessagePane(
    title: String,
    body: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(28.dp))
        ArtifaceWidePrimaryButton(
            text = primaryLabel,
            onClick = onPrimary,
            modifier = Modifier.testTag("camera_permission_primary"),
        )
        if (secondaryLabel != null && onSecondary != null) {
            Spacer(modifier = Modifier.height(12.dp))
            ArtifaceSecondaryButton(
                text = secondaryLabel,
                onClick = onSecondary,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun CameraPreviewHost(
    lensFacing: Int,
    onImageCaptureReady: (ImageCapture?) -> Unit,
    onFlashSupported: (Boolean) -> Unit,
    onCameraUnavailable: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    LaunchedEffect(lensFacing) {
        val cameraProvider = context.getCameraProvider()
        try {
            cameraProvider.unbindAll()
            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }
            val imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()
            val selector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()
            val camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                selector,
                preview,
                imageCapture,
            )
            onImageCaptureReady(imageCapture)
            onFlashSupported(camera.cameraInfo.hasFlashUnit())
        } catch (_: Exception) {
            // Front camera missing or no camera hardware — try back, else mark unavailable.
            if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                try {
                    cameraProvider.unbindAll()
                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }
                    val imageCapture = ImageCapture.Builder().build()
                    val camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageCapture,
                    )
                    onImageCaptureReady(imageCapture)
                    onFlashSupported(camera.cameraInfo.hasFlashUnit())
                } catch (_: Exception) {
                    onImageCaptureReady(null)
                    onCameraUnavailable()
                }
            } else {
                onImageCaptureReady(null)
                onCameraUnavailable()
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            onImageCaptureReady(null)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener(
                { cameraProviderFuture.get().unbindAll() },
                ContextCompat.getMainExecutor(context),
            )
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier.testTag("camera_preview"),
    )
}

private suspend fun Context.getCameraProvider(): ProcessCameraProvider =
    suspendCoroutine { continuation ->
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener(
            { continuation.resume(future.get()) },
            ContextCompat.getMainExecutor(this),
        )
    }

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
