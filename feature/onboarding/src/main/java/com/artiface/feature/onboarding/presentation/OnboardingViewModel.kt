package com.artiface.feature.onboarding.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artiface.core.preferences.UserPreferencesRepository
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

data class OnboardingUiState(
    val pageIndex: Int = 0,
    val pageCount: Int = OnboardingPages.COUNT,
    val isSaving: Boolean = false,
)

sealed interface OnboardingEvent {
    data object Skip : OnboardingEvent
    data object Next : OnboardingEvent
    data object GetStarted : OnboardingEvent
    data class PageSelected(val index: Int) : OnboardingEvent
}

sealed interface OnboardingEffect {
    data object NavigateToCamera : OnboardingEffect
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<OnboardingEffect>(extraBufferCapacity = 1)
    val effects: SharedFlow<OnboardingEffect> = _effects.asSharedFlow()

    fun onEvent(event: OnboardingEvent) {
        when (event) {
            OnboardingEvent.Skip, OnboardingEvent.GetStarted -> completeOnboarding()
            OnboardingEvent.Next -> {
                val state = _uiState.value
                if (state.pageIndex >= state.pageCount - 1) {
                    completeOnboarding()
                } else {
                    _uiState.update { it.copy(pageIndex = it.pageIndex + 1) }
                }
            }
            is OnboardingEvent.PageSelected -> {
                _uiState.update {
                    it.copy(pageIndex = event.index.coerceIn(0, it.pageCount - 1))
                }
            }
        }
    }

    private fun completeOnboarding() {
        if (_uiState.value.isSaving) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            preferencesRepository.setOnboardingCompleted(true)
            _effects.emit(OnboardingEffect.NavigateToCamera)
            _uiState.update { it.copy(isSaving = false) }
        }
    }
}

object OnboardingPages {
    const val COUNT = 3
}
