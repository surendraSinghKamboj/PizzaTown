package com.pizzatown.customer.domain.repository

import com.pizzatown.customer.domain.model.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun observeCategories(): Flow<List<Category>>
}
