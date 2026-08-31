package com.pizzatown.admin.data.repository

import com.pizzatown.admin.domain.repository.AdminAuthRepository
import com.pizzatown.admin.domain.repository.BroadcastRepository
import com.pizzatown.admin.domain.repository.CategoryRepository
import com.pizzatown.admin.domain.repository.CouponRepository
import com.pizzatown.admin.domain.repository.CustomerRepository
import com.pizzatown.admin.domain.repository.MenuRepository
import com.pizzatown.admin.domain.repository.OfferRepository
import com.pizzatown.admin.domain.repository.OrderRepository
import com.pizzatown.admin.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAdminAuthRepository(impl: AdminAuthRepositoryImpl): AdminAuthRepository

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(impl: CategoryRepositoryImpl): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindMenuRepository(impl: MenuRepositoryImpl): MenuRepository

    @Binds
    @Singleton
    abstract fun bindOrderRepository(impl: OrderRepositoryImpl): OrderRepository

    @Binds
    @Singleton
    abstract fun bindOfferRepository(impl: OfferRepositoryImpl): OfferRepository

    @Binds
    @Singleton
    abstract fun bindCustomerRepository(impl: CustomerRepositoryImpl): CustomerRepository

    @Binds
    @Singleton
    abstract fun bindBroadcastRepository(impl: BroadcastRepositoryImpl): BroadcastRepository

    @Binds
    @Singleton
    abstract fun bindCouponRepository(impl: CouponRepositoryImpl): CouponRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
}
