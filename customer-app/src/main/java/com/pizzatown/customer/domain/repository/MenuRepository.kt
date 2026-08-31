package com.pizzatown.customer.domain.repository

import com.pizzatown.customer.domain.model.MenuItem
import kotlinx.coroutines.flow.Flow

interface MenuRepository {
    fun observeMenuItems(): Flow<List<MenuItem>>
    suspend fun getMenuItem(id: String): Result<MenuItem>
    /** Re-reads a specific item's current price/availability right before checkout. */
    suspend fun refreshMenuItem(id: String): Result<MenuItem>
}
