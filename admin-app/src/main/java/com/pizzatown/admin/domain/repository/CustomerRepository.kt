package com.pizzatown.admin.domain.repository

import com.pizzatown.admin.domain.model.Customer
import kotlinx.coroutines.flow.Flow

interface CustomerRepository {
    /** All registered customers (admin-only read, per Firestore rules). */
    fun observeCustomers(): Flow<List<Customer>>
}
