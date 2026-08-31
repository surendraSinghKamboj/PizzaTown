package com.pizzatown.admin.presentation.coupons

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pizzatown.admin.domain.model.Coupon
import com.pizzatown.admin.domain.model.Customer
import com.pizzatown.admin.domain.model.DiscountType
import com.pizzatown.admin.domain.repository.CouponRepository
import com.pizzatown.admin.domain.repository.CustomerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CouponEditorUiState(
    val code: String = "",
    val discountType: DiscountType = DiscountType.PERCENTAGE,
    val discountValue: String = "",
    val minOrderValue: String = "0",
    val maxDiscountAmount: String = "0",
    val usageLimit: String = "0",
    val targetCustomer: Customer? = null,
    val customers: List<Customer> = emptyList(),
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val saveSuccess: Boolean = false
)

@HiltViewModel
class CouponEditorViewModel @Inject constructor(
    private val couponRepository: CouponRepository,
    customerRepository: CustomerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CouponEditorUiState())
    val uiState: StateFlow<CouponEditorUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            customerRepository.observeCustomers().collect { customers ->
                _uiState.value = _uiState.value.copy(customers = customers)
            }
        }
    }

    fun onCodeChange(v: String) { _uiState.value = _uiState.value.copy(code = v.uppercase(), errorMessage = null) }
    fun onDiscountTypeChange(v: DiscountType) { _uiState.value = _uiState.value.copy(discountType = v) }
    fun onDiscountValueChange(v: String) { _uiState.value = _uiState.value.copy(discountValue = v, errorMessage = null) }
    fun onMinOrderValueChange(v: String) { _uiState.value = _uiState.value.copy(minOrderValue = v) }
    fun onMaxDiscountAmountChange(v: String) { _uiState.value = _uiState.value.copy(maxDiscountAmount = v) }
    fun onUsageLimitChange(v: String) { _uiState.value = _uiState.value.copy(usageLimit = v) }
    fun onTargetCustomerChange(customer: Customer?) { _uiState.value = _uiState.value.copy(targetCustomer = customer) }

    fun save() {
        val state = _uiState.value
        if (state.code.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Coupon code is required.")
            return
        }
        val discountValue = state.discountValue.toDoubleOrNull()
        if (discountValue == null || discountValue <= 0.0) {
            _uiState.value = state.copy(errorMessage = "Enter a valid discount value.")
            return
        }
        if (state.discountType == DiscountType.PERCENTAGE && discountValue > 100.0) {
            _uiState.value = state.copy(errorMessage = "Percentage discount can't exceed 100%.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)

            val coupon = Coupon(
                code = state.code.trim(),
                discountType = state.discountType,
                discountValue = discountValue,
                minOrderValue = state.minOrderValue.toDoubleOrNull() ?: 0.0,
                maxDiscountAmount = state.maxDiscountAmount.toDoubleOrNull() ?: 0.0,
                targetUserId = state.targetCustomer?.userId,
                targetCustomerName = state.targetCustomer?.fullName.orEmpty(),
                usageLimit = state.usageLimit.toIntOrNull() ?: 0
            )

            val result = couponRepository.addCoupon(coupon)
            _uiState.value = result.fold(
                onSuccess = { _uiState.value.copy(isSaving = false, saveSuccess = true) },
                onFailure = { _uiState.value.copy(isSaving = false, errorMessage = it.message ?: "Failed to save coupon") }
            )
        }
    }
}
