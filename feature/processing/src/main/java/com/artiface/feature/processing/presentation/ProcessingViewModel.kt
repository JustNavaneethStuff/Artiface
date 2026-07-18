package com.artiface.feature.processing.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artiface.core.common.generation.GenerationRepository
import com.artiface.core.model.GenerationJob
import com.artiface.core.model.GenerationStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProcessingUiState(
    val job: GenerationJob? = null,
    val messageIndex: Int = 0,
    val humorousMessage: String = ProcessingMessages.all.first(),
    val isFailed: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface ProcessingEvent {
    data object Retry : ProcessingEvent
    data object GoBack : ProcessingEvent
}

sealed interface ProcessingEffect {
    data class NavigateToResult(val resultId: String) : ProcessingEffect
    data object NavigateBack : ProcessingEffect
}

@HiltViewModel
class ProcessingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val generationRepository: GenerationRepository,
) : ViewModel() {

    private val jobId: String = checkNotNull(savedStateHandle["jobId"])

    private val messageTicks = kotlinx.coroutines.flow.flow {
        var index = 0
        while (true) {
            emit(index)
            delay(1_600)
            index = (index + 1) % ProcessingMessages.all.size
        }
    }

    val uiState: StateFlow<ProcessingUiState> = combine(
        generationRepository.observeJob(jobId),
        messageTicks,
    ) { job, messageIndex ->
        ProcessingUiState(
            job = job,
            messageIndex = messageIndex,
            humorousMessage = ProcessingMessages.all[messageIndex],
            isFailed = job?.status == GenerationStatus.Failed,
            errorMessage = job?.errorMessage,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProcessingUiState(),
    )

    private val _effects = MutableSharedFlow<ProcessingEffect>(extraBufferCapacity = 1)
    val effects: SharedFlow<ProcessingEffect> = _effects.asSharedFlow()

    init {
        // Cold-start / process-death recovery: Room emits the latest job, including Completed.
        viewModelScope.launch {
            var navigatedResultId: String? = null
            generationRepository.observeJob(jobId).collect { job ->
                if (job?.status != GenerationStatus.Completed) return@collect
                val resultId = generationRepository.getResultIdForJob(jobId) ?: return@collect
                if (resultId == navigatedResultId) return@collect
                navigatedResultId = resultId
                _effects.emit(ProcessingEffect.NavigateToResult(resultId))
            }
        }
    }

    fun onEvent(event: ProcessingEvent) {
        when (event) {
            ProcessingEvent.Retry -> viewModelScope.launch {
                runCatching { generationRepository.retry(jobId) }
            }
            ProcessingEvent.GoBack -> viewModelScope.launch {
                _effects.emit(ProcessingEffect.NavigateBack)
            }
        }
    }
}

object ProcessingMessages {
    val all = listOf(
        "Measuring dramatic potential",
        "Negotiating with your eyebrows",
        "Adding unnecessary grandeur",
        "Consulting the colour spirits",
        "Exaggerating responsibly",
        "Polishing the absurdity",
        "Asking the mirror for a second opinion",
    )
}
