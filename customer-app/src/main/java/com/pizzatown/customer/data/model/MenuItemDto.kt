package com.pizzatown.customer.data.model

import com.google.firebase.firestore.DocumentId
import com.pizzatown.customer.domain.model.*

data class MenuVariantDto(
    val id: String = "", val name: String = "", val price: Double = 0.0, val available: Boolean = true
)
data class CustomizationOptionDto(
    val id: String = "", val name: String = "", val priceAdjustment: Double = 0.0, val available: Boolean = true
)
data class CustomizationGroupDto(
    val id: String = "", val name: String = "", val selectionType: String = "SINGLE",
    val required: Boolean = false, val minSelections: Int = 0, val maxSelections: Int = 1,
    val options: List<CustomizationOptionDto> = emptyList()
)
data class MenuItemDto(
    @DocumentId val id: String = "",
    val name: String = "", val description: String = "", val categoryId: String = "",
    val imageUrl: String = "", val pricingMode: String = "FIXED", val basePrice: Double = 0.0,
    val available: Boolean = true,
    // Optional, admin-settable merchandising fields. Absent in older/existing
    // Firestore documents -> default to null/false below, so nothing renders
    // until an admin actually sets them (no fabricated ratings/badges).
    val rating: Double? = null,
    val reviewCount: Int? = null,
    val isBestseller: Boolean = false,
    val variants: List<MenuVariantDto> = emptyList(),
    val customizationGroups: List<CustomizationGroupDto> = emptyList()
)

fun MenuItemDto.toDomain() = MenuItem(
    id = id, name = name, description = description, categoryId = categoryId, imageUrl = imageUrl,
    pricingMode = runCatching { PricingMode.valueOf(pricingMode) }.getOrDefault(PricingMode.FIXED),
    basePrice = basePrice, available = available,
    rating = rating, reviewCount = reviewCount, isBestseller = isBestseller,
    variants = variants.map { MenuVariant(it.id, it.name, it.price, it.available) },
    customizationGroups = customizationGroups.map { group ->
        CustomizationGroup(
            id = group.id, name = group.name,
            selectionType = runCatching { SelectionType.valueOf(group.selectionType) }.getOrDefault(SelectionType.SINGLE),
            required = group.required, minSelections = group.minSelections, maxSelections = group.maxSelections,
            options = group.options.map { CustomizationOption(it.id, it.name, it.priceAdjustment, it.available) }
        )
    }
)
