package com.pizzatown.admin.presentation.customers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pizzatown.admin.core.common.UiState
import com.pizzatown.admin.domain.repository.CustomerRepository
import com.pizzatown.admin.domain.usecase.GetUpcomingCustomerEventsUseCase
import com.pizzatown.admin.domain.usecase.UpcomingCustomerEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class UpcomingEventsViewModel @Inject constructor(
    customerRepository: CustomerRepository,
    private val getUpcomingCustomerEvents: GetUpcomingCustomerEventsUseCase
) : ViewModel() {

    private val _windowDays = MutableStateFlow(30)
    val windowDays: StateFlow<Int> = _windowDays.asStateFlow()

    val eventsState: StateFlow<UiState<List<UpcomingCustomerEvent>>> = combine(
        customerRepository.observeCustomers(), _windowDays
    ) { customers, window ->
        val events = getUpcomingCustomerEvents(customers, windowDays = window)
        if (events.isEmpty()) UiState.Empty else UiState.Success(events) as UiState<List<UpcomingCustomerEvent>>
    }.catch { emit(UiState.Error(it.message ?: "Failed to load upcoming events")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    fun setWindowDays(days: Int) { _windowDays.value = days }
}
