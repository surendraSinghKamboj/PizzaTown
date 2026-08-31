package com.pizzatown.customer.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.pizzatown.customer.core.firebase.FirestoreCollections
import com.pizzatown.customer.domain.model.DeliveryArea
import com.pizzatown.customer.domain.model.DeliveryPricing
import com.pizzatown.customer.domain.model.RestaurantStatus
import com.pizzatown.customer.domain.repository.SettingsRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

class SettingsRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : SettingsRepository {

    private val settingsCollection get() = firestore.collection(FirestoreCollections.SETTINGS)

    override fun observeRestaurantStatus(): Flow<RestaurantStatus> = callbackFlow {
        val registration = settingsCollection.document(FirestoreCollections.RESTAURANT_STATUS_DOC)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val isOpen = snapshot?.getBoolean("open") ?: false
                val updatedAt = snapshot?.getLong("updatedAt") ?: 0L
                trySend(RestaurantStatus(isOpen, updatedAt))
            }
        awaitClose { registration.remove() }
    }


    override fun observeDeliveryPricing(): Flow<DeliveryPricing> = callbackFlow {
        val registration = settingsCollection
            .document(FirestoreCollections.DELIVERY_PRICING_DOC)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                trySend(
                    DeliveryPricing(
                        minimumOrderValue =
                            snapshot?.getDouble("minimumOrderValue") ?: 0.0,
                        deliveryCharge =
                            snapshot?.getDouble("deliveryCharge") ?: 0.0,
                        freeDeliveryAbove =
                            snapshot?.getDouble("freeDeliveryAbove") ?: 0.0,
                        updatedAt =
                            snapshot?.getLong("updatedAt") ?: 0L
                    )
                )
            }

        awaitClose { registration.remove() }
    }

    override fun observeDeliveryArea(): Flow<DeliveryArea> = callbackFlow {
        val registration = settingsCollection.document(FirestoreCollections.DELIVERY_AREA_DOC)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(
                    DeliveryArea(
                        centerLat = snapshot?.getDouble("centerLat") ?: 0.0,
                        centerLng = snapshot?.getDouble("centerLng") ?: 0.0,
                        radiusKm = snapshot?.getDouble("radiusKm") ?: 5.0,
                        updatedAt = snapshot?.getLong("updatedAt") ?: 0L
                    )
                )
            }
        awaitClose { registration.remove() }
    }
}
