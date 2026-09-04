package com.pizzatown.customer.domain.model

enum class OrderStatus {
    BEING_PAYMENT,
    PENDING,
    CONFIRMED,
    PREPARING,
    READY,
    ON_THE_WAY,
    DELIVERED,
    COMPLETED, // legacy/historical status
    CANCELLED
}

enum class PaymentMethod { ONLINE, COD }

enum class PaymentStatus { PENDING, PAID, FAILED, NOT_REQUIRED, CANCELLED }

data class OrderCustomer(
    val name: String = "",
    val phone: String = "",
    val address: String = ""
)

/** Snapshot of one ordered line — preserved forever, independent of later menu edits. */
data class OrderLineItem(
    val menuItemId: String,
    val name: String,
    val variantName: String?,
    val customizationNames: List<String>,
    val quantity: Int,
    val unitPrice: Double,
    val lineTotal: Double
)

data class Order(
    val orderId: String = "",
    val userId: String = "",
    val customer: OrderCustomer = OrderCustomer(),
    val items: List<OrderLineItem> = emptyList(),
    val subtotal: Double = 0.0,
    val discount: Double = 0.0,
    val couponCode: String = "",
    val deliveryFee: Double = 0.0,
    val tax: Double = 0.0,
    val grandTotal: Double = 0.0,
    val totalItems: Int = 0,
    val specialInstructions: String = "",
    val status: OrderStatus = OrderStatus.PENDING,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val deliveryLat: Double = 0.0,
    val deliveryLng: Double = 0.0,
    val deliveredById: String = "",
    val deliveredByName: String = "",
    val paymentMethod: PaymentMethod = PaymentMethod.COD,
    val paymentStatus: PaymentStatus = PaymentStatus.NOT_REQUIRED,
    val cashfreeOrderId: String = "",
    val cashfreePaymentId: String = ""
)
