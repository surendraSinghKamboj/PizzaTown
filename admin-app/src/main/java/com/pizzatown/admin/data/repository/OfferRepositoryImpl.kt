package com.pizzatown.admin.data.repository

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.pizzatown.admin.core.cloudinary.CloudinaryUploader
import com.pizzatown.admin.core.firebase.FirestoreCollections
import com.pizzatown.admin.data.model.OfferDto
import com.pizzatown.admin.data.model.toDomain
import com.pizzatown.admin.data.model.toDto
import com.pizzatown.admin.domain.model.Offer
import com.pizzatown.admin.domain.repository.OfferRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class OfferRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val cloudinaryUploader: CloudinaryUploader
) : OfferRepository {

    private val collection get() = firestore.collection(FirestoreCollections.OFFERS)

    override fun observeOffers(): Flow<List<Offer>> = callbackFlow {
        val registration = collection
            .orderBy("sortOrder")
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(snapshot?.toObjects(OfferDto::class.java)?.map { it.toDomain() } ?: emptyList())
            }
        awaitClose { registration.remove() }
    }

    override suspend fun getOffer(id: String): Result<Offer> = runCatching {
        collection.document(id).get().await().toObject(OfferDto::class.java)?.toDomain()
            ?: throw NoSuchElementException("Offer not found: $id")
    }

    override suspend fun addOffer(offer: Offer): Result<String> = runCatching {
        require(offer.isValid()) { "Offer needs a title and image." }
        val now = System.currentTimeMillis()
        val docRef = collection.document()
        docRef.set(offer.copy(id = docRef.id, createdAt = now, updatedAt = now).toDto()).await()
        docRef.id
    }.onFailure { FirebaseCrashlytics.getInstance().recordException(it) }

    override suspend fun updateOffer(offer: Offer): Result<Unit> = runCatching {
        require(offer.id.isNotBlank()) { "Offer id is required for update" }
        require(offer.isValid()) { "Offer needs a title and image." }
        collection.document(offer.id).set(offer.copy(updatedAt = System.currentTimeMillis()).toDto()).await(); Unit
    }.onFailure { FirebaseCrashlytics.getInstance().recordException(it) }

    override suspend fun deleteOffer(id: String): Result<Unit> = runCatching {
        collection.document(id).delete().await()
    }

    override suspend fun setActive(id: String, active: Boolean): Result<Unit> = runCatching {
        collection.document(id).update(mapOf("active" to active, "updatedAt" to System.currentTimeMillis())).await()
    }

    override suspend fun uploadOfferImage(offerId: String, imageBytes: ByteArray): Result<String> =
        cloudinaryUploader.uploadImage(imageBytes, publicIdHint = "offer_$offerId")
}
