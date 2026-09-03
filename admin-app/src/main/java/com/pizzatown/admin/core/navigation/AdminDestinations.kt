package com.pizzatown.admin.core.navigation

object AdminDestinations {
    const val LOGIN = "login"
    const val DASHBOARD = "dashboard"
    const val CATEGORIES = "categories"
    const val MENU_LIST = "menu_list"
    const val ORDERS = "orders"
    const val MENU_EDITOR = "menu_editor"
    const val MENU_EDITOR_ARG_ID = "itemId"
    const val MENU_EDITOR_NEW = "menu_editor?$MENU_EDITOR_ARG_ID=new"

    const val OFFERS_LIST = "offers_list"
    const val OFFER_EDITOR = "offer_editor"
    const val OFFER_EDITOR_ARG_ID = "offerId"
    const val OFFER_EDITOR_NEW = "offer_editor?$OFFER_EDITOR_ARG_ID=new"

    const val UPCOMING_EVENTS = "upcoming_events"
    const val BROADCAST = "broadcast"
    const val COUPONS_LIST = "coupons_list"
    const val COUPON_EDITOR = "coupon_editor"
    const val ANALYTICS = "analytics"
    const val SHOP_SETTINGS = "shop_settings"
   const val DELIVERY_PARTNERS = "delivery_partners"

    const val PROFILE = "profile"

    fun menuEditorRoute(itemId: String) = "menu_editor?$MENU_EDITOR_ARG_ID=$itemId"
    fun offerEditorRoute(offerId: String) = "offer_editor?$OFFER_EDITOR_ARG_ID=$offerId"
}
