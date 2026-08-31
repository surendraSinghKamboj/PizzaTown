package com.pizzatown.customer.domain.repository

import com.pizzatown.customer.domain.model.Offer
import kotlinx.coroutines.flow.Flow

interface OfferRepository {
    /** Only active offers, in display order. */
    fun observeActiveOffers(): Flow<List<Offer>>
}
