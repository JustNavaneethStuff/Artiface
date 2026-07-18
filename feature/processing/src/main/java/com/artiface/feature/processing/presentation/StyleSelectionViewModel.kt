package com.artiface.feature.processing.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artiface.core.common.generation.GenerationRepository
import com.artiface.core.model.CaricatureStyle
import com.artiface.core.model.StyleCatalog
import com.artiface.core.model.StyleId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StyleSelectionUiState(
    val selfieId: String = "",
    val styles: List<CaricatureStyle> = StyleCatalog.all,
    val selectedStyleId: StyleId? = null,
    val isStarting: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface StyleSelectionEvent {
    data class StyleClicked(val styleId: StyleId) : StyleSelectionEvent
    data object Continue : StyleSelectionEvent
    data object DismissError : StyleSelectionEvent
}

sealed interface StyleSelectionEffect {
    data class NavigateToProcessing(val jobId: String) : StyleSelectionEffect
}

@HiltViewModel
class StyleSelectionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val generationRepository: GenerationRepository,
) : ViewModel() {

    private val selfieId: String = checkNotNull(savedStateHandle["selfieId"])

    private val _uiState = MutableStateFlow(
        StyleSelectionUiState(selfieId = selfieId, selectedStyleId = StyleCatalog.ComicBurst.id),
    )
    val uiState: StateFlow<StyleSelectionUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<StyleSelectionEffect>(extraBufferCapacity = 1)
    val effects: SharedFlow<StyleSelectionEffect> = _effects.asSharedFlow()

    fun onEvent(event: StyleSelectionEvent) {
        when (event) {
            is StyleSelectionEvent.StyleClicked -> {
                _uiState.update { it.copy(selectedStyleId = event.styleId, errorMessage = null) }
            }
            StyleSelectionEvent.Continue -> startSelected()
            StyleSelectionEvent.DismissError -> _uiState.update { it.copy(errorMessage = null) }
        }
    }

    private fun startSelected() {
        val styleId = _uiState.value.selectedStyleId ?: return
        if (_uiState.value.isStarting) return
        viewModelScope.launch {
            _uiState.update { it.copy(isStarting = true, errorMessage = null) }
            runCatching {
                generationRepository.startGeneration(selfieId, styleId)
            }.onSuccess { job ->
                _effects.emit(StyleSelectionEffect.NavigateToProcessing(job.id))
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isStarting = false,
                        errorMessage = error.message ?: "Could not start generation",
                    )
                }
            }
            _uiState.update { it.copy(isStarting = false) }
        }
    }
}
