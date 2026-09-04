package com.pizzatown.customer.data.repository

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
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

    private fun readProfileFromSnapshot(
        userId: String,
        snapshot: com.google.firebase.firestore.DocumentSnapshot
    ): UserProfile {
        val data = snapshot.data ?: return UserProfileDto().toDomain(userId)

        val rawAddresses = data["addresses"] as? List<*> ?: emptyList<Any>()

        val addresses = rawAddresses.mapNotNull { raw ->
            val map = raw as? Map<*, *> ?: return@mapNotNull null

            val id = map["id"]?.toString().orEmpty()
            if (id.isBlank()) return@mapNotNull null

            com.pizzatown.customer.domain.model.Address(
                id = id,
                label = map["label"]?.toString().orEmpty(),
                fullAddress = map["fullAddress"]?.toString().orEmpty(),
                houseFlat = map["houseFlat"]?.toString().orEmpty(),
                areaStreet = map["areaStreet"]?.toString().orEmpty(),
                landmark = map["landmark"]?.toString().orEmpty(),
                city = map["city"]?.toString().orEmpty(),
                pincode = map["pincode"]?.toString().orEmpty(),
                latitude = (map["latitude"] as? Number)?.toDouble() ?: 0.0,
                longitude = (map["longitude"] as? Number)?.toDouble() ?: 0.0,
                isDefault = when {
                    map["isDefault"] is Boolean -> map["isDefault"] as Boolean
                    map["default"] is Boolean -> map["default"] as Boolean
                    else -> false
                }
            )
        }

        return UserProfile(
            userId = userId,
            fullName = data["fullName"]?.toString().orEmpty(),
            mobile = data["mobile"]?.toString().orEmpty(),
            email = data["email"]?.toString().orEmpty(),
            addresses = addresses,
            profileImageUrl = data["profileImageUrl"]?.toString().orEmpty(),
            dateOfBirth = (data["dateOfBirth"] as? Number)?.toLong() ?: 0L,
            anniversaryDate = (data["anniversaryDate"] as? Number)?.toLong() ?: 0L
        )
    }

    private fun addressToFirestoreMap(address: com.pizzatown.customer.domain.model.Address): Map<String, Any> =
        buildMap {
            put("id", address.id)
            put("label", address.label)
            put("fullAddress", address.fullAddress)
            put("houseFlat", address.houseFlat)
            put("areaStreet", address.areaStreet)
            put("landmark", address.landmark)
            put("city", address.city)
            put("pincode", address.pincode)
            put("latitude", address.latitude)
            put("longitude", address.longitude)
            put("isDefault", address.isDefault)
        }

    override suspend fun getProfile(userId: String): Result<UserProfile> = runCatching {
        val snapshot = collection.document(userId).get(Source.SERVER).await()
        readProfileFromSnapshot(userId, snapshot)
    }

    override suspend fun updateProfile(profile: UserProfile): Result<Unit> = runCatching {
        collection.document(profile.userId).set(profile.toDto()).await()
    }

    override suspend fun setDefaultAddress(
        userId: String,
        addressId: String
    ): Result<Unit> = runCatching {
        val ref = collection.document(userId)

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(ref)

            if (!snapshot.exists()) {
                throw IllegalStateException("Customer profile was not found.")
            }

            val currentProfile = readProfileFromSnapshot(userId, snapshot)

            if (currentProfile.addresses.none { it.id == addressId }) {
                throw IllegalArgumentException("Selected address was not found.")
            }

            val updatedAddresses = currentProfile.addresses.map { address ->
                addressToFirestoreMap(
                    address.copy(isDefault = address.id == addressId)
                )
            }

            transaction.update(
                ref,
                "addresses",
                updatedAddresses
            )
        }.await()
    }

    override suspend fun uploadProfileImage(userId: String, imageBytes: ByteArray): Result<String> =
        cloudinaryUploader.uploadImage(imageBytes, publicIdHint = userId)
}
