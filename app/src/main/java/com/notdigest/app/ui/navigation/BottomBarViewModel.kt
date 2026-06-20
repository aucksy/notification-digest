package com.notdigest.app.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notdigest.app.domain.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Supplies the live "pending" count for the Inbox tab badge. */
@HiltViewModel
class BottomBarViewModel @Inject constructor(
    notificationRepository: NotificationRepository,
) : ViewModel() {
    val pendingCount = notificationRepository.observePendingCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
}
