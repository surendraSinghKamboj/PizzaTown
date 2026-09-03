package com.pizzatown.delivery.presentation.home

enum class DeliveryAppearance {
    SYSTEM,
    LIGHT,
    DARK
}

data class DeliveryUiState(
    val selectedTab: String = "home",
    val appearance: DeliveryAppearance = DeliveryAppearance.SYSTEM
)
