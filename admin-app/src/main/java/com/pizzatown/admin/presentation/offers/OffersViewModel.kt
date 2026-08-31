package com.pizzatown.admin.presentation.offers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pizzatown.admin.core.common.UiState
import com.pizzatown.admin.domain.model.Offer
import com.pizzatown.admin.domain.repository.OfferRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OffersViewModel @Inject constructor(
    private val offerRepository: OfferRepository
) : ViewModel() {

    val offersState: StateFlow<UiState<List<Offer>>> = offerRepository.observeOffers()
        .map { offers -> if (offers.isEmpty()) UiState.Empty else UiState.Success(offers) as UiState<List<Offer>> }
        .catch { emit(UiState.Error(it.message ?: "Failed to load offers")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    fun setActive(offer: Offer, active: Boolean) {
        viewModelScope.launch { offerRepository.setActive(offer.id, active) }
    }

    fun deleteOffer(offer: Offer) {
        viewModelScope.launch { offerRepository.deleteOffer(offer.id) }
    }
}
