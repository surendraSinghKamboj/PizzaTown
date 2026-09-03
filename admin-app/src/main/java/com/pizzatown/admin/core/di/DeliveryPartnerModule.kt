package com.pizzatown.admin.core.di

import com.google.firebase.functions.FirebaseFunctions
import com.pizzatown.admin.data.repository.DeliveryPartnerRepositoryImpl
import com.pizzatown.admin.domain.repository.DeliveryPartnerRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DeliveryPartnerBindings {

    @Binds
    @Singleton
    abstract fun bindDeliveryPartnerRepository(
        impl: DeliveryPartnerRepositoryImpl
    ): DeliveryPartnerRepository
}

@Module
@InstallIn(SingletonComponent::class)
object DeliveryPartnerProviders {

    @Provides
    @Singleton
    fun provideFirebaseFunctions(): FirebaseFunctions =
        FirebaseFunctions.getInstance("asia-south1")
}
