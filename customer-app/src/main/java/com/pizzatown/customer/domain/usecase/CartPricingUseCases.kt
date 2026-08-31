package com.pizzatown.customer.domain.usecase

import com.pizzatown.customer.domain.model.CartItem
import javax.inject.Inject

data class CartTotals(
    val subtotal: Double,
    val discount: Double,
    val deliveryFee: Double,
    val tax: Double,
    val grandTotal: Double,
    val totalItemCount: Int
)

/**
 * Computes the price of a single cart line. This is the ONLY place unit
 * price / line total math happens for a cart item — the cart screen
 * and checkout both call this (or
 * read the already-computed CartItem properties) instead of
 * re-implementing the arithmetic, so totals can never drift apart.
 */
class CalculateCartItemPriceUseCase @Inject constructor() {
    operator fun invoke(item: CartItem): Double = item.lineTotal
}

/**
 * Computes the full order total from the cart. Discount / delivery fee
 * / tax default to zero (not implemented in v1) but are already part of
 * the formula so they can be turned on later without touching every
 * call site.
 */
class CalculateCartTotalUseCase @Inject constructor() {
    operator fun invoke(
        items: List<CartItem>,
        discount: Double = 0.0,
        deliveryFee: Double = 0.0,
        taxRate: Double = 0.0
    ): CartTotals {
        val subtotal = items.sumOf { it.lineTotal }
        val tax = subtotal * taxRate
        val grandTotal = (subtotal - discount + deliveryFee + tax).coerceAtLeast(0.0)
        return CartTotals(
            subtotal = subtotal,
            discount = discount,
            deliveryFee = deliveryFee,
            tax = tax,
            grandTotal = grandTotal,
            totalItemCount = items.sumOf { it.quantity }
        )
    }
}
