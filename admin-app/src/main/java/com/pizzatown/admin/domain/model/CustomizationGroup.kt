package com.pizzatown.admin.domain.model

enum class SelectionType {
    SINGLE,
    MULTIPLE
}

/**
 * A generic customization group attached to a [MenuItem].
 * e.g. "Add-ons" (MULTIPLE, optional, max 5)
 * e.g. "Choose Size" (SINGLE, required)
 *
 * This is intentionally generic so it can represent pizza toppings,
 * burger add-ons, drink sizes, mocktail extras, etc. without any
 * food-specific modeling.
 */
data class CustomizationGroup(
    val id: String = "",
    val name: String = "",
    val selectionType: SelectionType = SelectionType.SINGLE,
    val required: Boolean = false,
    val minSelections: Int = 0,
    val maxSelections: Int = 1,
    val options: List<CustomizationOption> = emptyList()
) {
    /** Basic self-consistency check used by the admin editor before saving. */
    fun isValid(): Boolean {
        if (name.isBlank()) return false
        if (options.isEmpty()) return false
        if (minSelections < 0) return false
        if (maxSelections < minSelections) return false
        if (selectionType == SelectionType.SINGLE && maxSelections > 1) return false
        if (required && minSelections < 1) return false
        return true
    }
}
