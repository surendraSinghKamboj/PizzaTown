package com.pizzatown.admin.presentation.menu

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pizzatown.admin.core.navigation.AdminDestinations
import com.pizzatown.admin.domain.model.*
import com.pizzatown.admin.domain.repository.CategoryRepository
import com.pizzatown.admin.domain.repository.MenuRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

// Local draft models carry a stable UI key so Compose lists can be
// edited (add/remove/reorder) before anything is persisted.
data class VariantDraft(
    val uiKey: String = UUID.randomUUID().toString(),
    val id: String = "",
    val name: String = "",
    val price: String = "",
    val available: Boolean = true
)

data class OptionDraft(
    val uiKey: String = UUID.randomUUID().toString(),
    val id: String = "",
    val name: String = "",
    val priceAdjustment: String = "0",
    val available: Boolean = true
)

data class GroupDraft(
    val uiKey: String = UUID.randomUUID().toString(),
    val id: String = "",
    val name: String = "",
    val selectionType: SelectionType = SelectionType.SINGLE,
    val required: Boolean = false,
    val minSelections: String = "0",
    val maxSelections: String = "1",
    val options: List<OptionDraft> = emptyList()
)

data class MenuEditorUiState(
    val isNew: Boolean = true,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val saveSuccess: Boolean = false,

    val name: String = "",
    val description: String = "",
    val categoryId: String = "",
    val categories: List<Category> = emptyList(),
    val imageUrl: String = "",
    val pendingImageBytes: ByteArray? = null,

    val pricingMode: PricingMode = PricingMode.FIXED,
    val basePrice: String = "",
    val variants: List<VariantDraft> = emptyList(),
    val customizationGroups: List<GroupDraft> = emptyList(),
    val available: Boolean = true
)

@HiltViewModel
class MenuEditorViewModel @Inject constructor(
    private val menuRepository: MenuRepository,
    private val categoryRepository: CategoryRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val itemId: String? = savedStateHandle.get<String>(AdminDestinations.MENU_EDITOR_ARG_ID)
        ?.takeIf { it != "new" }

    private val _uiState = MutableStateFlow(MenuEditorUiState(isNew = itemId == null, isLoading = itemId != null))
    val uiState: StateFlow<MenuEditorUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            categoryRepository.getCategories().onSuccess { categories ->
                _uiState.value = _uiState.value.copy(
                    categories = categories,
                    categoryId = _uiState.value.categoryId.ifBlank { categories.firstOrNull()?.id ?: "" }
                )
            }
        }
        if (itemId != null) {
            viewModelScope.launch {
                menuRepository.getMenuItem(itemId)
                    .onSuccess { item -> _uiState.value = fromDomain(item) }
                    .onFailure {
                        _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = it.message)
                    }
            }
        }
    }

    private fun fromDomain(item: MenuItem) = MenuEditorUiState(
        isNew = false,
        isLoading = false,
        name = item.name,
        description = item.description,
        categoryId = item.categoryId,
        categories = _uiState.value.categories,
        imageUrl = item.imageUrl,
        pricingMode = item.pricingMode,
        basePrice = if (item.basePrice > 0) item.basePrice.toString() else "",
        variants = item.variants.map { VariantDraft(id = it.id, name = it.name, price = it.price.toString(), available = it.available) },
        customizationGroups = item.customizationGroups.map { group ->
            GroupDraft(
                id = group.id,
                name = group.name,
                selectionType = group.selectionType,
                required = group.required,
                minSelections = group.minSelections.toString(),
                maxSelections = group.maxSelections.toString(),
                options = group.options.map { OptionDraft(id = it.id, name = it.name, priceAdjustment = it.priceAdjustment.toString(), available = it.available) }
            )
        },
        available = item.available
    )

    fun onNameChange(v: String) { _uiState.value = _uiState.value.copy(name = v, errorMessage = null) }
    fun onDescriptionChange(v: String) { _uiState.value = _uiState.value.copy(description = v) }
    fun onCategoryChange(v: String) { _uiState.value = _uiState.value.copy(categoryId = v) }
    fun onAvailableChange(v: Boolean) { _uiState.value = _uiState.value.copy(available = v) }
    fun onPendingImage(bytes: ByteArray) { _uiState.value = _uiState.value.copy(pendingImageBytes = bytes) }

    fun onPricingModeChange(mode: PricingMode) {
        _uiState.value = _uiState.value.copy(pricingMode = mode)
    }

    fun onBasePriceChange(v: String) { _uiState.value = _uiState.value.copy(basePrice = v, errorMessage = null) }

    // ---- Variants ----
    fun addVariant() {
        _uiState.value = _uiState.value.copy(variants = _uiState.value.variants + VariantDraft())
    }
    fun updateVariant(uiKey: String, name: String? = null, price: String? = null, available: Boolean? = null) {
        _uiState.value = _uiState.value.copy(
            variants = _uiState.value.variants.map {
                if (it.uiKey == uiKey) it.copy(
                    name = name ?: it.name,
                    price = price ?: it.price,
                    available = available ?: it.available
                ) else it
            }
        )
    }
    fun removeVariant(uiKey: String) {
        _uiState.value = _uiState.value.copy(variants = _uiState.value.variants.filterNot { it.uiKey == uiKey })
    }

    // ---- Customization groups ----
    fun addGroup() {
        _uiState.value = _uiState.value.copy(customizationGroups = _uiState.value.customizationGroups + GroupDraft())
    }
    fun updateGroup(uiKey: String, transform: (GroupDraft) -> GroupDraft) {
        _uiState.value = _uiState.value.copy(
            customizationGroups = _uiState.value.customizationGroups.map { if (it.uiKey == uiKey) transform(it) else it }
        )
    }
    fun removeGroup(uiKey: String) {
        _uiState.value = _uiState.value.copy(
            customizationGroups = _uiState.value.customizationGroups.filterNot { it.uiKey == uiKey }
        )
    }
    fun addOption(groupKey: String) {
        updateGroup(groupKey) { it.copy(options = it.options + OptionDraft()) }
    }
    fun updateOption(groupKey: String, optionKey: String, name: String? = null, price: String? = null, available: Boolean? = null) {
        updateGroup(groupKey) { group ->
            group.copy(options = group.options.map {
                if (it.uiKey == optionKey) it.copy(
                    name = name ?: it.name,
                    priceAdjustment = price ?: it.priceAdjustment,
                    available = available ?: it.available
                ) else it
            })
        }
    }
    fun removeOption(groupKey: String, optionKey: String) {
        updateGroup(groupKey) { group -> group.copy(options = group.options.filterNot { it.uiKey == optionKey }) }
    }

    fun save() {
        val state = _uiState.value
        val validationError = validate(state)
        if (validationError != null) {
            _uiState.value = state.copy(errorMessage = validationError)
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isSaving = true, errorMessage = null)

            var imageUrl = state.imageUrl
            val provisionalId = itemId ?: UUID.randomUUID().toString()

            if (state.pendingImageBytes != null) {
                menuRepository.uploadMenuItemImage(provisionalId, state.pendingImageBytes)
                    .onSuccess { imageUrl = it }
                    .onFailure {
                        _uiState.value = _uiState.value.copy(isSaving = false, errorMessage = "Image upload failed: ${it.message}")
                        return@launch
                    }
            }

            val domainItem = toDomain(state, itemId, imageUrl)
            val result = if (itemId == null) {
                menuRepository.addMenuItem(domainItem).map { }
            } else {
                menuRepository.updateMenuItem(domainItem)
            }

            _uiState.value = result.fold(
                onSuccess = { _uiState.value.copy(isSaving = false, saveSuccess = true) },
                onFailure = { _uiState.value.copy(isSaving = false, errorMessage = it.message ?: "Failed to save item") }
            )
        }
    }

    private fun toDomain(state: MenuEditorUiState, id: String?, imageUrl: String): MenuItem {
        return MenuItem(
            id = id ?: "",
            name = state.name.trim(),
            description = state.description.trim(),
            categoryId = state.categoryId,
            imageUrl = imageUrl,
            pricingMode = state.pricingMode,
            basePrice = if (state.pricingMode == PricingMode.FIXED) state.basePrice.toDoubleOrNull() ?: 0.0 else 0.0,
            available = state.available,
            variants = if (state.pricingMode == PricingMode.VARIANTS) {
                state.variants.map { MenuVariant(it.id.ifBlank { UUID.randomUUID().toString() }, it.name.trim(), it.price.toDoubleOrNull() ?: 0.0, it.available) }
            } else emptyList(),
            customizationGroups = state.customizationGroups.map { group ->
                CustomizationGroup(
                    id = group.id.ifBlank { UUID.randomUUID().toString() },
                    name = group.name.trim(),
                    selectionType = group.selectionType,
                    required = group.required,
                    minSelections = group.minSelections.toIntOrNull() ?: 0,
                    maxSelections = group.maxSelections.toIntOrNull() ?: 1,
                    options = group.options.map {
                        CustomizationOption(
                            it.id.ifBlank { UUID.randomUUID().toString() },
                            it.name.trim(),
                            it.priceAdjustment.toDoubleOrNull() ?: 0.0,
                            it.available
                        )
                    }
                )
            }
        )
    }

    private fun validate(state: MenuEditorUiState): String? {
        if (state.name.isBlank()) return "Item name is required."
        if (state.categoryId.isBlank()) return "Please select a category."
        if (state.pricingMode == PricingMode.FIXED) {
            val price = state.basePrice.toDoubleOrNull()
            if (price == null || price <= 0.0) return "Enter a valid price greater than 0."
        } else {
            if (state.variants.isEmpty()) return "Add at least one variant, or switch to Fixed Price."
            for (v in state.variants) {
                if (v.name.isBlank()) return "Every variant needs a name (e.g. Regular, Medium, Large)."
                val price = v.price.toDoubleOrNull()
                if (price == null || price <= 0.0) return "Every variant needs a valid price."
            }
        }
        for (group in state.customizationGroups) {
            if (group.name.isBlank()) return "Every customization group needs a name."
            if (group.options.isEmpty()) return "\"${group.name}\" needs at least one option."
            val min = group.minSelections.toIntOrNull() ?: -1
            val max = group.maxSelections.toIntOrNull() ?: -1
            if (min < 0 || max < 0) return "Min/Max selections must be valid numbers."
            if (max < min) return "Max selections cannot be less than min selections in \"${group.name}\"."
            if (group.selectionType == SelectionType.SINGLE && max > 1) return "\"${group.name}\" is Single-select but Max is greater than 1."
            for (opt in group.options) {
                if (opt.name.isBlank()) return "Every option in \"${group.name}\" needs a name."
                if (opt.priceAdjustment.toDoubleOrNull() == null) return "Every option in \"${group.name}\" needs a valid price adjustment."
            }
        }
        return null
    }
}
