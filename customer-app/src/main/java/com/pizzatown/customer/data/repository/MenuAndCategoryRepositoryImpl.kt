package com.pizzatown.customer.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.pizzatown.customer.core.firebase.FirestoreCollections
import com.pizzatown.customer.data.model.CategoryDto
import com.pizzatown.customer.data.model.MenuItemDto
import com.pizzatown.customer.data.model.toDomain
import com.pizzatown.customer.domain.model.Category
import com.pizzatown.customer.domain.model.MenuItem
import com.pizzatown.customer.domain.repository.CategoryRepository
import com.pizzatown.customer.domain.repository.MenuRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class MenuRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : MenuRepository {

    private val collection get() = firestore.collection(FirestoreCollections.MENU_ITEMS)

    override fun observeMenuItems(): Flow<List<MenuItem>> = callbackFlow {
        val registration = collection.addSnapshotListener { snapshot, error ->
            if (error != null) { close(error); return@addSnapshotListener }
            trySend(snapshot?.toObjects(MenuItemDto::class.java)?.map { it.toDomain() } ?: emptyList())
        }
        awaitClose { registration.remove() }
    }

    override suspend fun getMenuItem(id: String): Result<MenuItem> = runCatching {
        collection.document(id).get().await().toObject(MenuItemDto::class.java)?.toDomain()
            ?: throw NoSuchElementException("Item not found")
    }

    override suspend fun refreshMenuItem(id: String): Result<MenuItem> = getMenuItem(id)
}

class CategoryRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : CategoryRepository {

    override fun observeCategories(): Flow<List<Category>> = callbackFlow {
        val registration = firestore.collection(FirestoreCollections.CATEGORIES)
            .orderBy("sortOrder")
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val categories = snapshot?.toObjects(CategoryDto::class.java)
                    ?.map { it.toDomain() }
                    ?.filter { it.enabled }
                    ?: emptyList()
                trySend(categories)
            }
        awaitClose { registration.remove() }
    }
}
