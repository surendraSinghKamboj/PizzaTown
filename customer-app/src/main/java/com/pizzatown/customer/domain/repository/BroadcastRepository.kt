package com.pizzatown.customer.domain.repository

import com.pizzatown.customer.domain.model.Broadcast
import kotlinx.coroutines.flow.Flow

interface BroadcastRepository {
    /**
     * Messages visible to this signed-in user: broadcasts sent to
     * everyone, plus any sent specifically to them. Firestore security
     * rules do the actual filtering (see firebase/firestore.rules) —
     * this is a plain unfiltered listener, and the server only ever
     * returns documents this user is allowed to read.
     */
    fun observeMyBroadcasts(): Flow<List<Broadcast>>
}
