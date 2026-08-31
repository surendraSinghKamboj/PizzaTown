package com.pizzatown.customer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import com.pizzatown.customer.core.common.UiState
import com.pizzatown.customer.presentation.profile.ProfileViewModel
import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.cashfree.pg.api.CFPaymentGatewayService
import com.cashfree.pg.core.api.CFSession.CFSessionBuilder
import com.cashfree.pg.core.api.callback.CFCheckoutResponseCallback
import com.cashfree.pg.core.api.exception.CFException
import com.cashfree.pg.core.api.utils.CFErrorResponse
import com.cashfree.pg.core.api.webcheckout.CFWebCheckoutPayment.CFWebCheckoutPaymentBuilder
import com.pizzatown.customer.core.navigation.CustomerDestinations
import com.pizzatown.customer.core.navigation.CustomerNavGraph
import com.pizzatown.customer.core.notifications.NotificationRegistrar
import com.pizzatown.customer.core.payment.CashfreeCheckoutBridge
import com.pizzatown.customer.core.payment.CashfreeConfig
import com.pizzatown.customer.domain.repository.AuthRepository
import com.pizzatown.customer.domain.repository.OrderRepository
import com.pizzatown.customer.presentation.cart.CartViewModel
import com.pizzatown.customer.presentation.cart.CartMiniBar
import com.pizzatown.customer.presentation.orders.CurrentOrderMiniBar
import com.pizzatown.customer.presentation.settings.ThemeViewModel
import com.pizzatown.customer.ui.theme.PizzaTownCustomerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity(), CFCheckoutResponseCallback {

    @Inject lateinit var authRepository: AuthRepository
    @Inject lateinit var orderRepository: OrderRepository
    @Inject lateinit var notificationRegistrar: NotificationRegistrar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Cashfree requires the checkout callback to be (re)set in onCreate
        // — this also correctly handles activity-recreation cases (e.g.
        // rotation, or process death while the payment screen was open).
        try {
            CFPaymentGatewayService.getInstance().setCheckoutCallback(this)
        } catch (e: CFException) {
            e.printStackTrace()
        }

        // Listens for CheckoutViewModel asking us to open the Cashfree
        // checkout screen — see CashfreeCheckoutBridge for why this lives
        // here rather than in the ViewModel (the SDK needs an Activity).
        lifecycleScope.launch {
            CashfreeCheckoutBridge.launchRequests.collect { request ->
                try {
                    val cfSession = CFSessionBuilder()
                        .setEnvironment(CashfreeConfig.environment)
                        .setPaymentSessionID(request.paymentSessionId)
                        .setOrderId(request.orderId)
                        .build()
                    val cfWebCheckoutPayment = CFWebCheckoutPaymentBuilder()
                        .setSession(cfSession)
                        .build()
                    CFPaymentGatewayService.getInstance().doPayment(this@MainActivity, cfWebCheckoutPayment)
                } catch (e: CFException) {
                    CashfreeCheckoutBridge.reportFailure(
                        request.orderId,
                        e.message ?: "Couldn't open the payment screen. Please try again."
                    )
                }
            }
        }

        setContent {
            val themeViewModel: ThemeViewModel = hiltViewModel()
            val themeMode by themeViewModel.themeMode.collectAsStateWithLifecycle()

            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { /* no-op: if denied, we just skip showing notifications */ }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            val isSignedIn by authRepository.isSignedIn.collectAsStateWithLifecycle(initialValue = false)
            LaunchedEffect(isSignedIn) {
                if (isSignedIn) {
                    authRepository.currentUserId?.let { uid -> notificationRegistrar.syncForSignedInUser(uid) }
                }
            }

            PizzaTownCustomerTheme(themeMode = themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    CustomerRoot(
                    authRepository = authRepository,
                    orderRepository = orderRepository
                )
                }
            }
        }
    }

    override fun onPaymentVerify(orderID: String) {
        // Checkout journey finished — verification of the *real* status
        // happens server-side (CheckoutViewModel calls verifyCashfreePayment).
        lifecycleScope.launch { CashfreeCheckoutBridge.reportVerify(orderID) }
    }

    override fun onPaymentFailure(cfErrorResponse: CFErrorResponse, orderID: String) {
        lifecycleScope.launch {
            CashfreeCheckoutBridge.reportFailure(
                orderID,
                cfErrorResponse.message ?: "Payment failed or was cancelled."
            )
        }
    }
}

@Composable
private fun CustomerRoot(
    authRepository: AuthRepository,
    orderRepository: OrderRepository
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in CustomerDestinations.bottomNavRoutes

    val profileViewModel: ProfileViewModel = hiltViewModel()
    val profileState by profileViewModel.profileState.collectAsStateWithLifecycle()

    val profileInitial = when (val current = profileState) {
        is UiState.Success -> current.data.fullName
            .trim()
            .firstOrNull()
            ?.uppercaseChar()
            ?.toString()
            ?: "?"
        else -> "?"
    }

    val cartViewModel: CartViewModel = hiltViewModel()
    val cartData by cartViewModel.cartData.collectAsStateWithLifecycle()
    val cartCount = cartData.totals.totalItemCount
    val cartTotal = cartData.totals.grandTotal

    val currentUserId = authRepository.currentUserId

    val activeOrders by (
        currentUserId?.let { uid ->
            orderRepository.observeOrdersForUser(uid)
        } ?: kotlinx.coroutines.flow.flowOf(emptyList())
    ).collectAsStateWithLifecycle(initialValue = emptyList())

    Scaffold(
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                if (currentRoute == CustomerDestinations.MENU) {

                    CurrentOrderMiniBar(
                        orders = activeOrders,
                        onViewOrders = {
                            navController.navigate(CustomerDestinations.ORDER_HISTORY) {
                                launchSingleTop = true
                            }
                        }
                    )

                    CartMiniBar(
                        itemCount = cartCount,
                        total = cartTotal,
                        onViewCart = {
                            navController.navigate(CustomerDestinations.CART) {
                                launchSingleTop = true
                            }
                        }
                    )
                }

                if (showBottomBar) {
                    CustomerBottomBar(
                        navController = navController,
                        currentRoute = currentRoute,
                        profileInitial = profileInitial,
                        cartViewModel = cartViewModel
                    )
                }
            }
        }
    ) { padding ->
        androidx.compose.foundation.layout.Box(
            modifier = Modifier.padding(padding)
        ) {
            CustomerNavGraph(navController = navController)
        }
    }
}

@Composable
private fun CustomerBottomBar(
    navController: androidx.navigation.NavHostController,
    currentRoute: String?,
    profileInitial: String,
    cartViewModel: CartViewModel = hiltViewModel()
) {
    val cartData by cartViewModel.cartData.collectAsStateWithLifecycle()
    val cartCount = cartData.totals.totalItemCount

    NavigationBar(
        modifier = Modifier.height(78.dp),
        tonalElevation = 6.dp,
        containerColor = MaterialTheme.colorScheme.surface
    ) {

        NavigationBarItem(
            selected = currentRoute == CustomerDestinations.MENU,
            onClick = {
                navigateToTab(
                    navController,
                    CustomerDestinations.MENU
                )
            },
            icon = {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(
                            if (currentRoute == CustomerDestinations.MENU)
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.13f)
                            else
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Home,
                        contentDescription = "Home",
                        tint = if (currentRoute == CustomerDestinations.MENU)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(21.dp)
                    )
                }
            },
            label = {
                Text(
                    "Home",
                    fontWeight = if (
                        currentRoute == CustomerDestinations.MENU
                    ) FontWeight.Bold else FontWeight.Medium
                )
            },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                selectedIconColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )

        NavigationBarItem(
            selected = currentRoute == CustomerDestinations.CART,
            onClick = {
                navigateToTab(
                    navController,
                    CustomerDestinations.CART
                )
            },
            icon = {
                BadgedBox(
                    badge = {
                        if (cartCount > 0) {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.primary
                            ) {
                                androidx.compose.animation.AnimatedContent(
                                    targetState = cartCount,
                                    label = "cart-badge"
                                ) { count ->
                                    Text(
                                        count.toString(),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                if (currentRoute == CustomerDestinations.CART)
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.13f)
                                else
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.ShoppingCart,
                            contentDescription = "Cart",
                            tint = if (currentRoute == CustomerDestinations.CART)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }
            },
            label = {
                Text(
                    "Cart",
                    fontWeight = if (
                        currentRoute == CustomerDestinations.CART
                    ) FontWeight.Bold else FontWeight.Medium
                )
            },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                selectedIconColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )

        NavigationBarItem(
            selected = currentRoute == CustomerDestinations.PROFILE,
            onClick = {
                navigateToTab(
                    navController,
                    CustomerDestinations.PROFILE
                )
            },
            icon = {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(
                            if (currentRoute == CustomerDestinations.PROFILE)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = profileInitial,
                        color = if (currentRoute == CustomerDestinations.PROFILE)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            label = {
                Text(
                    "Profile",
                    fontWeight = if (
                        currentRoute == CustomerDestinations.PROFILE
                    ) FontWeight.Bold else FontWeight.Medium
                )
            },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                selectedIconColor = MaterialTheme.colorScheme.primary,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}

private fun navigateToTab(navController: androidx.navigation.NavHostController, route: String) {
    navController.navigate(route) {
        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
