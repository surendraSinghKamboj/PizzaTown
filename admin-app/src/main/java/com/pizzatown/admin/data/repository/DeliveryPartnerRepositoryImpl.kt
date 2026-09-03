package com.pizzatown.admin.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.pizzatown.admin.domain.model.DeliveryPartner
import com.pizzatown.admin.domain.repository.DeliveryPartnerRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class DeliveryPartnerRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val functions: FirebaseFunctions
) : DeliveryPartnerRepository {

    override suspend fun createPartner(
        name: String,
        email: String,
        phone: String,
        password: String
    ) {
        functions
            .getHttpsCallable("createDeliveryPartner")
            .call(
                mapOf(
                    "name" to name.trim(),
                    "email" to email.trim(),
                    "phone" to phone.trim(),
                    "password" to password
                )
            )
            .await()
    }

    override suspend fun getPartners(): List<DeliveryPartner> {
        val snapshot = firestore
            .collection("users")
            .whereEqualTo("role", "delivery")
            .get()
            .await()

        return snapshot.documents.map { doc ->
            DeliveryPartner(
                id = doc.id,
                name = doc.getString("name").orEmpty(),
                email = doc.getString("email").orEmpty(),
                phone = doc.getString("phone").orEmpty(),
                active = doc.getBoolean("active") ?: true,
                createdAt = doc.getLong("createdAt") ?: 0L
            )
        }
    }

    override suspend fun updatePartner(
        uid: String,
        name: String,
        email: String,
        phone: String
    ) {
        functions
            .getHttpsCallable("updateDeliveryPartner")
            .call(
                mapOf(
                    "uid" to uid,
                    "name" to name.trim(),
                    "email" to email.trim(),
                    "phone" to phone.trim()
                )
            )
            .await()
    }

    override suspend fun resetPassword(
        uid: String,
        password: String
    ) {
        functions
            .getHttpsCallable("resetDeliveryPartnerPassword")
            .call(
                mapOf(
                    "uid" to uid,
                    "password" to password
                )
            )
            .await()
    }

    override suspend fun deletePartner(
        uid: String
    ) {
        functions
            .getHttpsCallable("deleteDeliveryPartner")
            .call(
                mapOf(
                    "uid" to uid
                )
            )
            .await()
    }

    override suspend fun setPartnerActive(
        uid: String,
        active: Boolean
    ) {
        functions
            .getHttpsCallable("setDeliveryPartnerActive")
            .call(
                mapOf(
                    "uid" to uid,
                    "active" to active
                )
            )
            .await()
    }
}
