package com.pizzatown.customer.domain.model

enum class SelectionType { SINGLE, MULTIPLE }

/** Generic customization group — same shape the admin app writes. */
data class CustomizationGroup(
    val id: String = "",
    val name: String = "",
    val selectionType: SelectionType = SelectionType.SINGLE,
    val required: Boolean = false,
    val minSelections: Int = 0,
    val maxSelections: Int = 1,
    val options: List<CustomizationOption> = emptyList()
)
