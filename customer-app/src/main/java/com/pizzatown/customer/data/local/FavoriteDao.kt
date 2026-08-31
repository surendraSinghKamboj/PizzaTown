package com.pizzatown.customer.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Favorites are device-local (Room), not synced to Firestore — there is
 * no server-side "favorites" concept in this app yet, so this is real,
 * working local functionality rather than a fabricated backend feature.
 * If a synced/cross-device favorites list is wanted later, this can be
 * swapped for a Firestore-backed implementation behind the same
 * FavoriteRepository interface without touching any screen.
 */
@Entity(tableName = "favorite_items")
data class FavoriteEntity(
    @PrimaryKey val menuItemId: String
)

@Dao
interface FavoriteDao {
    @Query("SELECT menuItemId FROM favorite_items")
    fun observeFavoriteIds(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(entity: FavoriteEntity)

    @Query("DELETE FROM favorite_items WHERE menuItemId = :menuItemId")
    suspend fun remove(menuItemId: String)
}
