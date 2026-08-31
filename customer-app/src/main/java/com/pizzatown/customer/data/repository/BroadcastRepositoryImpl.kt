package com.pizzatown.customer.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.pizzatown.customer.core.firebase.FirestoreCollections
import com.pizzatown.customer.data.model.BroadcastDto
import com.pizzatown.customer.data.model.toDomain
import com.pizzatown.customer.domain.model.Broadcast
import com.pizzatown.customer.domain.repository.BroadcastRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

class BroadcastRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : BroadcastRepository {

    override fun observeMyBroadcasts(): Flow<List<Broadcast>> = callbackFlow {
        val registration = firestore.collection(FirestoreCollections.BROADCASTS)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(snapshot?.toObjects(BroadcastDto::class.java)?.map { it.toDomain() } ?: emptyList())
            }
        awaitClose { registration.remove() }
    }
}
