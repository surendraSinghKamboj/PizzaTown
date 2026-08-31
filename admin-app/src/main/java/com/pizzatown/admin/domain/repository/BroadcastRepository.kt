package com.pizzatown.admin.domain.repository

import com.pizzatown.admin.domain.model.Broadcast
import kotlinx.coroutines.flow.Flow

interface BroadcastRepository {
    /** Full send history, most recent first. */
    fun observeBroadcasts(): Flow<List<Broadcast>>

    /** [targetUserId] null = send to every customer; set = targeted to just that one customer. */
    suspend fun send(title: String, message: String, targetUserId: String?, targetCustomerName: String): Result<Unit>

    suspend fun delete(id: String): Result<Unit>
}
