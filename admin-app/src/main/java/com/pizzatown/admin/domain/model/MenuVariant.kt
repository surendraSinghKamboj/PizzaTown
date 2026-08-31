package com.pizzatown.admin.domain.model

/**
 * A priced variant of a [MenuItem].
 * e.g. Pizza -> Regular/Medium/Large, Burger -> Single/Double,
 * Cold Drink -> 250ml/500ml/750ml.
 *
 * Names are fully admin-defined; nothing here is hardcoded to "R/M/L"
 * or any specific food type.
 */
data class MenuVariant(
    val id: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val available: Boolean = true
)
