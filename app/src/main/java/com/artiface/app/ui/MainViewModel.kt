package com.artiface.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artiface.core.model.AppThemeMode
import com.artiface.core.preferences.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    preferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    val themeMode: StateFlow<AppThemeMode> = preferencesRepository.preferences
        .map { it.themeMode }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = AppThemeMode.System,
        )
}
