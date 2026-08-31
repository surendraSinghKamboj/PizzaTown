package com.pizzatown.admin.domain.repository

import com.pizzatown.admin.domain.model.DeliveryArea
import com.pizzatown.admin.domain.model.DeliveryPricing
import com.pizzatown.admin.domain.model.RestaurantStatus
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun observeRestaurantStatus(): Flow<RestaurantStatus>
    suspend fun setRestaurantOpen(isOpen: Boolean): Result<Unit>

    fun observeDeliveryArea(): Flow<DeliveryArea>
    suspend fun updateDeliveryArea(area: DeliveryArea): Result<Unit>

    fun observeDeliveryPricing(): Flow<DeliveryPricing>
    suspend fun updateDeliveryPricing(pricing: DeliveryPricing): Result<Unit>
}
