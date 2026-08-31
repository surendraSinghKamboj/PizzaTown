package com.pizzatown.customer.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.pizzatown.customer.domain.model.CartItem
import com.pizzatown.customer.domain.model.SelectedOption
import org.json.JSONArray
import org.json.JSONObject

/**
 * Room entity for the local cart. Selected customizations are stored as
 * a small JSON array using Android's built-in org.json (no extra
 * serialization library / Gradle plugin required) so the whole
 * selected-configuration snapshot survives app restarts.
 */
@Entity(tableName = "cart_items")
data class CartItemEntity(
    @PrimaryKey val cartItemId: String,
    val menuItemId: String,
    val menuItemName: String,
    val imageUrl: String,
    val selectedVariantId: String?,
    val selectedVariantName: String?,
    val basePrice: Double,
    val selectedOptionsJson: String,
    val quantity: Int
)

private fun encodeOptions(options: List<SelectedOption>): String {
    val array = JSONArray()
    options.forEach { option ->
        val obj = JSONObject()
        obj.put("groupId", option.groupId)
        obj.put("groupName", option.groupName)
        obj.put("optionId", option.optionId)
        obj.put("optionName", option.optionName)
        obj.put("priceAdjustment", option.priceAdjustment)
        array.put(obj)
    }
    return array.toString()
}

private fun decodeOptions(raw: String): List<SelectedOption> {
    if (raw.isBlank()) return emptyList()
    val array = JSONArray(raw)
    return buildList {
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            add(
                SelectedOption(
                    groupId = obj.getString("groupId"),
                    groupName = obj.getString("groupName"),
                    optionId = obj.getString("optionId"),
                    optionName = obj.getString("optionName"),
                    priceAdjustment = obj.getDouble("priceAdjustment")
                )
            )
        }
    }
}

fun CartItemEntity.toDomain(): CartItem = CartItem(
    cartItemId = cartItemId,
    menuItemId = menuItemId,
    menuItemName = menuItemName,
    imageUrl = imageUrl,
    selectedVariantId = selectedVariantId,
    selectedVariantName = selectedVariantName,
    basePrice = basePrice,
    selectedOptions = decodeOptions(selectedOptionsJson),
    quantity = quantity
)

fun CartItem.toEntity(): CartItemEntity = CartItemEntity(
    cartItemId = cartItemId,
    menuItemId = menuItemId,
    menuItemName = menuItemName,
    imageUrl = imageUrl,
    selectedVariantId = selectedVariantId,
    selectedVariantName = selectedVariantName,
    basePrice = basePrice,
    selectedOptionsJson = encodeOptions(selectedOptions),
    quantity = quantity
)
