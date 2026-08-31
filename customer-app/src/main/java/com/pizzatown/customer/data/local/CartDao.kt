package com.pizzatown.customer.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CartDao {

    @Query("SELECT * FROM cart_items ORDER BY cartItemId")
    fun observeCartItems(): Flow<List<CartItemEntity>>

    @Query("SELECT * FROM cart_items WHERE cartItemId = :cartItemId LIMIT 1")
    suspend fun getByCartItemId(cartItemId: String): CartItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CartItemEntity)

    @Update
    suspend fun update(entity: CartItemEntity)

    @Query("DELETE FROM cart_items WHERE cartItemId = :cartItemId")
    suspend fun deleteByCartItemId(cartItemId: String)

    @Query("DELETE FROM cart_items")
    suspend fun clearCart()
}
