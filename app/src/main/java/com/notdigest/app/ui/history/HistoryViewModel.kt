package com.notdigest.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notdigest.app.domain.model.Digest
import com.notdigest.app.domain.repository.DigestRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryUiState(
    val digests: List<Digest> = emptyList(),
    val query: String = "",
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val digestRepository: DigestRepository,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val eventChannel = Channel<String>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    val uiState = combine(
        query.flatMapLatest { q -> digestRepository.searchDigests(q) },
        query,
    ) { digests, q ->
        HistoryUiState(digests = digests, query = q)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    fun onQueryChange(value: String) { query.value = value }

    fun delete(id: Long) {
        viewModelScope.launch {
            digestRepository.deleteDigest(id)
            eventChannel.send("Digest deleted")
        }
    }
}
