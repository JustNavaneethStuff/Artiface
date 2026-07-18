package com.artiface.feature.gallery.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artiface.core.common.generation.GenerationRepository
import com.artiface.core.model.GalleryItem
import com.artiface.core.model.StyleCatalog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import javax.inject.Inject

data class GalleryRowUi(
    val id: String,
    val title: String,
    val styleName: String,
    val statusLabel: String,
    val createdAtLabel: String,
    val thumbnailUri: String,
    val isFavourite: Boolean,
)

data class GalleryUiState(
    val items: List<GalleryRowUi> = emptyList(),
    val favouritesOnly: Boolean = false,
    val isEmpty: Boolean = true,
    val pendingDeleteId: String? = null,
)

sealed interface GalleryEvent {
    data object Back : GalleryEvent
    data object ToggleFavouritesFilter : GalleryEvent
    data class OpenResult(val resultId: String) : GalleryEvent
    data class DeleteClicked(val resultId: String) : GalleryEvent
    data object DeleteConfirmed : GalleryEvent
    data object DeleteDismissed : GalleryEvent
}

sealed interface GalleryEffect {
    data object NavigateBack : GalleryEffect
    data class NavigateToResult(val resultId: String) : GalleryEffect
}

@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val generationRepository: GenerationRepository,
) : ViewModel() {

    private val favouritesOnly = MutableStateFlow(false)
    private val pendingDeleteId = MutableStateFlow<String?>(null)

    val uiState: StateFlow<GalleryUiState> = combine(
        generationRepository.observeGalleryItems(),
        favouritesOnly,
        pendingDeleteId,
    ) { items, favOnly, deleteId ->
        val filtered = if (favOnly) items.filter { it.result.isFavourite } else items
        GalleryUiState(
            items = filtered.map { it.toRowUi() },
            favouritesOnly = favOnly,
            isEmpty = filtered.isEmpty(),
            pendingDeleteId = deleteId,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = GalleryUiState(),
    )

    private val _effects = MutableSharedFlow<GalleryEffect>(extraBufferCapacity = 1)
    val effects: SharedFlow<GalleryEffect> = _effects.asSharedFlow()

    fun onEvent(event: GalleryEvent) {
        when (event) {
            GalleryEvent.Back -> viewModelScope.launch {
                _effects.emit(GalleryEffect.NavigateBack)
            }
            GalleryEvent.ToggleFavouritesFilter -> {
                favouritesOnly.value = !favouritesOnly.value
            }
            is GalleryEvent.OpenResult -> viewModelScope.launch {
                _effects.emit(GalleryEffect.NavigateToResult(event.resultId))
            }
            is GalleryEvent.DeleteClicked -> pendingDeleteId.value = event.resultId
            GalleryEvent.DeleteDismissed -> pendingDeleteId.value = null
            GalleryEvent.DeleteConfirmed -> viewModelScope.launch {
                val id = pendingDeleteId.value ?: return@launch
                pendingDeleteId.value = null
                generationRepository.deleteResult(id)
            }
        }
    }

    private fun GalleryItem.toRowUi(): GalleryRowUi {
        val styleName = runCatching { StyleCatalog.require(result.styleId).name }
            .getOrDefault(result.styleId.value)
        return GalleryRowUi(
            id = result.id,
            title = result.title,
            styleName = styleName,
            statusLabel = status.name,
            createdAtLabel = DATE_FORMAT.format(result.createdAt.atZone(ZoneId.systemDefault())),
            thumbnailUri = result.generatedImageUri,
            isFavourite = result.isFavourite,
        )
    }

    companion object {
        private val DATE_FORMAT = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    }
}
