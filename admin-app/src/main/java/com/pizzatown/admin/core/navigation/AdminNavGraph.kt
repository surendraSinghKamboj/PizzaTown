package com.pizzatown.admin.core.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.pizzatown.admin.presentation.analytics.AnalyticsScreen
import com.pizzatown.admin.presentation.auth.LoginScreen
import com.pizzatown.admin.presentation.broadcast.BroadcastScreen
import com.pizzatown.admin.presentation.categories.CategoriesScreen
import com.pizzatown.admin.presentation.coupons.CouponEditorScreen
import com.pizzatown.admin.presentation.coupons.CouponsListScreen
import com.pizzatown.admin.presentation.customers.UpcomingEventsScreen
import com.pizzatown.admin.presentation.dashboard.DashboardScreen
import com.pizzatown.admin.presentation.delivery.DeliveryPartnersScreen
import com.pizzatown.admin.presentation.menu.MenuEditorScreen
import com.pizzatown.admin.presentation.menu.MenuListScreen
import com.pizzatown.admin.presentation.offers.OfferEditorScreen
import com.pizzatown.admin.presentation.offers.OffersListScreen
import com.pizzatown.admin.presentation.orders.OrdersScreen
import com.pizzatown.admin.presentation.profile.ProfileScreen
import com.pizzatown.admin.presentation.settings.ShopSettingsScreen
import com.pizzatown.admin.ui.theme.AppearancePreferences

@Composable
fun AdminNavGraph(
    navController: NavHostController,
    startDestination: String,
    appearancePreferences: AppearancePreferences
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(AdminDestinations.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(AdminDestinations.DASHBOARD) {
                        popUpTo(AdminDestinations.LOGIN) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable(AdminDestinations.DASHBOARD) {
            DashboardScreen(
                onOpenCategories = {
                    navController.navigate(AdminDestinations.CATEGORIES)
                },
                onOpenMenu = {
                    navController.navigate(AdminDestinations.MENU_LIST)
                },
                onOpenOrders = {
                    navController.navigate(AdminDestinations.ORDERS)
                },
                onOpenOffers = {
                    navController.navigate(AdminDestinations.OFFERS_LIST)
                },
                onOpenUpcomingEvents = {
                    navController.navigate(AdminDestinations.UPCOMING_EVENTS)
                },
                onOpenBroadcast = {
                    navController.navigate(AdminDestinations.BROADCAST)
                },
                onOpenCoupons = {
                    navController.navigate(AdminDestinations.COUPONS_LIST)
                },
                onOpenAnalytics = {
                    navController.navigate(AdminDestinations.ANALYTICS)
                },
                onOpenShopSettings = {
                    navController.navigate(AdminDestinations.SHOP_SETTINGS)
                },
                onLoggedOut = {
                    navController.navigate(AdminDestinations.LOGIN) {
                        popUpTo(AdminDestinations.DASHBOARD) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable(AdminDestinations.ANALYTICS) {
            AnalyticsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(AdminDestinations.SHOP_SETTINGS) {
            ShopSettingsScreen(
                onBack = { navController.popBackStack() },
                appearancePreferences = appearancePreferences
            )
        }

        composable(AdminDestinations.PROFILE) {
            ProfileScreen(
                onBack = { navController.popBackStack() },
                onLoggedOut = {
                    navController.navigate(AdminDestinations.LOGIN) {
                        popUpTo(AdminDestinations.DASHBOARD) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable(AdminDestinations.ORDERS) {
            OrdersScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(AdminDestinations.CATEGORIES) {
            CategoriesScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(AdminDestinations.MENU_LIST) {
            MenuListScreen(
                onBack = { navController.popBackStack() },
                onAddItem = {
                    navController.navigate(
                        AdminDestinations.MENU_EDITOR_NEW
                    )
                },
                onEditItem = { id ->
                    navController.navigate(
                        AdminDestinations.menuEditorRoute(id)
                    )
                }
            )
        }

        composable(
            route = "${AdminDestinations.MENU_EDITOR}?${AdminDestinations.MENU_EDITOR_ARG_ID}={${AdminDestinations.MENU_EDITOR_ARG_ID}}",
            arguments = listOf(
                navArgument(AdminDestinations.MENU_EDITOR_ARG_ID) {
                    type = NavType.StringType
                    defaultValue = "new"
                }
            )
        ) {
            MenuEditorScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(AdminDestinations.OFFERS_LIST) {
            OffersListScreen(
                onBack = { navController.popBackStack() },
                onAddOffer = {
                    navController.navigate(
                        AdminDestinations.OFFER_EDITOR_NEW
                    )
                },
                onEditOffer = { id ->
                    navController.navigate(
                        AdminDestinations.offerEditorRoute(id)
                    )
                }
            )
        }

        composable(
            route = "${AdminDestinations.OFFER_EDITOR}?${AdminDestinations.OFFER_EDITOR_ARG_ID}={${AdminDestinations.OFFER_EDITOR_ARG_ID}}",
            arguments = listOf(
                navArgument(AdminDestinations.OFFER_EDITOR_ARG_ID) {
                    type = NavType.StringType
                    defaultValue = "new"
                }
            )
        ) {
            OfferEditorScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(AdminDestinations.UPCOMING_EVENTS) {
            UpcomingEventsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(AdminDestinations.BROADCAST) {
            BroadcastScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(AdminDestinations.COUPONS_LIST) {
            CouponsListScreen(
                onBack = { navController.popBackStack() },
                onAddCoupon = {
                    navController.navigate(
                        AdminDestinations.COUPON_EDITOR
                    )
                }
            )
        }

        composable(AdminDestinations.COUPON_EDITOR) {
            CouponEditorScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(AdminDestinations.DELIVERY_PARTNERS) {
            DeliveryPartnersScreen(
                onBack = { navController.popBackStack() }
            )
        }

    }
}
