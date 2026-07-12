package com.artiface.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artiface.core.preferences.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SplashEffect {
    data class NavigateForward(val hasCompletedOnboarding: Boolean) : SplashEffect
}

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val _effects = MutableSharedFlow<SplashEffect>(extraBufferCapacity = 1)
    val effects: SharedFlow<SplashEffect> = _effects.asSharedFlow()

    init {
        viewModelScope.launch {
            delay(900)
            val prefs = preferencesRepository.preferences.first()
            _effects.emit(SplashEffect.NavigateForward(prefs.hasCompletedOnboarding))
        }
    }
}
