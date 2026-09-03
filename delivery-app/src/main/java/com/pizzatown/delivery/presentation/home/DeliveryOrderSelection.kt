package com.pizzatown.delivery.presentation.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object DeliveryOrderSelection {
    var selectedOrderId by mutableStateOf<String?>(null)
}
