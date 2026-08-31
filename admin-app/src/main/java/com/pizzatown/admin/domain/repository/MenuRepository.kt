package com.pizzatown.admin.domain.repository

import com.pizzatown.admin.domain.model.MenuItem
import kotlinx.coroutines.flow.Flow

interface MenuRepository {
    fun observeMenuItems(): Flow<List<MenuItem>>
    suspend fun getMenuItem(id: String): Result<MenuItem>
    suspend fun addMenuItem(item: MenuItem): Result<String>
    suspend fun updateMenuItem(item: MenuItem): Result<Unit>
    suspend fun deleteMenuItem(id: String): Result<Unit>
    suspend fun setAvailable(id: String, available: Boolean): Result<Unit>

    /** Uploads to Firebase Storage and returns the download URL. */
    suspend fun uploadMenuItemImage(itemId: String, imageBytes: ByteArray): Result<String>
}
