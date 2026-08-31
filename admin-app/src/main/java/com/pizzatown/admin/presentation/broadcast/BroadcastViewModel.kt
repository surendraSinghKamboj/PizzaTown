package com.pizzatown.admin.presentation.broadcast

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pizzatown.admin.core.common.OpState
import com.pizzatown.admin.core.common.UiState
import com.pizzatown.admin.domain.model.Broadcast
import com.pizzatown.admin.domain.model.Customer
import com.pizzatown.admin.domain.repository.BroadcastRepository
import com.pizzatown.admin.domain.repository.CustomerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BroadcastComposeState(
    val title: String = "",
    val message: String = "",
    val targetCustomer: Customer? = null, // null = send to everyone
    val customers: List<Customer> = emptyList()
)

@HiltViewModel
class BroadcastViewModel @Inject constructor(
    private val broadcastRepository: BroadcastRepository,
    customerRepository: CustomerRepository
) : ViewModel() {

    val historyState: StateFlow<UiState<List<Broadcast>>> = broadcastRepository.observeBroadcasts()
        .map { list -> if (list.isEmpty()) UiState.Empty else UiState.Success(list) as UiState<List<Broadcast>> }
        .catch { emit(UiState.Error(it.message ?: "Failed to load history")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    private val _composeState = MutableStateFlow(BroadcastComposeState())
    val composeState: StateFlow<BroadcastComposeState> = _composeState.asStateFlow()

    private val _opState = MutableStateFlow<OpState>(OpState.Idle)
    val opState: StateFlow<OpState> = _opState.asStateFlow()

    init {
        viewModelScope.launch {
            customerRepository.observeCustomers().collect { customers ->
                _composeState.value = _composeState.value.copy(customers = customers)
            }
        }
    }

    fun onTitleChange(v: String) { _composeState.value = _composeState.value.copy(title = v) }
    fun onMessageChange(v: String) { _composeState.value = _composeState.value.copy(message = v) }
    fun onTargetCustomerChange(customer: Customer?) { _composeState.value = _composeState.value.copy(targetCustomer = customer) }

    fun sendBroadcast() {
        val state = _composeState.value
        if (state.title.isBlank() || state.message.isBlank()) {
            _opState.value = OpState.Error("Title and message are required.")
            return
        }
        viewModelScope.launch {
            _opState.value = OpState.InProgress
            val result = broadcastRepository.send(
                title = state.title,
                message = state.message,
                targetUserId = state.targetCustomer?.userId,
                targetCustomerName = state.targetCustomer?.fullName.orEmpty()
            )
            _opState.value = result.fold(
                onSuccess = {
                    _composeState.value = BroadcastComposeState(customers = state.customers)
                    OpState.Success
                },
                onFailure = { OpState.Error(it.message ?: "Failed to send") }
            )
        }
    }

    fun deleteBroadcast(broadcast: Broadcast) {
        viewModelScope.launch { broadcastRepository.delete(broadcast.id) }
    }

    fun consumeOpState() { _opState.value = OpState.Idle }
}
