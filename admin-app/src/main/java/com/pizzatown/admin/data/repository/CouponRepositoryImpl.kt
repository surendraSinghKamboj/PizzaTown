package com.pizzatown.admin.data.repository

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.pizzatown.admin.core.firebase.FirestoreCollections
import com.pizzatown.admin.data.model.CouponDto
import com.pizzatown.admin.data.model.toDomain
import com.pizzatown.admin.data.model.toDto
import com.pizzatown.admin.domain.model.Coupon
import com.pizzatown.admin.domain.repository.CouponRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class CouponRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : CouponRepository {

    private val collection get() = firestore.collection(FirestoreCollections.COUPONS)

    override fun observeCoupons(): Flow<List<Coupon>> = callbackFlow {
        val registration = collection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(snapshot?.toObjects(CouponDto::class.java)?.map { it.toDomain() } ?: emptyList())
            }
        awaitClose { registration.remove() }
    }

    override suspend fun getCoupon(id: String): Result<Coupon> = runCatching {
        collection.document(id).get().await().toObject(CouponDto::class.java)?.toDomain()
            ?: throw NoSuchElementException("Coupon not found: $id")
    }

    override suspend fun addCoupon(coupon: Coupon): Result<String> = runCatching {
        require(coupon.isValid()) { "Coupon needs a code and a discount value." }
        val docRef = collection.document()
        val toSave = coupon.copy(id = docRef.id, code = coupon.code.uppercase().trim(), createdAt = System.currentTimeMillis())
        docRef.set(toSave.toDto()).await()
        docRef.id
    }.onFailure { FirebaseCrashlytics.getInstance().recordException(it) }

    override suspend fun updateCoupon(coupon: Coupon): Result<Unit> = runCatching {
        require(coupon.id.isNotBlank()) { "Coupon id is required for update" }
        require(coupon.isValid()) { "Coupon needs a code and a discount value." }
        collection.document(coupon.id).set(coupon.copy(code = coupon.code.uppercase().trim()).toDto()).await(); Unit
    }.onFailure { FirebaseCrashlytics.getInstance().recordException(it) }

    override suspend fun deleteCoupon(id: String): Result<Unit> = runCatching {
        collection.document(id).delete().await()
    }

    override suspend fun setActive(id: String, active: Boolean): Result<Unit> = runCatching {
        collection.document(id).update(mapOf("active" to active)).await()
    }
}
