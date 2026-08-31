package com.pizzatown.admin

import com.airbnb.lottie.compose.LottieConstants

import androidx.compose.foundation.layout.size



import androidx.compose.ui.unit.dp

import androidx.compose.ui.Alignment

import com.pizzatown.admin.R




import com.airbnb.lottie.compose.rememberLottieComposition

import com.airbnb.lottie.compose.animateLottieCompositionAsState

import com.airbnb.lottie.compose.LottieCompositionSpec

import com.airbnb.lottie.compose.LottieAnimation

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.compose.rememberNavController
import com.pizzatown.admin.ui.theme.ThemeMode
import com.pizzatown.admin.ui.theme.AppearancePreferences
import com.pizzatown.admin.core.navigation.AdminDestinations
import com.pizzatown.admin.core.navigation.AdminNavGraph
import com.pizzatown.admin.core.notifications.AdminNotificationRegistrar
import com.pizzatown.admin.domain.repository.AdminAuthRepository
import com.pizzatown.admin.ui.theme.PizzaTownAdminTheme
import com.pizzatown.admin.domain.model.RestaurantStatus
import com.pizzatown.admin.domain.repository.SettingsRepository
import com.pizzatown.admin.presentation.shell.AdminShell
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf

@HiltViewModel
class SessionViewModel @Inject constructor(
    authRepository: AdminAuthRepository
) : ViewModel() {
    // null = still resolving; true/false once we know
    val isAdminSignedIn: StateFlow<Boolean?> = authRepository.isAdminSignedIn
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var notificationRegistrar: AdminNotificationRegistrar
    @Inject lateinit var appearancePreferences: AppearancePreferences
    @Inject lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by appearancePreferences.themeMode.collectAsState(
                initial = ThemeMode.SYSTEM
            )

            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { /* no-op: if denied, we just skip showing notifications */ }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            PizzaTownAdminTheme(themeMode = themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AdminRoot(
                        notificationRegistrar = notificationRegistrar,
                        appearancePreferences = appearancePreferences,
                        settingsRepository = settingsRepository
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminAuthLoading() {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.admin_loading)
    )

    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever
    )

    androidx.compose.foundation.layout.Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = Modifier.size(180.dp)
        )
    }
}

@Composable
private fun AdminRoot(
    notificationRegistrar: AdminNotificationRegistrar,
    appearancePreferences: AppearancePreferences,
    settingsRepository: SettingsRepository,
    sessionViewModel: SessionViewModel = hiltViewModel()
) {
    val isSignedIn by sessionViewModel.isAdminSignedIn.collectAsState()

    LaunchedEffect(isSignedIn) {
        if (isSignedIn == true) {
            notificationRegistrar.subscribeToAdminTopic()
        }
    }

    when (isSignedIn) {
        null -> AdminAuthLoading()

        false -> {
            val navController = rememberNavController()

            AdminNavGraph(
                navController = navController,
                startDestination = AdminDestinations.LOGIN,
                appearancePreferences = appearancePreferences
            )
        }

        true -> {
            val navController = rememberNavController()


            val restaurantStatus by settingsRepository
                .observeRestaurantStatus()
                .collectAsState(
                    initial = RestaurantStatus()
                )

            val currentRoute = navController.currentBackStackEntryFlow
                .collectAsState(
                    initial = null
                )
                .value
                ?.destination
                ?.route

            val scope = rememberCoroutineScope()

            fun navigateTo(route: String) {
                navController.navigate(route) {
                    launchSingleTop = true
                    restoreState = true
                }
            }

            AdminShell(
                title = when (currentRoute) {
                    AdminDestinations.ORDERS -> "Orders"
                    AdminDestinations.ANALYTICS -> "Analytics"
                    AdminDestinations.MENU_LIST -> "Menu"
                    AdminDestinations.CATEGORIES -> "Categories"
                    AdminDestinations.OFFERS_LIST -> "Offers & Banners"
                    AdminDestinations.COUPONS_LIST -> "Coupons"
                    AdminDestinations.UPCOMING_EVENTS -> "Events"
                    AdminDestinations.BROADCAST -> "Broadcast"
                    AdminDestinations.SHOP_SETTINGS -> "Shop Settings"
                    AdminDestinations.PROFILE -> "Profile"
                    else -> "Dashboard"
                },
                restaurantStatus = restaurantStatus,
                currentRoute = currentRoute,
                onNavigate = ::navigateTo,
                onToggleRestaurant = { isOpen ->
                    scope.launch {
                        settingsRepository.setRestaurantOpen(isOpen)
                    }
                },
                onOpenOrders = {
                    navController.navigate(AdminDestinations.ORDERS) {
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onOpenProfile = {
                    navController.navigate(AdminDestinations.PROFILE) {
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                content = {
                    AdminNavGraph(
                        navController = navController,
                        startDestination = AdminDestinations.DASHBOARD,
                        appearancePreferences = appearancePreferences
                    )
                }
            )
        }
    }
}

