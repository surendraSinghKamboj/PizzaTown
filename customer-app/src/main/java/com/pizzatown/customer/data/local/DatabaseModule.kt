package com.pizzatown.customer.data.local

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PizzaTownDatabase =
        Room.databaseBuilder(context, PizzaTownDatabase::class.java, "pizza_town.db")
            // Cart + Favorites are both local-only caches (not the source of truth —
            // Firestore/checkout own the real order data), so a destructive migration
            // on schema bump is an acceptable, safe tradeoff versus writing/testing a
            // real Room migration for a non-critical local table.
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    @Singleton
    fun provideCartDao(database: PizzaTownDatabase): CartDao = database.cartDao()

    @Provides
    @Singleton
    fun provideFavoriteDao(database: PizzaTownDatabase): FavoriteDao = database.favoriteDao()
}
