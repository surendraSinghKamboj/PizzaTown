package com.pizzatown.admin.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.pizzatown.admin.domain.model.*

/**
 * Firestore-compatible DTOs. Firestore requires a public no-arg
 * constructor for automatic deserialization, so every field has a
 * default value. These are mapped to/from the domain models so the
 * rest of the app (ViewModels, UI, use cases) never touches Firestore
 * annotations directly.
 */

data class MenuVariantDto(
    val id: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val available: Boolean = true
)

data class CustomizationOptionDto(
    val id: String = "",
    val name: String = "",
    val priceAdjustment: Double = 0.0,
    val available: Boolean = true
)

data class CustomizationGroupDto(
    val id: String = "",
    val name: String = "",
    val selectionType: String = "SINGLE",
    val required: Boolean = false,
    val minSelections: Int = 0,
    val maxSelections: Int = 1,
    val options: List<CustomizationOptionDto> = emptyList()
)

data class MenuItemDto(
    @DocumentId
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val categoryId: String = "",
    val imageUrl: String = "",
    @get:PropertyName("pricingMode") @set:PropertyName("pricingMode")
    var pricingMode: String = "FIXED",
    val basePrice: Double = 0.0,
    val available: Boolean = true,
    val variants: List<MenuVariantDto> = emptyList(),
    val customizationGroups: List<CustomizationGroupDto> = emptyList(),
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

fun MenuVariantDto.toDomain() = MenuVariant(id, name, price, available)
fun MenuVariant.toDto() = MenuVariantDto(id, name, price, available)

fun CustomizationOptionDto.toDomain() = CustomizationOption(id, name, priceAdjustment, available)
fun CustomizationOption.toDto() = CustomizationOptionDto(id, name, priceAdjustment, available)

fun CustomizationGroupDto.toDomain() = CustomizationGroup(
    id = id,
    name = name,
    selectionType = runCatching { SelectionType.valueOf(selectionType) }.getOrDefault(SelectionType.SINGLE),
    required = required,
    minSelections = minSelections,
    maxSelections = maxSelections,
    options = options.map { it.toDomain() }
)

fun CustomizationGroup.toDto() = CustomizationGroupDto(
    id = id,
    name = name,
    selectionType = selectionType.name,
    required = required,
    minSelections = minSelections,
    maxSelections = maxSelections,
    options = options.map { it.toDto() }
)

fun MenuItemDto.toDomain() = MenuItem(
    id = id,
    name = name,
    description = description,
    categoryId = categoryId,
    imageUrl = imageUrl,
    pricingMode = runCatching { PricingMode.valueOf(pricingMode) }.getOrDefault(PricingMode.FIXED),
    basePrice = basePrice,
    available = available,
    variants = variants.map { it.toDomain() },
    customizationGroups = customizationGroups.map { it.toDomain() },
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun MenuItem.toDto() = MenuItemDto(
    id = id,
    name = name,
    description = description,
    categoryId = categoryId,
    imageUrl = imageUrl,
    pricingMode = pricingMode.name,
    basePrice = basePrice,
    available = available,
    variants = variants.map { it.toDto() },
    customizationGroups = customizationGroups.map { it.toDto() },
    createdAt = createdAt,
    updatedAt = updatedAt
)
