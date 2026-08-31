package com.pizzatown.admin.data.repository

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.pizzatown.admin.core.cloudinary.CloudinaryUploader
import com.pizzatown.admin.core.firebase.FirestoreCollections
import com.pizzatown.admin.data.model.MenuItemDto
import com.pizzatown.admin.data.model.toDomain
import com.pizzatown.admin.data.model.toDto
import com.pizzatown.admin.domain.model.MenuItem
import com.pizzatown.admin.domain.repository.MenuRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class MenuRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val cloudinaryUploader: CloudinaryUploader
) : MenuRepository {

    private val collection get() = firestore.collection(FirestoreCollections.MENU_ITEMS)

    override fun observeMenuItems(): Flow<List<MenuItem>> = callbackFlow {
        val registration = collection
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val items = snapshot?.toObjects(MenuItemDto::class.java)?.map { it.toDomain() } ?: emptyList()
                trySend(items)
            }
        awaitClose { registration.remove() }
    }

    override suspend fun getMenuItem(id: String): Result<MenuItem> = runCatching {
        val snapshot = collection.document(id).get().await()
        snapshot.toObject(MenuItemDto::class.java)?.toDomain()
            ?: throw NoSuchElementException("Menu item not found: $id")
    }

    override suspend fun addMenuItem(item: MenuItem): Result<String> = runCatching {
        require(item.isValid()) { "Menu item is missing required fields (name, category, pricing)" }
        val now = System.currentTimeMillis()
        val docRef = collection.document()
        val dto = item.copy(id = docRef.id, createdAt = now, updatedAt = now).toDto()
        docRef.set(dto).await()
        docRef.id
    }.onFailure { FirebaseCrashlytics.getInstance().recordException(it) }

    override suspend fun updateMenuItem(item: MenuItem): Result<Unit> = runCatching {
        require(item.id.isNotBlank()) { "Menu item id is required for update" }
        require(item.isValid()) { "Menu item is missing required fields (name, category, pricing)" }
        val dto = item.copy(updatedAt = System.currentTimeMillis()).toDto()
        collection.document(item.id).set(dto).await(); Unit
    }.onFailure { FirebaseCrashlytics.getInstance().recordException(it) }

    override suspend fun deleteMenuItem(id: String): Result<Unit> = runCatching {
        collection.document(id).delete().await()
    }

    override suspend fun setAvailable(id: String, available: Boolean): Result<Unit> = runCatching {
        collection.document(id)
            .update(mapOf("available" to available, "updatedAt" to System.currentTimeMillis()))
            .await()
    }

    override suspend fun uploadMenuItemImage(itemId: String, imageBytes: ByteArray): Result<String> =
        cloudinaryUploader.uploadImage(imageBytes, publicIdHint = itemId)
}
