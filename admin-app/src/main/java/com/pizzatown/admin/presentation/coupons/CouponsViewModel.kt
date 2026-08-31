package com.pizzatown.admin.presentation.coupons

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pizzatown.admin.core.common.UiState
import com.pizzatown.admin.domain.model.Coupon
import com.pizzatown.admin.domain.repository.CouponRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CouponsViewModel @Inject constructor(
    private val couponRepository: CouponRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    /** null = both active and inactive */
    private val _activeFilter = MutableStateFlow<Boolean?>(null)
    val activeFilter: StateFlow<Boolean?> = _activeFilter.asStateFlow()

    private val rawCoupons: StateFlow<UiState<List<Coupon>>> = couponRepository.observeCoupons()
        .map { list -> if (list.isEmpty()) UiState.Empty else UiState.Success(list) as UiState<List<Coupon>> }
        .catch { emit(UiState.Error(it.message ?: "Failed to load coupons")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    val couponsState: StateFlow<UiState<List<Coupon>>> =
        combine(rawCoupons, _searchQuery, _activeFilter) { state, query, active ->
            if (state !is UiState.Success) return@combine state
            var list = state.data
            if (active != null) list = list.filter { it.active == active }
            if (query.isNotBlank()) {
                val q = query.trim().lowercase()
                list = list.filter { it.code.lowercase().contains(q) || it.targetCustomerName.lowercase().contains(q) }
            }
            if (list.isEmpty()) UiState.Empty else UiState.Success(list)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun setActiveFilter(active: Boolean?) { _activeFilter.value = active }

    fun setActive(coupon: Coupon, active: Boolean) {
        viewModelScope.launch { couponRepository.setActive(coupon.id, active) }
    }

    fun deleteCoupon(coupon: Coupon) {
        viewModelScope.launch { couponRepository.deleteCoupon(coupon.id) }
    }
}
