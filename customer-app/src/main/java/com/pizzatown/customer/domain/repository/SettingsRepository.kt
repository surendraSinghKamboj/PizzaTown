package com.pizzatown.customer.domain.repository

import com.pizzatown.customer.domain.model.DeliveryArea
import com.pizzatown.customer.domain.model.DeliveryPricing
import com.pizzatown.customer.domain.model.RestaurantStatus
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun observeRestaurantStatus(): Flow<RestaurantStatus>
    fun observeDeliveryArea(): Flow<DeliveryArea>
    fun observeDeliveryPricing(): Flow<DeliveryPricing>
}
