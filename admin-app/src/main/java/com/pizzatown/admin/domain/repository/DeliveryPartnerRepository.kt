package com.pizzatown.admin.domain.repository

import com.pizzatown.admin.domain.model.DeliveryPartner

interface DeliveryPartnerRepository {
    suspend fun createPartner(
        name: String,
        email: String,
        phone: String,
        password: String
    )

    suspend fun getPartners(): List<DeliveryPartner>

    suspend fun updatePartner(
        uid: String,
        name: String,
        email: String,
        phone: String
    )

    suspend fun setPartnerActive(
        uid: String,
        active: Boolean
    )

    suspend fun resetPassword(
        uid: String,
        password: String
    )

    suspend fun deletePartner(
        uid: String
    )
}
