package com.pizzatown.customer.domain.model

/** One chosen option inside one chosen customization group, snapshotted for the cart line. */
data class SelectedOption(
    val groupId: String,
    val groupName: String,
    val optionId: String,
    val optionName: String,
    val priceAdjustment: Double
)

/**
 * A single cart line. Two lines for the same product with DIFFERENT
 * variant/customizations must never merge — see [configKey].
 */
data class CartItem(
    val cartItemId: String = "",
    val menuItemId: String,
    val menuItemName: String,
    val imageUrl: String,
    val selectedVariantId: String? = null,
    val selectedVariantName: String? = null,
    val basePrice: Double,          // variant price if VARIANTS, else fixed basePrice
    val selectedOptions: List<SelectedOption> = emptyList(),
    val quantity: Int = 1
) {
    val customizationTotal: Double get() = selectedOptions.sumOf { it.priceAdjustment }
    val finalUnitPrice: Double get() = basePrice + customizationTotal
    val lineTotal: Double get() = finalUnitPrice * quantity

    companion object {
        /**
         * Deterministic identity for a cart line based on product + exact
         * configuration. Same product + same variant + same option set =
         * same key = same cart line (quantity increments). Any difference
         * in variant or selected options produces a different key, i.e. a
         * separate cart line — this is what requirement #23 in the spec
         * (different customizations = different cart lines) depends on.
         */
        fun configKey(
            menuItemId: String,
            variantId: String?,
            selectedOptionIds: List<String>
        ): String {
            val sortedOptions = selectedOptionIds.sorted().joinToString(",")
            return "$menuItemId|${variantId.orEmpty()}|$sortedOptions"
        }
    }
}
