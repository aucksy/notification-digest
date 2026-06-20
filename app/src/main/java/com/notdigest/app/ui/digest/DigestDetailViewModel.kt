package com.notdigest.app.ui.digest

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notdigest.app.domain.model.AppGroup
import com.notdigest.app.domain.model.AppNotification
import com.notdigest.app.domain.model.Digest
import com.notdigest.app.domain.repository.DigestRepository
import com.notdigest.app.domain.repository.NotificationRepository
import com.notdigest.app.domain.system.LaunchResult
import com.notdigest.app.domain.usecase.GroupNotificationsUseCase
import com.notdigest.app.domain.usecase.OpenNotificationUseCase
import com.notdigest.app.ui.navigation.NavRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DigestDetailUiState(
    val digest: Digest? = null,
    val groups: List<AppGroup> = emptyList(),
    val loaded: Boolean = false,
)

@HiltViewModel
class DigestDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val digestRepository: DigestRepository,
    notificationRepository: NotificationRepository,
    private val groupNotifications: GroupNotificationsUseCase,
    private val openNotificationUseCase: OpenNotificationUseCase,
) : ViewModel() {

    private val digestId: Long = savedStateHandle[NavRoutes.ARG_DIGEST_ID] ?: 0L
    private val header = MutableStateFlow<Digest?>(null)

    private val eventChannel = Channel<String>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    val uiState = combine(
        header,
        notificationRepository.observeByDigest(digestId),
    ) { digest, items ->
        DigestDetailUiState(digest = digest, groups = groupNotifications(items), loaded = true)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DigestDetailUiState())

    init {
        viewModelScope.launch { header.value = digestRepository.getDigestWithItems(digestId)?.digest }
    }

    fun open(notification: AppNotification) {
        viewModelScope.launch {
            val result = openNotificationUseCase(notification)
            eventChannel.send(
                when (result) {
                    LaunchResult.DEEP_LINKED -> "Opening in ${notification.appName}…"
                    LaunchResult.OPENED_APP -> "Opened ${notification.appName}"
                    LaunchResult.FAILED -> "Couldn't open this notification"
                },
            )
        }
    }
}
