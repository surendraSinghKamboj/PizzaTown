package com.pizzatown.admin.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pizzatown.admin.core.location.AdminLocationProvider
import com.pizzatown.admin.domain.model.DeliveryArea
import com.pizzatown.admin.domain.model.DeliveryPricing
import com.pizzatown.admin.domain.model.RestaurantStatus
import com.pizzatown.admin.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DeliveryAreaFormState(
    val centerLat: String = "",
    val centerLng: String = "",
    val radiusKm: String = "",
    val isDirty: Boolean = false
)


data class DeliveryPricingFormState(
    val minimumOrderValue: String = "",
    val deliveryCharge: String = "",
    val freeDeliveryAbove: String = "",
    val isDirty: Boolean = false
)

@HiltViewModel
class ShopSettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val locationProvider: AdminLocationProvider
) : ViewModel() {

    val restaurantStatus: StateFlow<RestaurantStatus> = settingsRepository.observeRestaurantStatus()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RestaurantStatus())

    private val _formState = MutableStateFlow(DeliveryAreaFormState())
    val formState: StateFlow<DeliveryAreaFormState> = _formState.asStateFlow()

    
    val deliveryPricing: StateFlow<DeliveryPricing> =
        settingsRepository.observeDeliveryPricing()
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                DeliveryPricing()
            )

    private val _pricingState = MutableStateFlow(DeliveryPricingFormState())
    val pricingState: StateFlow<DeliveryPricingFormState> = _pricingState.asStateFlow()

    private val _saveMessage = MutableStateFlow<String?>(null)
    val saveMessage: StateFlow<String?> = _saveMessage.asStateFlow()

    init {
        // Prefill the form from the saved area, but only until the admin
        // starts editing — otherwise every Firestore echo of our own save
        // would stomp on whatever they're mid-typing.
        viewModelScope.launch {
            settingsRepository.observeDeliveryPricing().collect { pricing ->
                if (!_pricingState.value.isDirty) {
                    _pricingState.value = DeliveryPricingFormState(
                        minimumOrderValue = pricing.minimumOrderValue.toString(),
                        deliveryCharge = pricing.deliveryCharge.toString(),
                        freeDeliveryAbove = pricing.freeDeliveryAbove.toString()
                    )
                }
            }
        }

        viewModelScope.launch {
            settingsRepository.observeDeliveryArea().collect { area ->
                if (!_formState.value.isDirty) {
                    _formState.value = DeliveryAreaFormState(
                        centerLat = if (area.isConfigured()) area.centerLat.toString() else "",
                        centerLng = if (area.isConfigured()) area.centerLng.toString() else "",
                        radiusKm = area.radiusKm.toString()
                    )
                }
            }
        }
    }

    fun toggleOpen(isOpen: Boolean) {
        viewModelScope.launch {
            val result = settingsRepository.setRestaurantOpen(isOpen)
            result.onSuccess {
                android.util.Log.d("PizzaTownAdmin", "Restaurant status saved: isOpen=$isOpen")
                _saveMessage.value = if (isOpen) "Restaurant opened." else "Restaurant closed."
            }.onFailure {
                android.util.Log.e("PizzaTownAdmin", "Failed to save restaurant status: isOpen=$isOpen", it)
                _saveMessage.value = "Failed to update restaurant status: ${it.message ?: "Unknown error"}"
            }
        }
    }

    fun fetchCurrentLocation() {
        viewModelScope.launch {
            val location = locationProvider.getCurrentLocation()

            if (location == null) {
                _saveMessage.value =
                    "Unable to get current location. Check GPS and location permission."
                return@launch
            }

            _formState.value = _formState.value.copy(
                centerLat = location.latitude.toString(),
                centerLng = location.longitude.toString(),
                isDirty = true
            )

            _saveMessage.value = "Current location loaded."
        }
    }

    fun onLatChange(value: String) { _formState.value = _formState.value.copy(centerLat = value, isDirty = true) }
    fun onLngChange(value: String) { _formState.value = _formState.value.copy(centerLng = value, isDirty = true) }
    fun onRadiusChange(value: String) { _formState.value = _formState.value.copy(radiusKm = value, isDirty = true) }

    fun onLatitudeChange(value: String) {
        _formState.value = _formState.value.copy(
            centerLat = value,
            isDirty = true
        )
    }

    fun onLongitudeChange(value: String) {
        _formState.value = _formState.value.copy(
            centerLng = value,
            isDirty = true
        )
    }


    fun saveDeliveryArea() {
        val state = _formState.value
        val lat = state.centerLat.toDoubleOrNull()
        val lng = state.centerLng.toDoubleOrNull()
        val radius = state.radiusKm.toDoubleOrNull()
        if (lat == null || lng == null || lat !in -90.0..90.0 || lng !in -180.0..180.0) {
            _saveMessage.value = "Enter a valid latitude (-90 to 90) and longitude (-180 to 180)."
            return
        }
        if (radius == null || radius <= 0.0) {
            _saveMessage.value = "Enter a delivery radius greater than 0 km."
            return
        }
        viewModelScope.launch {
            settingsRepository.updateDeliveryArea(DeliveryArea(centerLat = lat, centerLng = lng, radiusKm = radius))
                .onSuccess {
                    _formState.value = _formState.value.copy(isDirty = false)
                    _saveMessage.value = "Delivery area saved."
                }
                .onFailure { _saveMessage.value = it.message ?: "Failed to save delivery area." }
        }
    }


    fun onMinimumOrderValueChange(value: String) {
        _pricingState.value = _pricingState.value.copy(
            minimumOrderValue = value,
            isDirty = true
        )
    }

    fun onDeliveryChargeChange(value: String) {
        _pricingState.value = _pricingState.value.copy(
            deliveryCharge = value,
            isDirty = true
        )
    }

    fun onFreeDeliveryAboveChange(value: String) {
        _pricingState.value = _pricingState.value.copy(
            freeDeliveryAbove = value,
            isDirty = true
        )
    }

    fun saveDeliveryPricing() {
        val state = _pricingState.value

        val minimum = state.minimumOrderValue.toDoubleOrNull()
        val delivery = state.deliveryCharge.toDoubleOrNull()
        val freeAbove = state.freeDeliveryAbove.toDoubleOrNull()

        if (minimum == null || minimum < 0.0) {
            _saveMessage.value = "Enter a valid minimum order value."
            return
        }

        if (delivery == null || delivery < 0.0) {
            _saveMessage.value = "Enter a valid delivery charge."
            return
        }

        if (freeAbove == null || freeAbove < 0.0) {
            _saveMessage.value = "Enter a valid free-delivery threshold."
            return
        }

        if (freeAbove > 0.0 && freeAbove < minimum) {
            _saveMessage.value =
                "Free delivery threshold must be at least the minimum order value."
            return
        }

        viewModelScope.launch {
            settingsRepository.updateDeliveryPricing(
                DeliveryPricing(
                    minimumOrderValue = minimum,
                    deliveryCharge = delivery,
                    freeDeliveryAbove = freeAbove
                )
            ).onSuccess {
                _pricingState.value = _pricingState.value.copy(isDirty = false)
                _saveMessage.value = "Order pricing saved."
            }.onFailure {
                _saveMessage.value =
                    it.message ?: "Failed to save order pricing."
            }
        }
    }

    fun clearMessage() { _saveMessage.value = null }
}
