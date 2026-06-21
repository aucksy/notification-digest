package com.notdigest.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notdigest.app.domain.model.ThemeMode
import com.notdigest.app.domain.repository.PreferencesRepository
import com.notdigest.app.ui.navigation.NavRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class AppUiState(
    val loading: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val onboardingComplete: Boolean = false,
) {
    val startDestination: String
        get() = if (onboardingComplete) NavRoutes.HOME else NavRoutes.ONBOARDING
}

/** Holds the global app state needed before any screen draws: theme & start destination. */
@HiltViewModel
class AppViewModel @Inject constructor(
    preferencesRepository: PreferencesRepository,
) : ViewModel() {

    val uiState = preferencesRepository.preferences
        .map { prefs ->
            AppUiState(
                loading = false,
                themeMode = prefs.themeMode,
                dynamicColor = prefs.dynamicColor,
                hapticsEnabled = prefs.hapticsEnabled,
                onboardingComplete = prefs.onboardingComplete,
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AppUiState(loading = true),
        )
}
