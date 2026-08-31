package com.pizzatown.customer.domain.repository

import com.pizzatown.customer.domain.model.Order
import kotlinx.coroutines.flow.Flow

interface OrderRepository {
    /** Saves the order to Firestore and returns it with a generated orderId. */
    suspend fun createOrder(order: Order): Result<Order>

    /** Live list of this user's own past orders, most recent first. */
    fun observeOrdersForUser(userId: String): Flow<List<Order>>
}
