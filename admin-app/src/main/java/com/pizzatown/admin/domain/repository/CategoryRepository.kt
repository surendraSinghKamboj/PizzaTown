package com.pizzatown.admin.domain.repository

import com.pizzatown.admin.domain.model.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun observeCategories(): Flow<List<Category>>
    suspend fun getCategories(): Result<List<Category>>
    suspend fun addCategory(category: Category): Result<String>
    suspend fun updateCategory(category: Category): Result<Unit>
    suspend fun deleteCategory(categoryId: String): Result<Unit>
    suspend fun setEnabled(categoryId: String, enabled: Boolean): Result<Unit>
}
