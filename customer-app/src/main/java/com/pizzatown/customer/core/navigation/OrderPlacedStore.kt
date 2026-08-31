package com.pizzatown.customer.core.navigation

import com.pizzatown.customer.domain.model.Order

/**
 * Temporary in-process handoff for the just-created order.
 *
 * We intentionally do NOT use SavedStateHandle here because Order is a
 * domain object and is not a Bundle/Parcelable-compatible saved-state value.
 *
 * The order itself is already persisted by the checkout flow; this store
 * only keeps the just-created instance alive while showing Order Placed.
 */
object OrderPlacedStore {
    var order: Order? = null
}
