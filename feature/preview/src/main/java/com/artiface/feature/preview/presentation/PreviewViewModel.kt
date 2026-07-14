package com.artiface.feature.preview.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artiface.core.common.selfie.SelfieRepository
import com.artiface.core.model.CapturedSelfie
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PreviewUiState(
    val selfieId: String = "",
    val selfie: CapturedSelfie? = null,
    val isLoading: Boolean = true,
    val missing: Boolean = false,
)

sealed interface PreviewEvent {
    data object Retake : PreviewEvent
    data object Continue : PreviewEvent
}

sealed interface PreviewEffect {
    data object NavigateRetake : PreviewEffect
    data class NavigateContinue(val selfieId: String) : PreviewEffect
}

@HiltViewModel
class PreviewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    selfieRepository: SelfieRepository,
) : ViewModel() {

    private val selfieId: String = checkNotNull(savedStateHandle["selfieId"])

    val uiState: StateFlow<PreviewUiState> = selfieRepository.observeById(selfieId)
        .map { selfie ->
            PreviewUiState(
                selfieId = selfieId,
                selfie = selfie,
                isLoading = false,
                missing = selfie == null,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = PreviewUiState(selfieId = selfieId, isLoading = true),
        )

    private val _effects = MutableSharedFlow<PreviewEffect>(extraBufferCapacity = 1)
    val effects: SharedFlow<PreviewEffect> = _effects.asSharedFlow()

    fun onEvent(event: PreviewEvent) {
        when (event) {
            PreviewEvent.Retake -> viewModelScope.launch {
                _effects.emit(PreviewEffect.NavigateRetake)
            }
            PreviewEvent.Continue -> {
                val id = uiState.value.selfie?.id ?: return
                viewModelScope.launch {
                    _effects.emit(PreviewEffect.NavigateContinue(id))
                }
            }
        }
    }
}
