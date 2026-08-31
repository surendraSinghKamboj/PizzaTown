package com.pizzatown.admin.data.repository

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.pizzatown.admin.core.firebase.FirestoreCollections
import com.pizzatown.admin.data.model.OrderDto
import com.pizzatown.admin.data.model.toDomain
import com.pizzatown.admin.domain.model.Order
import com.pizzatown.admin.domain.model.OrderStatus
import com.pizzatown.admin.domain.model.PaymentStatus
import com.pizzatown.admin.domain.repository.OrderRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class OrderRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : OrderRepository {

    override fun observeOrders(): Flow<List<Order>> = callbackFlow {
        val registration = firestore.collection(FirestoreCollections.ORDERS)
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

    override suspend fun updateOrderStatus(orderId: String, status: OrderStatus): Result<Unit> = runCatching {
        firestore.collection(FirestoreCollections.ORDERS).document(orderId)
            .update(mapOf("status" to status.name, "updatedAt" to System.currentTimeMillis()))
            .await(); Unit
    }.onFailure { FirebaseCrashlytics.getInstance().recordException(it) }
}
