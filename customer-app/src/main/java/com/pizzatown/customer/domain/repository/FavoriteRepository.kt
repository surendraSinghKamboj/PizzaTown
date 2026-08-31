package com.pizzatown.customer.domain.repository

import kotlinx.coroutines.flow.Flow

interface FavoriteRepository {
    fun observeFavoriteIds(): Flow<Set<String>>
    suspend fun toggleFavorite(menuItemId: String, currentlyFavorite: Boolean)
}
