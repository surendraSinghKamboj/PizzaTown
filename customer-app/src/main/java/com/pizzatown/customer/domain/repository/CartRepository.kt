package com.pizzatown.customer.domain.repository

import com.pizzatown.customer.domain.model.CartItem
import kotlinx.coroutines.flow.Flow

interface CartRepository {
    fun observeCart(): Flow<List<CartItem>>
    suspend fun addOrIncrement(item: CartItem)
    suspend fun updateQuantity(cartItemId: String, quantity: Int)
    suspend fun removeItem(cartItemId: String)
    suspend fun clearCart()
}
