package com.pizzatown.admin.domain.model

/**
 * A promotional banner shown in the customer app's home carousel.
 * Purely a marketing display — not tied to pricing logic (that's what
 * the coupon system is for).
 */
data class Offer(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val active: Boolean = true,
    val sortOrder: Int = 0,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
) {
    fun isValid(): Boolean = title.isNotBlank() && imageUrl.isNotBlank()
}
