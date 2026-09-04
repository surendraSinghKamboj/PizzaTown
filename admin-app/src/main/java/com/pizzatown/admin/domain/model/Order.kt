package com.pizzatown.admin.domain.model

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
    // Captured from the customer's device at checkout time (mandatory
    // location permission) — lets admin see where an order is actually
    // coming from and audit that it was inside the delivery area.
    val deliveryLat: Double = 0.0,
    val deliveryLng: Double = 0.0,
    val deliveredById: String = "",
    val deliveredByName: String = "",
    val paymentMethod: PaymentMethod = PaymentMethod.COD,
    val paymentStatus: PaymentStatus = PaymentStatus.NOT_REQUIRED,
    val cashfreeOrderId: String = "",
    val cashfreePaymentId: String = ""
) {
    /** The status this order can move to next in the normal flow (null = terminal state). */
    fun nextStatus(): OrderStatus? = when (status) {
        OrderStatus.BEING_PAYMENT -> null
        OrderStatus.PENDING -> OrderStatus.CONFIRMED
        OrderStatus.CONFIRMED -> OrderStatus.PREPARING
        OrderStatus.PREPARING -> OrderStatus.READY
        // READY means the kitchen has handed the order to the delivery workflow.
        // Admin must not advance it to COMPLETED.
        OrderStatus.READY -> null
        OrderStatus.ON_THE_WAY -> null
        OrderStatus.DELIVERED -> null
        OrderStatus.COMPLETED -> null
        OrderStatus.CANCELLED -> null
    }

    fun canCancel(): Boolean =
        status != OrderStatus.READY &&
        status != OrderStatus.ON_THE_WAY &&
        status != OrderStatus.DELIVERED &&
        status != OrderStatus.COMPLETED &&
        status != OrderStatus.CANCELLED
}
