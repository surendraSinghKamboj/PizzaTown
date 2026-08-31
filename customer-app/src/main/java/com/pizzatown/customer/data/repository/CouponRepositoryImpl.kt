package com.pizzatown.customer.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.pizzatown.customer.core.firebase.FirestoreCollections
import com.pizzatown.customer.data.model.CouponDto
import com.pizzatown.customer.data.model.toDomain
import com.pizzatown.customer.domain.model.Coupon
import com.pizzatown.customer.domain.repository.CouponRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class CouponRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : CouponRepository {

    private val collection get() = firestore.collection(FirestoreCollections.COUPONS)

    override suspend fun getCouponByCode(code: String): Result<Coupon?> = runCatching {
        val snapshot = collection.whereEqualTo("code", code.trim().uppercase()).limit(1).get().await()
        snapshot.documents.firstOrNull()?.toObject(CouponDto::class.java)?.toDomain()
    }

    override suspend fun incrementUsage(couponId: String): Result<Unit> = runCatching {
        // FieldValue.increment is an atomic server-side operation, so
        // concurrent redemptions from different customers can never
        // undercount each other's usage the way a read-then-write would.
        collection.document(couponId).update("usageCount", FieldValue.increment(1)).await()
    }
}
