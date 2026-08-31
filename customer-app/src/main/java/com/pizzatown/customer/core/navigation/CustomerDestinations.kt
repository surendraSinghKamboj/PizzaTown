package com.pizzatown.customer.core.navigation

object CustomerDestinations {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTER = "register"

    const val MENU = "menu"
    const val CART = "cart"
    const val PROFILE = "profile"
    const val CHECKOUT = "checkout"
    const val ORDER_HISTORY = "order_history"
    const val ORDER_PLACED = "order_placed"
    const val NOTIFICATIONS = "notifications"

    const val ITEM_DETAILS_ARG_ID = "itemId"
    const val ITEM_DETAILS = "item_details/{$ITEM_DETAILS_ARG_ID}"
    fun itemDetailsRoute(itemId: String) = "item_details/$itemId"

    /** Routes that show the bottom navigation bar. */
    val bottomNavRoutes = setOf(MENU, CART, PROFILE)
}
