package com.notdigest.app.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notdigest.app.core.util.ThemeSchedule
import com.notdigest.app.core.util.TimeProvider
import com.notdigest.app.domain.model.ThemeMode
import com.notdigest.app.domain.model.UserPreferences
import com.notdigest.app.domain.repository.PreferencesRepository
import com.notdigest.app.service.ListenerKeepAliveService
import com.notdigest.app.service.NotificationAccessState
import com.notdigest.app.ui.navigation.NavRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.delay
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
    private val time: TimeProvider,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    // Run the always-on keep-alive whenever the app has notification access (its core permission) —
    // no separate setting; it just runs, like Pause's monitor. The FGS is what keeps the listener
    // bound; START_STICKY + BootReceiver cover restarts. Started here (a foreground context while the
    // UI is up) and re-evaluated when the listener (dis)connects, i.e. when access is granted/revoked.
    init {
        NotificationAccessState.connected
            .map { NotificationAccessState.isGranted(appContext) }
            .distinctUntilChanged()
            .onEach { granted -> ListenerKeepAliveService.sync(appContext, granted) }
            .launchIn(viewModelScope)
    }

    // Drives re-evaluation of SCHEDULED mode while the app is open, so the theme actually flips when the
    // clock crosses the window edge. Only runs while uiState is subscribed (foreground), so it costs
    // nothing in the background; identical AppUiState values are skipped by Compose, so a tick that
    // doesn't change the resolved theme triggers no recomposition.
    private val minuteTicker = flow {
        while (true) {
            emit(Unit)
            delay(60_000)
        }
    }

    val uiState = combine(preferencesRepository.preferences, minuteTicker) { prefs, _ ->
        AppUiState(
            loading = false,
            themeMode = resolveThemeMode(prefs),
            dynamicColor = prefs.dynamicColor,
            hapticsEnabled = prefs.hapticsEnabled,
            onboardingComplete = prefs.onboardingComplete,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppUiState(loading = true),
    )

    /** Collapse SCHEDULED into a concrete LIGHT/DARK for the current wall-clock; pass others through. */
    private fun resolveThemeMode(prefs: UserPreferences): ThemeMode {
        if (prefs.themeMode != ThemeMode.SCHEDULED) return prefs.themeMode
        val dark = ThemeSchedule.isDarkAt(time.now(), time.zone(), prefs.darkModeStartTime, prefs.darkModeEndTime)
        return if (dark) ThemeMode.DARK else ThemeMode.LIGHT
    }
}
