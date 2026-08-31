package com.pizzatown.admin.domain.model

/**
 * A seller-sent message/offer. [targetUserId] null means it goes to
 * every customer (a broadcast); set to one customer's id, it's a
 * targeted offer (e.g. a birthday special) visible only to them.
 */
data class Broadcast(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val targetUserId: String? = null,
    val targetCustomerName: String = "", // snapshot for display in admin history, empty if broadcast to all
    val createdAt: Long = 0L
)
