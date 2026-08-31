package com.pizzatown.admin.domain.repository

import com.pizzatown.admin.domain.model.Order
import com.pizzatown.admin.domain.model.OrderStatus
import kotlinx.coroutines.flow.Flow

interface OrderRepository {
    /** All orders across all customers, most recent first. */
    fun observeOrders(): Flow<List<Order>>
    suspend fun updateOrderStatus(orderId: String, status: OrderStatus): Result<Unit>
}
