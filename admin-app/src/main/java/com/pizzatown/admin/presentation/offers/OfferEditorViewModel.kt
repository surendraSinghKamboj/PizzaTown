package com.pizzatown.admin.presentation.offers

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pizzatown.admin.domain.model.Offer
import com.pizzatown.admin.domain.repository.OfferRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class OfferEditorUiState(
    val isNew: Boolean = true,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val saveSuccess: Boolean = false,
    val title: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val pendingImageBytes: ByteArray? = null,
    val active: Boolean = true,
    val sortOrder: String = "0"
)

@HiltViewModel
class OfferEditorViewModel @Inject constructor(
    private val offerRepository: OfferRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val offerId: String? = savedStateHandle.get<String>("offerId")?.takeIf { it != "new" }

    private val _uiState = MutableStateFlow(OfferEditorUiState(isNew = offerId == null, isLoading = offerId != null))
    val uiState: StateFlow<OfferEditorUiState> = _uiState.asStateFlow()

    init {
        if (offerId != null) {
            viewModelScope.launch {
                offerRepository.getOffer(offerId)
                    .onSuccess { offer ->
                        _uiState.value = OfferEditorUiState(
                            isNew = false,
                            title = offer.title,
                            description = offer.description,
                            imageUrl = offer.imageUrl,
                            active = offer.active,
                            sortOrder = offer.sortOrder.toString()
                        )
                    }
                    .onFailure { _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = it.message) }
            }
        }
    }

    fun onTitleChange(v: String) { _uiState.value = _uiState.value.copy(title = v, errorMessage = null) }
    fun onDescriptionChange(v: String) { _uiState.value = _uiState.value.copy(description = v) }
    fun onActiveChange(v: Boolean) { _uiState.value = _uiState.value.copy(active = v) }
    fun onSortOrderChange(v: String) { _uiState.value = _uiState.value.copy(sortOrder = v) }
    fun onPendingImage(bytes: ByteArray) { _uiState.value = _uiState.value.copy(pendingImageBytes = bytes) }

    fun save() {
        val state = _uiState.value
        if (state.title.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Title is required.")
            return
        }
        if (state.imageUrl.isBlank() && state.pendingImageBytes == null) {
            _uiState.value = state.copy(errorMessage = "Please upload a banner image.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)

            var imageUrl = state.imageUrl
            val provisionalId = offerId ?: UUID.randomUUID().toString()
            if (state.pendingImageBytes != null) {
                offerRepository.uploadOfferImage(provisionalId, state.pendingImageBytes)
                    .onSuccess { imageUrl = it }
                    .onFailure {
                        _uiState.value = _uiState.value.copy(isSaving = false, errorMessage = "Image upload failed: ${it.message}")
                        return@launch
                    }
            }

            val offer = Offer(
                id = offerId ?: "",
                title = state.title.trim(),
                description = state.description.trim(),
                imageUrl = imageUrl,
                active = state.active,
                sortOrder = state.sortOrder.toIntOrNull() ?: 0
            )

            val result = if (offerId == null) offerRepository.addOffer(offer).map { } else offerRepository.updateOffer(offer)
            _uiState.value = result.fold(
                onSuccess = { _uiState.value.copy(isSaving = false, saveSuccess = true) },
                onFailure = { _uiState.value.copy(isSaving = false, errorMessage = it.message ?: "Failed to save offer") }
            )
        }
    }
}
