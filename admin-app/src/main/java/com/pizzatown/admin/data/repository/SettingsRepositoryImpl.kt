package com.pizzatown.admin.data.repository

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.pizzatown.admin.core.firebase.FirestoreCollections
import com.pizzatown.admin.data.model.DeliveryAreaDto
import com.pizzatown.admin.data.model.DeliveryPricingDto
import com.pizzatown.admin.data.model.RestaurantStatusDto
import com.pizzatown.admin.data.model.toDomain
import com.pizzatown.admin.data.model.toDto
import com.pizzatown.admin.domain.model.DeliveryArea
import com.pizzatown.admin.domain.model.DeliveryPricing
import com.pizzatown.admin.domain.model.RestaurantStatus
import com.pizzatown.admin.domain.repository.SettingsRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class SettingsRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : SettingsRepository {

    private val settingsCollection get() = firestore.collection(FirestoreCollections.SETTINGS)

    override fun observeRestaurantStatus(): Flow<RestaurantStatus> = callbackFlow {
        val registration = settingsCollection.document(FirestoreCollections.RESTAURANT_STATUS_DOC)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                // No document yet = shop has never been explicitly opened —
                // default to closed rather than silently accepting orders.
                val isOpen = snapshot?.getBoolean("open") ?: false
                val updatedAt = snapshot?.getLong("updatedAt") ?: 0L
                trySend(RestaurantStatus(isOpen = isOpen, updatedAt = updatedAt))
            }
        awaitClose { registration.remove() }
    }

    override suspend fun setRestaurantOpen(isOpen: Boolean): Result<Unit> = runCatching {
        val status = RestaurantStatus(isOpen = isOpen, updatedAt = System.currentTimeMillis())
        settingsCollection.document(FirestoreCollections.RESTAURANT_STATUS_DOC).set(status.toDto()).await(); Unit
    }.onFailure { FirebaseCrashlytics.getInstance().recordException(it) }

    override fun observeDeliveryArea(): Flow<DeliveryArea> = callbackFlow {
        val registration = settingsCollection.document(FirestoreCollections.DELIVERY_AREA_DOC)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(snapshot?.toObject(DeliveryAreaDto::class.java)?.toDomain() ?: DeliveryArea())
            }
        awaitClose { registration.remove() }
    }

    override suspend fun updateDeliveryArea(area: DeliveryArea): Result<Unit> = runCatching {
        val toSave = area.copy(updatedAt = System.currentTimeMillis())
        settingsCollection.document(FirestoreCollections.DELIVERY_AREA_DOC).set(toSave.toDto()).await(); Unit
    }.onFailure { FirebaseCrashlytics.getInstance().recordException(it) }


    override fun observeDeliveryPricing(): Flow<DeliveryPricing> = callbackFlow {
        val registration = settingsCollection
            .document(FirestoreCollections.DELIVERY_PRICING_DOC)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                trySend(
                    snapshot?.toObject(DeliveryPricingDto::class.java)?.toDomain()
                        ?: DeliveryPricing()
                )
            }

        awaitClose { registration.remove() }
    }

    override suspend fun updateDeliveryPricing(
        pricing: DeliveryPricing
    ): Result<Unit> = runCatching {
        val toSave = pricing.copy(updatedAt = System.currentTimeMillis())

        settingsCollection
            .document(FirestoreCollections.DELIVERY_PRICING_DOC)
            .set(toSave.toDto())
            .await()

        Unit
    }.onFailure {
        FirebaseCrashlytics.getInstance().recordException(it)
    }
}
