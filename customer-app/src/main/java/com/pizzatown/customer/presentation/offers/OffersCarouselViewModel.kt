package com.pizzatown.customer.presentation.offers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pizzatown.customer.domain.model.Offer
import com.pizzatown.customer.domain.repository.OfferRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class OffersCarouselViewModel @Inject constructor(
    offerRepository: OfferRepository
) : ViewModel() {
    val offers: StateFlow<List<Offer>> = offerRepository.observeActiveOffers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
