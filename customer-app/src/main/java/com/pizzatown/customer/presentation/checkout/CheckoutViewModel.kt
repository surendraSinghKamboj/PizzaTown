package com.pizzatown.customer.presentation.checkout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pizzatown.customer.core.analytics.AnalyticsLogger
import com.pizzatown.customer.core.location.LocationProvider
import com.pizzatown.customer.core.location.haversineDistanceKm
import com.pizzatown.customer.core.payment.CashfreeCheckoutBridge
import com.pizzatown.customer.domain.model.*
import com.pizzatown.customer.domain.repository.AuthRepository
import com.pizzatown.customer.domain.repository.CartRepository
import com.pizzatown.customer.domain.repository.CouponRepository
import com.pizzatown.customer.domain.repository.MenuRepository
import com.pizzatown.customer.domain.repository.OrderRepository
import com.pizzatown.customer.domain.repository.PaymentRepository
import com.pizzatown.customer.domain.repository.ProfileRepository
import com.pizzatown.customer.domain.repository.SettingsRepository
import com.pizzatown.customer.domain.usecase.CalculateCartTotalUseCase
import com.pizzatown.customer.domain.usecase.CouponValidationResult
import com.pizzatown.customer.domain.usecase.ValidateCouponUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/** Fine-grained progress for the ONLINE payment flow, so the UI never
 *  shows a bare/ambiguous spinner (spec: no infinite spinners). */
enum class PaymentStage {
    IDLE,
    CREATING_ORDER,
    OPENING_GATEWAY,
    AWAITING_GATEWAY,
    VERIFYING
}

data class CheckoutUiState(
    val name: String = "",
    val phone: String = "",
    val cartItems: List<CartItem> = emptyList(),
    val specialInstructions: String = "",

    // Address selection — a returning customer picks from saved addresses;
    // a first-time customer (no saved addresses yet) is shown the add-address
    // form directly instead of being asked at registration.
    val addresses: List<Address> = emptyList(),
    val selectedAddressId: String? = null,
    val showAddAddressForm: Boolean = false,
    val newAddressLabel: String = "Home",
    val newAddressText: String = "",

    // Coupon
    val couponCodeInput: String = "",
    val isApplyingCoupon: Boolean = false,
    val appliedCoupon: Coupon? = null,
    val couponDiscount: Double = 0.0,
    val couponError: String? = null,

    // Shop status / delivery area gating — see placeOrder().
    // null = not loaded yet, so we don't wrongly show "closed" for a flash.
    val restaurantOpen: Boolean? = null,
    val locationPermissionGranted: Boolean = false,
    val deliveryAreaConfigured: Boolean = false,
    val locationInsideDeliveryArea: Boolean? = null,
    val deliveryRadiusKm: Double = 0.0,
    val minimumOrderValue: Double = 0.0,
    val deliveryCharge: Double = 0.0,
    val freeDeliveryAbove: Double = 0.0,
    val isCheckingDeliveryArea: Boolean = false,

    // Payment method selection: ONLINE (Cashfree) or COD.
    val paymentMethod: PaymentMethod = PaymentMethod.ONLINE,

    val isPlacingOrder: Boolean = false,
    val paymentStage: PaymentStage = PaymentStage.IDLE,
    val paymentFailedMessage: String? = null,
    val errorMessage: String? = null,
    val priceChangedNotice: String? = null,
    val orderPlaced: Order? = null
) {
    /** True while any order/payment work is in flight — used to disable the
     *  CTA and prevent duplicate taps/duplicate order creation. */
    val isBusy: Boolean get() = isPlacingOrder || paymentStage != PaymentStage.IDLE
}

@HiltViewModel
class CheckoutViewModel @Inject constructor(
    private val cartRepository: CartRepository,
    private val menuRepository: MenuRepository,
    private val orderRepository: OrderRepository,
    private val paymentRepository: PaymentRepository,
    private val profileRepository: ProfileRepository,
    private val authRepository: AuthRepository,
    private val couponRepository: CouponRepository,
    private val settingsRepository: SettingsRepository,
    private val locationProvider: LocationProvider,
    private val calculateCartTotal: CalculateCartTotalUseCase,
    private val validateCoupon: ValidateCouponUseCase,
    private val analyticsLogger: AnalyticsLogger
) : ViewModel() {

    private val _uiState = MutableStateFlow(CheckoutUiState())
    val uiState: StateFlow<CheckoutUiState> = _uiState.asStateFlow()

    private var beginCheckoutLogged = false
    private var deliveryArea: DeliveryArea = DeliveryArea()

    // The order currently awaiting an ONLINE payment outcome from
    // CashfreeCheckoutBridge (set right after we create it in Firestore
    // and request the Cashfree checkout screen; cleared once verified).
    private var pendingOnlineOrder: Order? = null

    init {
        viewModelScope.launch {
            CashfreeCheckoutBridge.outcomes.collect { outcome -> handleCashfreeOutcome(outcome) }
        }
        viewModelScope.launch {
            cartRepository.observeCart().collect { items ->
                _uiState.value = _uiState.value.copy(cartItems = items)
                // Log once per checkout visit, the first time we see a
                // non-empty cart — not on every cart mutation while the
                // customer is still on this screen.
                if (!beginCheckoutLogged && items.isNotEmpty()) {
                    beginCheckoutLogged = true
                    analyticsLogger.logBeginCheckout(items, items.sumOf { it.lineTotal })
                }
            }
        }
        viewModelScope.launch {
            settingsRepository.observeRestaurantStatus().collect { status ->
                _uiState.value = _uiState.value.copy(restaurantOpen = status.isOpen)
            }
        }
        viewModelScope.launch {
            settingsRepository.observeDeliveryPricing().collect { pricing ->
                _uiState.value = _uiState.value.copy(
                    minimumOrderValue = pricing.minimumOrderValue,
                    deliveryCharge = pricing.deliveryCharge,
                    freeDeliveryAbove = pricing.freeDeliveryAbove
                )
            }
        }

        viewModelScope.launch {
            settingsRepository.observeDeliveryArea().collect { area ->
                deliveryArea = area
                _uiState.value = _uiState.value.copy(
                    deliveryAreaConfigured = area.isConfigured() && area.radiusKm > 0.0,
                    deliveryRadiusKm = area.radiusKm,
                    locationInsideDeliveryArea = null
                )
                if (locationProvider.hasLocationPermission()) {
                    refreshDeliveryEligibility()
                }
            }
        }
        val hasLocationPermission = locationProvider.hasLocationPermission()
        _uiState.value = _uiState.value.copy(
            locationPermissionGranted = hasLocationPermission
        )
        if (hasLocationPermission) {
            refreshDeliveryEligibility()
        }
        val userId = authRepository.currentUserId
        if (userId != null) {
            viewModelScope.launch {
                profileRepository.getProfile(userId).onSuccess { profile ->
                    val defaultAddress = profile.addresses.find { it.isDefault } ?: profile.addresses.firstOrNull()
                    _uiState.value = _uiState.value.copy(
                        name = profile.fullName,
                        phone = profile.mobile,
                        addresses = profile.addresses,
                        selectedAddressId = defaultAddress?.id,
                        // First order: no saved address yet, so show the add-address
                        // form right here instead of ever having asked at sign-up.
                        showAddAddressForm = profile.addresses.isEmpty()
                    )
                }
            }
        }
    }

    private fun refreshDeliveryEligibility() {
        if (!locationProvider.hasLocationPermission()) {
            _uiState.value = _uiState.value.copy(
                locationPermissionGranted = false,
                locationInsideDeliveryArea = null,
                isCheckingDeliveryArea = false
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                locationPermissionGranted = true,
                isCheckingDeliveryArea = true,
                locationInsideDeliveryArea = null
            )

            val area = deliveryArea

            if (!area.isConfigured() || area.radiusKm <= 0.0) {
                _uiState.value = _uiState.value.copy(
                    deliveryAreaConfigured = false,
                    deliveryRadiusKm = area.radiusKm,
                    locationInsideDeliveryArea = false,
                    isCheckingDeliveryArea = false
                )
                return@launch
            }

            val location = locationProvider.getCurrentLocation()

            if (location == null) {
                _uiState.value = _uiState.value.copy(
                    deliveryAreaConfigured = true,
                    deliveryRadiusKm = area.radiusKm,
                    locationInsideDeliveryArea = false,
                    isCheckingDeliveryArea = false
                )
                return@launch
            }

            val distanceKm = haversineDistanceKm(
                location.latitude,
                location.longitude,
                area.centerLat,
                area.centerLng
            )

            _uiState.value = _uiState.value.copy(
                deliveryAreaConfigured = true,
                deliveryRadiusKm = area.radiusKm,
                locationInsideDeliveryArea = distanceKm <= area.radiusKm,
                isCheckingDeliveryArea = false
            )
        }
    }

    /** Call after a runtime permission request completes (see CheckoutScreen). */
    fun onLocationPermissionResult(granted: Boolean) {
        _uiState.value = _uiState.value.copy(
            locationPermissionGranted = granted,
            errorMessage = null
        )
        if (granted) {
            refreshDeliveryEligibility()
        }
    }

    fun onNameChange(v: String) { _uiState.value = _uiState.value.copy(name = v, errorMessage = null) }
    fun onPhoneChange(v: String) { _uiState.value = _uiState.value.copy(phone = v, errorMessage = null) }
    fun onInstructionsChange(v: String) { _uiState.value = _uiState.value.copy(specialInstructions = v) }
    fun onPaymentMethodChange(method: PaymentMethod) {
        if (_uiState.value.isBusy) return
        _uiState.value = _uiState.value.copy(paymentMethod = method, errorMessage = null, paymentFailedMessage = null)
    }

    fun selectAddress(addressId: String) {
        _uiState.value = _uiState.value.copy(selectedAddressId = addressId, showAddAddressForm = false, errorMessage = null)
    }

    fun startAddingNewAddress() {
        _uiState.value = _uiState.value.copy(showAddAddressForm = true, newAddressText = "", newAddressLabel = "Home")
    }

    fun cancelAddingNewAddress() {
        // Only allow cancel if they already have at least one saved address to fall back on.
        if (_uiState.value.addresses.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(showAddAddressForm = false)
        }
    }

    fun onNewAddressLabelChange(v: String) { _uiState.value = _uiState.value.copy(newAddressLabel = v) }
    fun onNewAddressTextChange(v: String) { _uiState.value = _uiState.value.copy(newAddressText = v, errorMessage = null) }

    fun saveNewAddress() {
        val state = _uiState.value
        val userId = authRepository.currentUserId ?: return
        if (state.newAddressText.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Please enter your delivery address.")
            return
        }
        viewModelScope.launch {
            val profile = profileRepository.getProfile(userId).getOrNull() ?: return@launch
            val newAddress = Address(
                id = UUID.randomUUID().toString(),
                label = state.newAddressLabel.ifBlank { "Address" },
                fullAddress = state.newAddressText.trim(),
                isDefault = profile.addresses.isEmpty()
            )
            val updatedAddresses = profile.addresses + newAddress
            profileRepository.updateProfile(profile.copy(addresses = updatedAddresses)).onSuccess {
                _uiState.value = _uiState.value.copy(
                    addresses = updatedAddresses,
                    selectedAddressId = newAddress.id,
                    showAddAddressForm = false,
                    errorMessage = null
                )
            }
        }
    }

    // ---- Coupon ----

    fun onCouponCodeChange(v: String) {
        _uiState.value = _uiState.value.copy(couponCodeInput = v, couponError = null)
    }

    fun applyCoupon() {
        val state = _uiState.value
        val code = state.couponCodeInput.trim()
        if (code.isBlank()) {
            _uiState.value = state.copy(couponError = "Enter a coupon code.")
            return
        }
        val subtotal = state.cartItems.sumOf { it.lineTotal }
        val userId = authRepository.currentUserId

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isApplyingCoupon = true, couponError = null)
            val coupon = couponRepository.getCouponByCode(code).getOrNull()
            when (val result = validateCoupon(coupon, subtotal, userId)) {
                is CouponValidationResult.Valid -> {
                    _uiState.value = _uiState.value.copy(
                        isApplyingCoupon = false,
                        appliedCoupon = coupon,
                        couponDiscount = result.discountAmount,
                        couponError = null
                    )
                    analyticsLogger.logApplyCoupon(code, result.discountAmount)
                }
                is CouponValidationResult.Invalid -> {
                    _uiState.value = _uiState.value.copy(
                        isApplyingCoupon = false,
                        appliedCoupon = null,
                        couponDiscount = 0.0,
                        couponError = result.reason
                    )
                }
            }
        }
    }

    fun removeCoupon() {
        _uiState.value = _uiState.value.copy(
            appliedCoupon = null, couponDiscount = 0.0, couponCodeInput = "", couponError = null
        )
    }

    fun placeOrder() {
        val state = _uiState.value
        val userId = authRepository.currentUserId
        val selectedAddress = state.addresses.find { it.id == state.selectedAddressId }

        if (state.isBusy) return // prevent duplicate taps / duplicate order creation

        if (state.cartItems.isEmpty()) {
            _uiState.value = state.copy(errorMessage = "Your cart is empty.")
            return
        }
        if (state.name.isBlank() || state.phone.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Please fill in your name and mobile number.")
            return
        }
        if (selectedAddress == null) {
            _uiState.value = state.copy(errorMessage = "Please add or select a delivery address.")
            return
        }
        if (userId == null) {
            _uiState.value = state.copy(errorMessage = "You must be signed in to place an order.")
            return
        }
        if (state.restaurantOpen == false) {
            _uiState.value = state.copy(errorMessage = "Pizza Town is currently closed. Please check back when we're open.")
            return
        }
        if (!locationProvider.hasLocationPermission()) {
            _uiState.value = state.copy(
                errorMessage = "Location permission is required so we can confirm you're within our delivery area. Please allow it and try again."
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isPlacingOrder = true, errorMessage = null, priceChangedNotice = null, paymentFailedMessage = null
            )

            // Mandatory delivery-area check: fetch the device's current
            // location and reject the order if it's outside the shop's
            // configured radius. This is the client-side half of the
            // check — firestore.rules' restaurantIsOpen() covers the
            // open/closed gate server-side, and the createCashfreeOrder /
            // onOrderCreated Cloud Functions re-check both server-side too,
            // but a precise geo-radius check needs trig functions Firestore
            // rules can't do, so this client check is the first line here.
            val location = locationProvider.getCurrentLocation()
            if (location == null) {
                _uiState.value = _uiState.value.copy(
                    isPlacingOrder = false,
                    errorMessage = "Couldn't get your current location. Please make sure GPS/location is turned on and try again."
                )
                return@launch
            }
            if (!deliveryArea.isConfigured() || deliveryArea.radiusKm <= 0.0) {
                _uiState.value = _uiState.value.copy(
                    isPlacingOrder = false,
                    errorMessage = "The restaurant delivery area is not configured yet. Please try again in a moment."
                )
                return@launch
            }

            val distanceKm = haversineDistanceKm(
                location.latitude, location.longitude,
                deliveryArea.centerLat, deliveryArea.centerLng
            )

            if (distanceKm > deliveryArea.radiusKm) {
                _uiState.value = _uiState.value.copy(
                    isPlacingOrder = false,
                    errorMessage = "Sorry, your location is outside our delivery area (we currently deliver within ${deliveryArea.radiusKm.toInt()} km of the shop)."
                )
                return@launch
            }

            // Re-validate availability and pricing right before checkout (spec #58):
            // never let a stale price/availability produce an incorrect order.
            var pricesChanged = false
            val revalidatedItems = mutableListOf<CartItem>()
            for (cartItem in state.cartItems) {
                val fresh = menuRepository.refreshMenuItem(cartItem.menuItemId).getOrNull()
                if (fresh == null || !fresh.available) {
                    _uiState.value = _uiState.value.copy(
                        isPlacingOrder = false,
                        errorMessage = "\"${cartItem.menuItemName}\" is no longer available. Please update your cart."
                    )
                    return@launch
                }
                val freshUnitPrice = when {
                    cartItem.selectedVariantId != null ->
                        fresh.variants.find { it.id == cartItem.selectedVariantId }?.price ?: cartItem.basePrice
                    else -> fresh.basePrice
                }
                if (freshUnitPrice != cartItem.basePrice) pricesChanged = true
                revalidatedItems.add(cartItem.copy(basePrice = freshUnitPrice))
            }

            if (pricesChanged) {
                _uiState.value = _uiState.value.copy(
                    isPlacingOrder = false,
                    cartItems = revalidatedItems,
                    priceChangedNotice = "Some prices have changed. Your cart has been updated. Please review it before placing the order."
                )
                return@launch
            }

            // Re-validate the coupon too, right before charging — someone
            // else could have used up the last redemption in the meantime.
            var couponDiscount = state.couponDiscount
            var couponCode = ""
            val appliedCoupon = state.appliedCoupon
            if (appliedCoupon != null) {
                val freshCoupon = couponRepository.getCouponByCode(appliedCoupon.code).getOrNull()
                val revalidation = validateCoupon(freshCoupon, revalidatedItems.sumOf { it.lineTotal }, userId)
                when (revalidation) {
                    is CouponValidationResult.Valid -> {
                        couponDiscount = revalidation.discountAmount
                        couponCode = appliedCoupon.code
                    }
                    is CouponValidationResult.Invalid -> {
                        _uiState.value = _uiState.value.copy(
                            isPlacingOrder = false,
                            appliedCoupon = null,
                            couponDiscount = 0.0,
                            couponError = "Your coupon is no longer valid: ${revalidation.reason}"
                        )
                        return@launch
                    }
                }
            }

            val pricing = DeliveryPricing(
                minimumOrderValue = _uiState.value.minimumOrderValue,
                deliveryCharge = _uiState.value.deliveryCharge,
                freeDeliveryAbove = _uiState.value.freeDeliveryAbove
            )

            val subtotal = revalidatedItems.sumOf { it.lineTotal }

            if (!pricing.minimumOrderSatisfied(subtotal)) {
                _uiState.value = _uiState.value.copy(
                    isPlacingOrder = false,
                    errorMessage =
                        "Minimum order value is ₹${pricing.minimumOrderValue.toInt()}. "
                            + "Add ₹${(pricing.minimumOrderValue - subtotal).coerceAtLeast(0.0).toInt()} more to continue."
                )
                return@launch
            }

            val deliveryFee = pricing.expectedDeliveryFee(subtotal)

            val totals = calculateCartTotal(
                revalidatedItems,
                discount = couponDiscount,
                deliveryFee = deliveryFee
            )
            val paymentMethod = state.paymentMethod
            val order = Order(
                userId = userId,
                customer = OrderCustomer(state.name.trim(), state.phone.trim(), selectedAddress.fullAddress),
                items = revalidatedItems.map { item ->
                    OrderLineItem(
                        menuItemId = item.menuItemId,
                        name = item.menuItemName,
                        variantName = item.selectedVariantName,
                        customizationNames = item.selectedOptions.map {
                            if (it.priceAdjustment > 0) "${it.optionName} \u00D7 ${item.quantity} = \u20B9${(it.priceAdjustment * item.quantity).toInt()}"
                            else it.optionName
                        },
                        quantity = item.quantity,
                        unitPrice = item.finalUnitPrice,
                        lineTotal = item.lineTotal
                    )
                },
                subtotal = totals.subtotal,
                discount = totals.discount,
                couponCode = couponCode,
                deliveryFee = totals.deliveryFee,
                tax = totals.tax,
                grandTotal = totals.grandTotal,
                totalItems = totals.totalItemCount,
                specialInstructions = state.specialInstructions.trim(),
                status = if (paymentMethod == PaymentMethod.ONLINE) {
                    OrderStatus.BEING_PAYMENT
                } else {
                    OrderStatus.PENDING
                },
                deliveryLat = location.latitude,
                deliveryLng = location.longitude,
                paymentMethod = paymentMethod,
                paymentStatus = if (paymentMethod == PaymentMethod.COD) PaymentStatus.NOT_REQUIRED else PaymentStatus.PENDING
            )

            orderRepository.createOrder(order)
                .onSuccess { savedOrder ->
                    // Coupon usage is recorded the moment the order exists,
                    // for both COD and ONLINE — matches the WhatsApp-era
                    // behavior and avoids letting a coupon be redeemed twice
                    // just because an online payment hasn't finished yet.
                    if (appliedCoupon != null) {
                        couponRepository.incrementUsage(appliedCoupon.id)
                    }
                    analyticsLogger.logPurchase(
                        orderId = savedOrder.orderId,
                        items = revalidatedItems,
                        subtotal = totals.subtotal,
                        discount = totals.discount,
                        grandTotal = totals.grandTotal,
                        couponCode = couponCode.ifBlank { null }
                    )

                    if (paymentMethod == PaymentMethod.COD) {
                        cartRepository.clearCart()
                        _uiState.value = _uiState.value.copy(isPlacingOrder = false, orderPlaced = savedOrder)
                    } else {
                        startOnlinePayment(savedOrder)
                    }
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        isPlacingOrder = false,
                        errorMessage = "We couldn't save your order. Please check your internet connection and try again."
                    )
                }
        }
    }

    /** Requests a Cashfree payment session for an ONLINE order that has
     *  already been saved to Firestore (paymentStatus = PENDING), then
     *  asks [CashfreeCheckoutBridge] (backed by MainActivity) to open the
     *  Cashfree checkout screen. */
    private fun startOnlinePayment(savedOrder: Order) {
        pendingOnlineOrder = savedOrder
        _uiState.value = _uiState.value.copy(
            isPlacingOrder = false,
            paymentStage = PaymentStage.CREATING_ORDER
        )
        viewModelScope.launch {
            paymentRepository.createCashfreeOrder(savedOrder.orderId)
                .onSuccess { session ->
                    _uiState.value = _uiState.value.copy(paymentStage = PaymentStage.OPENING_GATEWAY)
                    CashfreeCheckoutBridge.requestCheckout(session.orderId, session.paymentSessionId)
                    _uiState.value = _uiState.value.copy(paymentStage = PaymentStage.AWAITING_GATEWAY)
                }
                .onFailure {
                    // Keep the already-created Firestore order so Retry Payment
                    // can create/open a fresh Cashfree payment session for the
                    // same order. Never create a duplicate Firestore order.
                    _uiState.value = _uiState.value.copy(
                        paymentStage = PaymentStage.IDLE,
                        paymentFailedMessage = "We couldn't start the payment. Your order is safe — please try again."
                    )
                }
        }
    }

    /** Retries the ONLINE payment for the order that's already saved (e.g.
     *  after a network error opening the gateway, or a cancelled/failed
     *  attempt) — never creates a second Firestore order. */
    fun retryOnlinePayment() {
        val state = _uiState.value

        if (state.isBusy) return
        if (state.paymentMethod != PaymentMethod.ONLINE) return

        // The previous temporary order may have been deleted by the backend
        // after Cashfree Back/Cancel. Never reuse that order for retry.
        pendingOnlineOrder = null

        // Re-run the complete checkout validation/order-creation flow.
        // This creates a fresh temporary ONLINE order and a fresh Cashfree
        // payment session.
        placeOrder()
    }

    private fun handleCashfreeOutcome(outcome: CashfreeCheckoutBridge.Outcome) {
        val order = pendingOnlineOrder ?: return
        if (outcome.orderId != order.orderId) return // not for the order currently in flight

        when (outcome) {
            is CashfreeCheckoutBridge.Outcome.Verify -> verifyOnlinePayment(order)
            is CashfreeCheckoutBridge.Outcome.Failure -> {
                // Cashfree Back/Cancel is NOT payment success.
                // Ask the backend to verify the real Cashfree state before
                // removing the temporary unpaid Firestore order.
                _uiState.value = _uiState.value.copy(
                    paymentStage = PaymentStage.IDLE,
                    paymentFailedMessage = "Payment was cancelled. You can try again."
                )

                viewModelScope.launch {
                    paymentRepository.abandonCashfreeOrder(order.orderId)
                        .onSuccess {
                            // Backend deletes the temporary unpaid order.
                            // If Cashfree reports PAID, backend preserves it.
                            pendingOnlineOrder = null
                        }
                        .onFailure {
                            // Fail safe: keep the local order reference so
                            // Retry Payment remains possible if the backend
                            // could not confirm the payment state.
                            _uiState.value = _uiState.value.copy(
                                paymentFailedMessage =
                                    "We couldn't confirm the payment cancellation. Please try again."
                            )
                        }
                }
            }
        }
    }

    private fun verifyOnlinePayment(order: Order, gatewayFailureMessage: String? = null) {
        _uiState.value = _uiState.value.copy(paymentStage = PaymentStage.VERIFYING)
        viewModelScope.launch {
            paymentRepository.verifyCashfreePayment(order.orderId)
                .onSuccess { result ->

                    val updatedOrder = order.copy(
                        status = if (result.paymentStatus == PaymentStatus.PAID) {
                            OrderStatus.PENDING
                        } else {
                            order.status
                        },
                        paymentStatus = result.paymentStatus,
                        cashfreePaymentId = result.cashfreePaymentId ?: order.cashfreePaymentId
                    )
                    when (result.paymentStatus) {
                        PaymentStatus.PAID -> {
                            cartRepository.clearCart()
                            _uiState.value = _uiState.value.copy(paymentStage = PaymentStage.IDLE, orderPlaced = updatedOrder)
                        }
                        PaymentStatus.PENDING -> {
                            // Payment is not confirmed yet.
                            // Do NOT show Order Placed and do NOT expose the
                            // temporary BEING_PAYMENT order as a completed order.
                            _uiState.value = _uiState.value.copy(
                                paymentStage = PaymentStage.IDLE,
                                paymentFailedMessage =
                                    "Payment is still being processed. Please check again shortly."
                            )
                        }
                        else -> {
                            _uiState.value = _uiState.value.copy(
                                paymentStage = PaymentStage.IDLE,
                                paymentFailedMessage =
                                    "Payment wasn't completed. Your order is safe and hasn't been charged. Please try again.")
                        }
                    }
                }
                .onFailure {
                    // Couldn't even reach the backend to confirm — leave the
                    // order as-is (still PENDING) so the customer can retry
                    // verification instead of losing the order entirely.
                    _uiState.value = _uiState.value.copy(
                        paymentStage = PaymentStage.IDLE,
                        paymentFailedMessage = "We couldn't confirm your payment status. Please check your internet connection, then check My Orders — if it shows as pending, try again from there."
                    )
                }
        }
    }

    fun consumeOrderPlaced() {
        _uiState.value = _uiState.value.copy(orderPlaced = null)
    }
}
