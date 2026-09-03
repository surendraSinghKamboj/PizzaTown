package com.pizzatown.delivery.domain.model

data class DeliveryOrder(
    val orderId: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val customerAddress: String = "",
    val deliveryLat: Double = 0.0,
    val deliveryLng: Double = 0.0,
    val items: List<DeliveryOrderItem> = emptyList(),
    val subtotal: Double = 0.0,
    val deliveryFee: Double = 0.0,
    val grandTotal: Double = 0.0,
    val paymentMethod: String = "COD",
    val paymentStatus: String = "NOT_REQUIRED",
    val status: String = "READY",
    val specialInstructions: String = ""
)

data class DeliveryOrderItem(
    val name: String = "",
    val variantName: String? = null,
    val customizationNames: List<String> = emptyList(),
    val quantity: Int = 0,
    val unitPrice: Double = 0.0,
    val lineTotal: Double = 0.0
)
