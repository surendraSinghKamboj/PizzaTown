package com.pizzatown.admin.data.repository

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.pizzatown.admin.core.firebase.FirestoreCollections
import com.pizzatown.admin.data.model.BroadcastDto
import com.pizzatown.admin.data.model.toDomain
import com.pizzatown.admin.data.model.toDto
import com.pizzatown.admin.domain.model.Broadcast
import com.pizzatown.admin.domain.repository.BroadcastRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class BroadcastRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : BroadcastRepository {

    private val collection get() = firestore.collection(FirestoreCollections.BROADCASTS)

    override fun observeBroadcasts(): Flow<List<Broadcast>> = callbackFlow {
        val registration = collection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(snapshot?.toObjects(BroadcastDto::class.java)?.map { it.toDomain() } ?: emptyList())
            }
        awaitClose { registration.remove() }
    }

    override suspend fun send(title: String, message: String, targetUserId: String?, targetCustomerName: String): Result<Unit> = runCatching {
        require(title.isNotBlank()) { "Title is required." }
        require(message.isNotBlank()) { "Message is required." }
        val docRef = collection.document()
        val broadcast = Broadcast(
            id = docRef.id,
            title = title.trim(),
            message = message.trim(),
            targetUserId = targetUserId,
            targetCustomerName = targetCustomerName,
            createdAt = System.currentTimeMillis()
        )
        docRef.set(broadcast.toDto()).await(); Unit
    }.onFailure { FirebaseCrashlytics.getInstance().recordException(it) }

    override suspend fun delete(id: String): Result<Unit> = runCatching {
        collection.document(id).delete().await()
    }
}
