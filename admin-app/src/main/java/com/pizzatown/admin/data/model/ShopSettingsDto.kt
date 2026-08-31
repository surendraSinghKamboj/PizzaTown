package com.pizzatown.admin.data.model

import com.pizzatown.admin.domain.model.DeliveryArea
import com.pizzatown.admin.domain.model.DeliveryPricing
import com.pizzatown.admin.domain.model.RestaurantStatus

data class RestaurantStatusDto(
    val isOpen: Boolean = false,
    val updatedAt: Long = 0L
)

fun RestaurantStatusDto.toDomain() = RestaurantStatus(isOpen, updatedAt)
fun RestaurantStatus.toDto() = RestaurantStatusDto(isOpen, updatedAt)

data class DeliveryAreaDto(
    val centerLat: Double = 0.0,
    val centerLng: Double = 0.0,
    val radiusKm: Double = 5.0,
    val updatedAt: Long = 0L
)

fun DeliveryAreaDto.toDomain() = DeliveryArea(centerLat, centerLng, radiusKm, updatedAt)
fun DeliveryArea.toDto() = DeliveryAreaDto(centerLat, centerLng, radiusKm, updatedAt)


data class DeliveryPricingDto(
    val minimumOrderValue: Double = 0.0,
    val deliveryCharge: Double = 0.0,
    val freeDeliveryAbove: Double = 0.0,
    val updatedAt: Long = 0L
)

fun DeliveryPricingDto.toDomain() =
    DeliveryPricing(
        minimumOrderValue = minimumOrderValue,
        deliveryCharge = deliveryCharge,
        freeDeliveryAbove = freeDeliveryAbove,
        updatedAt = updatedAt
    )

fun DeliveryPricing.toDto() =
    DeliveryPricingDto(
        minimumOrderValue = minimumOrderValue,
        deliveryCharge = deliveryCharge,
        freeDeliveryAbove = freeDeliveryAbove,
        updatedAt = updatedAt
    )
