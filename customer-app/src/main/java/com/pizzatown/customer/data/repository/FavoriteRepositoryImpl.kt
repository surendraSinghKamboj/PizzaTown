package com.pizzatown.customer.data.repository

import com.pizzatown.customer.data.local.FavoriteDao
import com.pizzatown.customer.data.local.FavoriteEntity
import com.pizzatown.customer.domain.repository.FavoriteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class FavoriteRepositoryImpl @Inject constructor(
    private val favoriteDao: FavoriteDao
) : FavoriteRepository {

    override fun observeFavoriteIds(): Flow<Set<String>> =
        favoriteDao.observeFavoriteIds().map { it.toSet() }

    override suspend fun toggleFavorite(menuItemId: String, currentlyFavorite: Boolean) {
        if (currentlyFavorite) favoriteDao.remove(menuItemId) else favoriteDao.add(FavoriteEntity(menuItemId))
    }
}
