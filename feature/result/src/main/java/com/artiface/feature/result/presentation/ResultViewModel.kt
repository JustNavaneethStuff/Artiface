package com.artiface.feature.result.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artiface.core.common.generation.GenerationRepository
import com.artiface.core.model.CaricatureResult
import com.artiface.core.model.StyleCatalog
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

data class ResultUiState(
    val result: CaricatureResult? = null,
    val styleName: String = "",
    val contextSummary: String = "",
    val selfieId: String? = null,
    val isLoading: Boolean = true,
    val missing: Boolean = false,
    val revealProgress: Boolean = false,
)

sealed interface ResultEvent {
    data object ToggleFavourite : ResultEvent
    data object Share : ResultEvent
    data object Save : ResultEvent
    data object TryAnotherStyle : ResultEvent
    data object CreateAnother : ResultEvent
    data object OpenGallery : ResultEvent
}

sealed interface ResultEffect {
    data class ShareImage(val imageUri: String, val title: String) : ResultEffect
    data class SaveImage(val imageUri: String, val title: String) : ResultEffect
    data class NavigateTryAnotherStyle(val selfieId: String) : ResultEffect
    data object NavigateCreateAnother : ResultEffect
    data object NavigateGallery : ResultEffect
    data class ShowMessage(val messageRes: Int) : ResultEffect
}

@HiltViewModel
class ResultViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val generationRepository: GenerationRepository,
) : ViewModel() {

    private val resultId: String = checkNotNull(savedStateHandle["resultId"])

    val uiState: StateFlow<ResultUiState> = generationRepository.observeResult(resultId)
        .map { result ->
            if (result == null) {
                ResultUiState(isLoading = false, missing = true)
            } else {
                val styleName = runCatching { StyleCatalog.require(result.styleId).name }
                    .getOrDefault(result.styleId.value)
                ResultUiState(
                    result = result,
                    styleName = styleName,
                    contextSummary = buildContextSummary(result),
                    selfieId = generationRepository.getSelfieIdForResult(result.id),
                    isLoading = false,
                    missing = false,
                    revealProgress = true,
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = ResultUiState(),
        )

    private val _effects = MutableSharedFlow<ResultEffect>(extraBufferCapacity = 1)
    val effects: SharedFlow<ResultEffect> = _effects.asSharedFlow()

    fun onEvent(event: ResultEvent) {
        when (event) {
            ResultEvent.ToggleFavourite -> viewModelScope.launch {
                val current = uiState.value.result ?: return@launch
                generationRepository.setFavourite(current.id, !current.isFavourite)
            }
            ResultEvent.Share -> viewModelScope.launch {
                val result = uiState.value.result ?: return@launch
                _effects.emit(ResultEffect.ShareImage(result.generatedImageUri, result.title))
            }
            ResultEvent.Save -> viewModelScope.launch {
                val result = uiState.value.result ?: return@launch
                _effects.emit(ResultEffect.SaveImage(result.generatedImageUri, result.title))
            }
            ResultEvent.TryAnotherStyle -> viewModelScope.launch {
                val selfieId = uiState.value.selfieId ?: return@launch
                _effects.emit(ResultEffect.NavigateTryAnotherStyle(selfieId))
            }
            ResultEvent.CreateAnother -> viewModelScope.launch {
                _effects.emit(ResultEffect.NavigateCreateAnother)
            }
            ResultEvent.OpenGallery -> viewModelScope.launch {
                _effects.emit(ResultEffect.NavigateGallery)
            }
        }
    }

    private fun buildContextSummary(result: CaricatureResult): String {
        val parts = buildList {
            add(result.expression.name.lowercase())
            add(result.timeOfDay.name.lowercase())
            result.broadLocationLabel?.let { add(it) }
        }
        return parts.joinToString(" · ")
    }
}
