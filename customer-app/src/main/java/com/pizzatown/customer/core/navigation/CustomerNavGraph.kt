package com.pizzatown.customer.core.navigation

import com.pizzatown.customer.core.navigation.OrderPlacedStore

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.pizzatown.customer.presentation.auth.LoginScreen
import com.pizzatown.customer.presentation.auth.RegisterScreen
import com.pizzatown.customer.presentation.cart.CartScreen
import com.pizzatown.customer.presentation.checkout.CheckoutScreen
import com.pizzatown.customer.presentation.menu.MenuItemDetailsScreen
import com.pizzatown.customer.presentation.menu.MenuScreen
import com.pizzatown.customer.presentation.notifications.NotificationInboxScreen
import com.pizzatown.customer.presentation.orders.OrderHistoryScreen
import com.pizzatown.customer.presentation.orders.OrderPlacedScreen
import com.pizzatown.customer.presentation.profile.ProfileScreen
import com.pizzatown.customer.presentation.splash.SplashScreen

private const val ANIM_DURATION = 280

private val slideInFromRight: AnimatedContentTransitionScope<*>.() -> EnterTransition = {
    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(ANIM_DURATION)) +
        fadeIn(tween(ANIM_DURATION))
}
private val slideOutToLeft: AnimatedContentTransitionScope<*>.() -> ExitTransition = {
    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(ANIM_DURATION)) +
        fadeOut(tween(ANIM_DURATION))
}
private val slideInFromLeft: AnimatedContentTransitionScope<*>.() -> EnterTransition = {
    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(ANIM_DURATION)) +
        fadeIn(tween(ANIM_DURATION))
}
private val slideOutToRight: AnimatedContentTransitionScope<*>.() -> ExitTransition = {
    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(ANIM_DURATION)) +
        fadeOut(tween(ANIM_DURATION))
}
private val fadeInSlow: AnimatedContentTransitionScope<*>.() -> EnterTransition = { fadeIn(tween(350)) }
private val fadeOutSlow: AnimatedContentTransitionScope<*>.() -> ExitTransition = { fadeOut(tween(200)) }

@Composable
fun CustomerNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = CustomerDestinations.SPLASH,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None }
    ) {

        composable(
            CustomerDestinations.SPLASH,
            enterTransition = fadeInSlow,
            exitTransition = fadeOutSlow
        ) {
            SplashScreen(
                onNavigateToLogin = {
                    navController.navigate(CustomerDestinations.LOGIN) {
                        popUpTo(CustomerDestinations.SPLASH) { inclusive = true }
                    }
                },
                onNavigateToMenu = {
                    navController.navigate(CustomerDestinations.MENU) {
                        popUpTo(CustomerDestinations.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(CustomerDestinations.LOGIN, enterTransition = fadeInSlow, exitTransition = fadeOutSlow) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(CustomerDestinations.MENU) {
                        popUpTo(CustomerDestinations.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToRegister = { navController.navigate(CustomerDestinations.REGISTER) }
            )
        }

        composable(CustomerDestinations.REGISTER) {
            RegisterScreen(
                onBack = { navController.popBackStack() },
                onRegisterSuccess = {
                    navController.navigate(CustomerDestinations.MENU) {
                        popUpTo(CustomerDestinations.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(CustomerDestinations.MENU, enterTransition = fadeInSlow, exitTransition = fadeOutSlow) {
            MenuScreen(
                onItemClick = { id -> navController.navigate(CustomerDestinations.itemDetailsRoute(id)) },
                onOpenNotifications = { navController.navigate(CustomerDestinations.NOTIFICATIONS) },
                onOpenProfile = {
                    navController.navigate(CustomerDestinations.PROFILE) {
                        launchSingleTop = true
                    }
                },
                onOpenOrderHistory = { navController.navigate(CustomerDestinations.ORDER_HISTORY) },
                onLoggedOut = {
                    navController.navigate(CustomerDestinations.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(CustomerDestinations.NOTIFICATIONS) {
            NotificationInboxScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = CustomerDestinations.ITEM_DETAILS,
            arguments = listOf(navArgument(CustomerDestinations.ITEM_DETAILS_ARG_ID) { type = NavType.StringType })
        ) {
            MenuItemDetailsScreen(
                onBack = { navController.popBackStack() },
                onAddedToCart = { }
            )
        }

        composable(CustomerDestinations.CART, enterTransition = fadeInSlow, exitTransition = fadeOutSlow) {
            CartScreen(onCheckout = { navController.navigate(CustomerDestinations.CHECKOUT) })
        }

        composable(CustomerDestinations.CHECKOUT) {
            CheckoutScreen(
                onBack = { navController.popBackStack() },
                onOrderPlaced = { order ->
                    OrderPlacedStore.order = order

                    navController.navigate(CustomerDestinations.ORDER_PLACED) {
                        // Payment success creates a new flow root:
                        // HOME -> ORDER_PLACED
                        // Cart and Checkout are removed from back stack.
                        popUpTo(CustomerDestinations.MENU) {
                            inclusive = false
                        }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(CustomerDestinations.PROFILE, enterTransition = fadeInSlow, exitTransition = fadeOutSlow) {
            ProfileScreen(
                onLoggedOut = {
                    navController.navigate(CustomerDestinations.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onViewOrders = { navController.navigate(CustomerDestinations.ORDER_HISTORY) }
            )
        }

        composable(CustomerDestinations.ORDER_PLACED) {
            val order = OrderPlacedStore.order

            if (order != null) {

                val goHome: () -> Unit = {
                    OrderPlacedStore.order = null
                    navController.popBackStack()
                }

                OrderPlacedScreen(
                    order = order,

                    onViewOrders = {
                        OrderPlacedStore.order = null

                        navController.navigate(CustomerDestinations.ORDER_HISTORY) {
                            popUpTo(CustomerDestinations.MENU) {
                                inclusive = false
                            }
                            launchSingleTop = true
                        }
                    },

                    onBackToHome = goHome
                )

            } else {
                // Defensive fallback: if the temporary in-memory order
                // is unavailable, send the user to the Orders screen.
                navController.navigate(CustomerDestinations.ORDER_HISTORY) {
                    popUpTo(CustomerDestinations.MENU) {
                        inclusive = false
                    }
                    launchSingleTop = true
                }
            }
        }

        composable(
            route = CustomerDestinations.ORDER_HISTORY,
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            OrderHistoryScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
