package com.pizzatown.customer.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.pizzatown.customer.core.firebase.FirestoreCollections
import com.pizzatown.customer.data.model.OfferDto
import com.pizzatown.customer.data.model.toDomain
import com.pizzatown.customer.domain.model.Offer
import com.pizzatown.customer.domain.repository.OfferRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

class OfferRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : OfferRepository {

    override fun observeActiveOffers(): Flow<List<Offer>> = callbackFlow {
        val registration = firestore.collection(FirestoreCollections.OFFERS)
            .orderBy("sortOrder")
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val offers = snapshot?.toObjects(OfferDto::class.java)
                    ?.filter { it.active }
                    ?.map { it.toDomain() }
                    ?: emptyList()
                trySend(offers)
            }
        awaitClose { registration.remove() }
    }
}
