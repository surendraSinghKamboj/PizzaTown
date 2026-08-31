package com.pizzatown.admin.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.pizzatown.admin.core.firebase.FirestoreCollections
import com.pizzatown.admin.data.model.CategoryDto
import com.pizzatown.admin.data.model.toDomain
import com.pizzatown.admin.data.model.toDto
import com.pizzatown.admin.domain.model.Category
import com.pizzatown.admin.domain.repository.CategoryRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class CategoryRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : CategoryRepository {

    private val collection get() = firestore.collection(FirestoreCollections.CATEGORIES)

    override fun observeCategories(): Flow<List<Category>> = callbackFlow {
        val registration = collection
            .orderBy("sortOrder")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val categories = snapshot?.toObjects(CategoryDto::class.java)?.map { it.toDomain() } ?: emptyList()
                trySend(categories)
            }
        awaitClose { registration.remove() }
    }

    override suspend fun getCategories(): Result<List<Category>> = runCatching {
        collection.orderBy("sortOrder").get().await()
            .toObjects(CategoryDto::class.java)
            .map { it.toDomain() }
    }

    override suspend fun addCategory(category: Category): Result<String> = runCatching {
        val now = System.currentTimeMillis()
        val docRef = collection.document()
        val dto = category.copy(id = docRef.id, createdAt = now, updatedAt = now).toDto()
        docRef.set(dto).await()
        docRef.id
    }

    override suspend fun updateCategory(category: Category): Result<Unit> = runCatching {
        require(category.id.isNotBlank()) { "Category id is required for update" }
        val dto = category.copy(updatedAt = System.currentTimeMillis()).toDto()
        collection.document(category.id).set(dto).await()
    }

    override suspend fun deleteCategory(categoryId: String): Result<Unit> = runCatching {
        collection.document(categoryId).delete().await()
    }

    override suspend fun setEnabled(categoryId: String, enabled: Boolean): Result<Unit> = runCatching {
        collection.document(categoryId)
            .update(mapOf("enabled" to enabled, "updatedAt" to System.currentTimeMillis()))
            .await()
    }
}
