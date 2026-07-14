package com.artiface.feature.camera.presentation

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artiface.core.common.result.Result
import com.artiface.feature.camera.domain.SelfieCaptureStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

enum class CameraPermissionStatus {
    Unknown,
    Granted,
    Denied,
    PermanentlyDenied,
}

enum class FlashModeOption {
    Off,
    On,
    Auto,
}

data class CameraUiState(
    val permissionStatus: CameraPermissionStatus = CameraPermissionStatus.Unknown,
    val hasRequestedPermission: Boolean = false,
    val lensFacing: Int = CameraSelector.LENS_FACING_FRONT,
    val flashMode: FlashModeOption = FlashModeOption.Off,
    val flashSupported: Boolean = false,
    val isCapturing: Boolean = false,
    val cameraAvailable: Boolean = true,
    val errorMessage: String? = null,
)

sealed interface CameraEvent {
    data object PermissionResultGranted : CameraEvent
    data class PermissionResultDenied(val permanently: Boolean) : CameraEvent
    data object RequestPermission : CameraEvent
    data object OpenAppSettings : CameraEvent
    data object SwitchCamera : CameraEvent
    data object CycleFlash : CameraEvent
    data object Capture : CameraEvent
    data object OpenGallery : CameraEvent
    data object OpenSettings : CameraEvent
    data object DismissError : CameraEvent
    data class FlashAvailabilityChanged(val supported: Boolean) : CameraEvent
    data class CaptureSucceeded(val selfieId: String) : CameraEvent
    data class CaptureFailed(val message: String) : CameraEvent
}

sealed interface CameraEffect {
    data object RequestCameraPermission : CameraEffect
    data object OpenAppSettings : CameraEffect
    data object OpenGallery : CameraEffect
    data object OpenSettings : CameraEffect
    data class NavigateToPreview(val selfieId: String) : CameraEffect
    data class PerformCapture(
        val lensFacing: Int,
        val flashMode: Int,
    ) : CameraEffect
}

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val captureStore: SelfieCaptureStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<CameraEffect>(extraBufferCapacity = 1)
    val effects: SharedFlow<CameraEffect> = _effects.asSharedFlow()

    private val captureInFlight = AtomicBoolean(false)

    fun onEvent(event: CameraEvent) {
        when (event) {
            CameraEvent.PermissionResultGranted -> {
                _uiState.update {
                    it.copy(
                        permissionStatus = CameraPermissionStatus.Granted,
                        errorMessage = null,
                    )
                }
            }
            is CameraEvent.PermissionResultDenied -> {
                _uiState.update {
                    it.copy(
                        permissionStatus = if (event.permanently) {
                            CameraPermissionStatus.PermanentlyDenied
                        } else {
                            CameraPermissionStatus.Denied
                        },
                    )
                }
            }
            CameraEvent.RequestPermission -> viewModelScope.launch {
                _uiState.update { it.copy(hasRequestedPermission = true) }
                _effects.emit(CameraEffect.RequestCameraPermission)
            }
            CameraEvent.OpenAppSettings -> viewModelScope.launch {
                _effects.emit(CameraEffect.OpenAppSettings)
            }
            CameraEvent.SwitchCamera -> {
                if (_uiState.value.isCapturing) return
                _uiState.update {
                    val next = if (it.lensFacing == CameraSelector.LENS_FACING_FRONT) {
                        CameraSelector.LENS_FACING_BACK
                    } else {
                        CameraSelector.LENS_FACING_FRONT
                    }
                    it.copy(
                        lensFacing = next,
                        flashMode = FlashModeOption.Off,
                        flashSupported = false,
                    )
                }
            }
            CameraEvent.CycleFlash -> {
                if (!_uiState.value.flashSupported || _uiState.value.isCapturing) return
                _uiState.update {
                    it.copy(
                        flashMode = when (it.flashMode) {
                            FlashModeOption.Off -> FlashModeOption.On
                            FlashModeOption.On -> FlashModeOption.Auto
                            FlashModeOption.Auto -> FlashModeOption.Off
                        },
                    )
                }
            }
            CameraEvent.Capture -> startCapture()
            CameraEvent.OpenGallery -> viewModelScope.launch {
                _effects.emit(CameraEffect.OpenGallery)
            }
            CameraEvent.OpenSettings -> viewModelScope.launch {
                _effects.emit(CameraEffect.OpenSettings)
            }
            CameraEvent.DismissError -> _uiState.update { it.copy(errorMessage = null) }
            is CameraEvent.FlashAvailabilityChanged -> {
                _uiState.update {
                    it.copy(
                        flashSupported = event.supported,
                        flashMode = if (event.supported) it.flashMode else FlashModeOption.Off,
                    )
                }
            }
            is CameraEvent.CaptureSucceeded -> {
                captureInFlight.set(false)
                _uiState.update { it.copy(isCapturing = false, errorMessage = null) }
                viewModelScope.launch {
                    _effects.emit(CameraEffect.NavigateToPreview(event.selfieId))
                }
            }
            is CameraEvent.CaptureFailed -> {
                captureInFlight.set(false)
                _uiState.update {
                    it.copy(isCapturing = false, errorMessage = event.message)
                }
            }
        }
    }

    fun onCameraHardwareUnavailable() {
        _uiState.update { it.copy(cameraAvailable = false) }
    }

    fun createTempCaptureFile() = captureStore.createTempCaptureFile()

    fun onImageSaved(tempFile: java.io.File) {
        viewModelScope.launch {
            when (val result = captureStore.persistCapturedImage(tempFile)) {
                is Result.Success -> onEvent(CameraEvent.CaptureSucceeded(result.data.id))
                is Result.Error -> onEvent(
                    CameraEvent.CaptureFailed(
                        result.message ?: "Could not save the photo",
                    ),
                )
            }
        }
    }

    private fun startCapture() {
        val state = _uiState.value
        if (state.permissionStatus != CameraPermissionStatus.Granted) {
            viewModelScope.launch { _effects.emit(CameraEffect.RequestCameraPermission) }
            return
        }
        if (state.isCapturing || !captureInFlight.compareAndSet(false, true)) return

        _uiState.update { it.copy(isCapturing = true, errorMessage = null) }
        viewModelScope.launch {
            _effects.emit(
                CameraEffect.PerformCapture(
                    lensFacing = state.lensFacing,
                    flashMode = state.flashMode.toImageCaptureMode(),
                ),
            )
        }
    }
}

fun FlashModeOption.toImageCaptureMode(): Int = when (this) {
    FlashModeOption.Off -> ImageCapture.FLASH_MODE_OFF
    FlashModeOption.On -> ImageCapture.FLASH_MODE_ON
    FlashModeOption.Auto -> ImageCapture.FLASH_MODE_AUTO
}
