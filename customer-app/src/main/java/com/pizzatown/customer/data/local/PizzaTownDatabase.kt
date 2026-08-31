package com.pizzatown.customer.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [CartItemEntity::class, FavoriteEntity::class], version = 2, exportSchema = false)
abstract class PizzaTownDatabase : RoomDatabase() {
    abstract fun cartDao(): CartDao
    abstract fun favoriteDao(): FavoriteDao
}
