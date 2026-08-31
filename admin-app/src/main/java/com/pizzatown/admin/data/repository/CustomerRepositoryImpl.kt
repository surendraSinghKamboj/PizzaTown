package com.pizzatown.admin.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.pizzatown.admin.core.firebase.FirestoreCollections
import com.pizzatown.admin.data.model.CustomerDto
import com.pizzatown.admin.data.model.toDomain
import com.pizzatown.admin.domain.model.Customer
import com.pizzatown.admin.domain.repository.CustomerRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

class CustomerRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : CustomerRepository {

    override fun observeCustomers(): Flow<List<Customer>> = callbackFlow {
        val registration = firestore.collection(FirestoreCollections.USERS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(snapshot?.toObjects(CustomerDto::class.java)?.map { it.toDomain() } ?: emptyList())
            }
        awaitClose { registration.remove() }
    }
}
