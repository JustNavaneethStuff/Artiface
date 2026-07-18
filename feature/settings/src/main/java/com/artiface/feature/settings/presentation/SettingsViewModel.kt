package com.artiface.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artiface.core.common.di.AppVersion
import com.artiface.core.common.generation.GenerationRepository
import com.artiface.core.model.AppThemeMode
import com.artiface.core.model.UserPreferences
import com.artiface.core.preferences.UserPreferencesRepository
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
import javax.inject.Inject

data class SettingsUiState(
    val preferences: UserPreferences = UserPreferences(),
    val appVersion: String = "",
    val showClearGalleryDialog: Boolean = false,
)

sealed interface SettingsEvent {
    data object Back : SettingsEvent
    data class ThemeSelected(val mode: AppThemeMode) : SettingsEvent
    data class ContextualPersonalizationToggled(val enabled: Boolean) : SettingsEvent
    data class LocationContextToggled(val enabled: Boolean) : SettingsEvent
    data object ClearGalleryClicked : SettingsEvent
    data object ClearGalleryConfirmed : SettingsEvent
    data object ClearGalleryDismissed : SettingsEvent
}

sealed interface SettingsEffect {
    data object NavigateBack : SettingsEffect
    data object GalleryCleared : SettingsEffect
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository,
    private val generationRepository: GenerationRepository,
    @AppVersion appVersion: String,
) : ViewModel() {

    private val showClearDialog = MutableStateFlow(false)

    val uiState: StateFlow<SettingsUiState> = combine(
        preferencesRepository.preferences,
        showClearDialog,
    ) { prefs, dialogVisible ->
        SettingsUiState(
            preferences = prefs,
            appVersion = appVersion,
            showClearGalleryDialog = dialogVisible,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = SettingsUiState(appVersion = appVersion),
    )

    private val _effects = MutableSharedFlow<SettingsEffect>(extraBufferCapacity = 1)
    val effects: SharedFlow<SettingsEffect> = _effects.asSharedFlow()

    fun onEvent(event: SettingsEvent) {
        when (event) {
            SettingsEvent.Back -> viewModelScope.launch {
                _effects.emit(SettingsEffect.NavigateBack)
            }
            is SettingsEvent.ThemeSelected -> viewModelScope.launch {
                preferencesRepository.setThemeMode(event.mode)
            }
            is SettingsEvent.ContextualPersonalizationToggled -> viewModelScope.launch {
                preferencesRepository.setContextualPersonalizationEnabled(event.enabled)
            }
            is SettingsEvent.LocationContextToggled -> viewModelScope.launch {
                preferencesRepository.setLocationContextEnabled(event.enabled)
            }
            SettingsEvent.ClearGalleryClicked -> showClearDialog.value = true
            SettingsEvent.ClearGalleryDismissed -> showClearDialog.value = false
            SettingsEvent.ClearGalleryConfirmed -> viewModelScope.launch {
                showClearDialog.value = false
                generationRepository.clearGallery()
                _effects.emit(SettingsEffect.GalleryCleared)
            }
        }
    }
}
