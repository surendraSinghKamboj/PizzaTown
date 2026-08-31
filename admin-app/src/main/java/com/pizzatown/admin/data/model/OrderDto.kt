package com.pizzatown.admin.data.model

import com.google.firebase.firestore.DocumentId
import com.pizzatown.admin.domain.model.*

data class OrderCustomerDto(val name: String = "", val phone: String = "", val address: String = "")

data class OrderLineItemDto(
    val menuItemId: String = "", val name: String = "", val variantName: String? = null,
    val customizationNames: List<String> = emptyList(), val quantity: Int = 0,
    val unitPrice: Double = 0.0, val lineTotal: Double = 0.0
)

data class OrderDto(
    @DocumentId val orderId: String = "",
    val userId: String = "",
    val customer: OrderCustomerDto = OrderCustomerDto(),
    val items: List<OrderLineItemDto> = emptyList(),
    val subtotal: Double = 0.0, val discount: Double = 0.0, val couponCode: String = "", val deliveryFee: Double = 0.0,
    val tax: Double = 0.0, val grandTotal: Double = 0.0, val totalItems: Int = 0,
    val specialInstructions: String = "", val status: String = "PENDING",
    val createdAt: Long = 0L, val updatedAt: Long = 0L,
    val deliveryLat: Double = 0.0, val deliveryLng: Double = 0.0,
    val paymentMethod: String = "COD",
    val paymentStatus: String = "NOT_REQUIRED",
    val cashfreeOrderId: String = "",
    val cashfreePaymentId: String = ""
)

fun OrderDto.toDomain() = Order(
    orderId = orderId, userId = userId,
    customer = OrderCustomer(customer.name, customer.phone, customer.address),
    items = items.map { OrderLineItem(it.menuItemId, it.name, it.variantName, it.customizationNames, it.quantity, it.unitPrice, it.lineTotal) },
    subtotal = subtotal, discount = discount, couponCode = couponCode, deliveryFee = deliveryFee, tax = tax, grandTotal = grandTotal,
    totalItems = totalItems, specialInstructions = specialInstructions,
    status = runCatching { OrderStatus.valueOf(status) }.getOrDefault(OrderStatus.PENDING),
    createdAt = createdAt, updatedAt = updatedAt,
    deliveryLat = deliveryLat, deliveryLng = deliveryLng,
    paymentMethod = runCatching { PaymentMethod.valueOf(paymentMethod) }.getOrDefault(PaymentMethod.COD),
    paymentStatus = runCatching { PaymentStatus.valueOf(paymentStatus) }.getOrDefault(PaymentStatus.NOT_REQUIRED),
    cashfreeOrderId = cashfreeOrderId, cashfreePaymentId = cashfreePaymentId
)
