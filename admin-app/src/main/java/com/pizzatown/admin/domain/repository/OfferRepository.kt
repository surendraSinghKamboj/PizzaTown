package com.pizzatown.admin.domain.repository

import com.pizzatown.admin.domain.model.Offer
import kotlinx.coroutines.flow.Flow

interface OfferRepository {
    fun observeOffers(): Flow<List<Offer>>
    suspend fun getOffer(id: String): Result<Offer>
    suspend fun addOffer(offer: Offer): Result<String>
    suspend fun updateOffer(offer: Offer): Result<Unit>
    suspend fun deleteOffer(id: String): Result<Unit>
    suspend fun setActive(id: String, active: Boolean): Result<Unit>
    suspend fun uploadOfferImage(offerId: String, imageBytes: ByteArray): Result<String>
}
