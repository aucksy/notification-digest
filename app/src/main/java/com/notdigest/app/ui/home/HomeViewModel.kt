package com.notdigest.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notdigest.app.core.util.TimeProvider
import com.notdigest.app.domain.model.AppRecommendation
import com.notdigest.app.domain.model.DigestMode
import com.notdigest.app.domain.model.DigestType
import com.notdigest.app.domain.model.NotificationStats
import com.notdigest.app.domain.repository.AppRuleRepository
import com.notdigest.app.domain.repository.RecommendationRepository
import com.notdigest.app.domain.repository.ScheduleRepository
import com.notdigest.app.domain.repository.StatsRepository
import com.notdigest.app.domain.usecase.ComputeNextDigestTimeUseCase
import com.notdigest.app.domain.usecase.DeliverDigestUseCase
import com.notdigest.app.domain.usecase.DeliverResult
import com.notdigest.app.ui.apps.AppsFilter
import com.notdigest.app.ui.apps.AppsFilterRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

// Note: the dashboard intentionally exposes NO pending notification content — only counts — so the
// user is never tempted to peek before delivering. The actual notifications surface in the Inbox
// (after delivery) and in History.
data class HomeUiState(
    val stats: NotificationStats = NotificationStats(),
    val nextDigestAt: Long? = null,
    val recommendations: List<AppRecommendation> = emptyList(),
    val isDelivering: Boolean = false,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    statsRepository: StatsRepository,
    scheduleRepository: ScheduleRepository,
    private val recommendationRepository: RecommendationRepository,
    private val appRuleRepository: AppRuleRepository,
    private val deliverDigestUseCase: DeliverDigestUseCase,
    private val computeNextDigestTime: ComputeNextDigestTimeUseCase,
    private val time: TimeProvider,
    private val appsFilterRequest: AppsFilterRequest,
) : ViewModel() {

    private val isDelivering = MutableStateFlow(false)
    private val eventChannel = Channel<String>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    val uiState = combine(
        statsRepository.observeStats(),
        scheduleRepository.observeSchedules(),
        recommendationRepository.observeRecommendations(),
        isDelivering,
    ) { stats, schedules, recs, delivering ->
        HomeUiState(
            stats = stats,
            nextDigestAt = computeNextDigestTime(schedules, time.now()),
            recommendations = recs,
            isDelivering = delivering,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun seeNow() {
        viewModelScope.launch {
            isDelivering.value = true
            val result = runCatching { deliverDigestUseCase(DigestType.MANUAL, postNotification = false) }.getOrNull()
            isDelivering.value = false
            eventChannel.send(
                when (result) {
                    is DeliverResult.Delivered ->
                        "${result.notificationCount} notifications added to your inbox"
                    else -> "Nothing to show yet"
                },
            )
        }
    }

    /** Ask the Apps tab to open showing only Real-Time apps (used by the "Real-Time apps" stat tile). */
    fun openRealtimeApps() { appsFilterRequest.request(AppsFilter.REALTIME) }

    fun applyRecommendation(recommendation: AppRecommendation) {
        viewModelScope.launch {
            appRuleRepository.setMode(recommendation.packageName, recommendation.appName, DigestMode.DIGEST)
            eventChannel.send("${recommendation.appName} is now in Digest")
            recommendationRepository.dismiss(recommendation.packageName)
        }
    }

    fun dismissRecommendation(recommendation: AppRecommendation) {
        viewModelScope.launch { recommendationRepository.dismiss(recommendation.packageName) }
    }
}
