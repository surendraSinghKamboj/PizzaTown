package com.pizzatown.customer.presentation.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pizzatown.customer.core.common.UiState
import com.pizzatown.customer.core.preferences.NotificationPreferences
import com.pizzatown.customer.domain.model.Broadcast
import com.pizzatown.customer.domain.repository.BroadcastRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationInboxViewModel @Inject constructor(
    private val broadcastRepository: BroadcastRepository,
    private val notificationPreferences: NotificationPreferences
) : ViewModel() {

    val inboxState: StateFlow<UiState<List<Broadcast>>> = broadcastRepository.observeMyBroadcasts()
        .map { list -> if (list.isEmpty()) UiState.Empty else UiState.Success(list) as UiState<List<Broadcast>> }
        .catch { emit(UiState.Error(it.message ?: "Unable to load notifications right now.")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    /** Number of messages that arrived after the inbox was last opened — drives the bell badge. */
    val unreadCount: StateFlow<Int> = combine(
        broadcastRepository.observeMyBroadcasts(),
        notificationPreferences.lastSeenAt
    ) { messages, lastSeen -> messages.count { it.createdAt > lastSeen } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun markSeen() {
        viewModelScope.launch { notificationPreferences.markSeenNow() }
    }
}
