package com.pizzatown.customer.data.repository

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.pizzatown.customer.core.cloudinary.CloudinaryUploader
import com.pizzatown.customer.core.firebase.FirestoreCollections
import com.pizzatown.customer.data.model.OrderDto
import com.pizzatown.customer.data.model.UserProfileDto
import com.pizzatown.customer.data.model.toDomain
import com.pizzatown.customer.data.model.toDto
import com.pizzatown.customer.domain.model.Order
import com.pizzatown.customer.domain.model.OrderStatus
import com.pizzatown.customer.domain.model.PaymentStatus
import com.pizzatown.customer.domain.model.UserProfile
import com.pizzatown.customer.domain.repository.OrderRepository
import com.pizzatown.customer.domain.repository.ProfileRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class OrderRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : OrderRepository {

    override suspend fun createOrder(order: Order): Result<Order> = runCatching {
        val docRef = firestore.collection(FirestoreCollections.ORDERS).document()
        val now = System.currentTimeMillis()
        val finalOrder = order.copy(orderId = docRef.id, createdAt = now, updatedAt = now)
        docRef.set(finalOrder.toDto()).await()
        finalOrder
    }.onFailure {
        // Order creation failing is a real money-losing bug in production —
        // record it as a non-fatal so it shows up in Crashlytics even
        // though the app itself recovers gracefully (shows a retry prompt).
        FirebaseCrashlytics.getInstance().recordException(it)
    }

    override fun observeOrdersForUser(userId: String): Flow<List<Order>> = callbackFlow {
        val registration = firestore.collection(FirestoreCollections.ORDERS)
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val orders = snapshot?.toObjects(OrderDto::class.java)
    ?.map { it.toDomain() }
    ?.filter { it.status != OrderStatus.BEING_PAYMENT }
    ?: emptyList()
                trySend(orders)
            }
        awaitClose { registration.remove() }
    }
}

class ProfileRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val cloudinaryUploader: CloudinaryUploader
) : ProfileRepository {

    private val collection get() = firestore.collection(FirestoreCollections.USERS)

    override suspend fun getProfile(userId: String): Result<UserProfile> = runCatching {
        val dto = collection.document(userId).get().await().toObject(UserProfileDto::class.java)
            ?: UserProfileDto()
        dto.toDomain(userId)
    }

    override suspend fun updateProfile(profile: UserProfile): Result<Unit> = runCatching {
        collection.document(profile.userId).set(profile.toDto()).await()
    }

    override suspend fun uploadProfileImage(userId: String, imageBytes: ByteArray): Result<String> =
        cloudinaryUploader.uploadImage(imageBytes, publicIdHint = userId)
}
