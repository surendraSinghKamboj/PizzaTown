package com.pizzatown.delivery

import com.pizzatown.delivery.presentation.navigation.DeliveryNavigationScreen

import com.pizzatown.delivery.presentation.home.DeliveryOrderSelection
import com.pizzatown.delivery.presentation.home.DeliveryOrderDetailScreen
import android.widget.Toast
import com.pizzatown.delivery.core.location.DeliveryLocationService
import com.pizzatown.delivery.core.notifications.DeliveryNotificationRegistrar

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Box
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import kotlinx.coroutines.delay


import dagger.hilt.android.AndroidEntryPoint

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pizzatown.delivery.presentation.DeliveryViewModel

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.HorizontalDivider
import com.pizzatown.delivery.core.theme.DeliveryTheme
import com.pizzatown.delivery.presentation.profile.ProfileScreen
import com.pizzatown.delivery.presentation.profile.ChangePasswordScreen
import com.pizzatown.delivery.presentation.profile.DeliveryProfile
import com.pizzatown.delivery.presentation.settings.AppearanceScreen
import com.pizzatown.delivery.presentation.home.DeliveryAppearance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.layout.ColumnScope
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.view.WindowCompat
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        WindowCompat.getInsetsController(
            window,
            window.decorView
        ).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }

        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        val requiredPermissions =
            buildList {
                if (android.os.Build.VERSION.SDK_INT >= 33) {
                    add(Manifest.permission.POST_NOTIFICATIONS)
                }

                add(Manifest.permission.ACCESS_FINE_LOCATION)
                add(Manifest.permission.ACCESS_COARSE_LOCATION)
            }.filter {
                ActivityCompat.checkSelfPermission(
                    this@MainActivity,
                    it
                ) != PackageManager.PERMISSION_GRANTED
            }

        if (requiredPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                requiredPermissions.toTypedArray(),
                7101
            )
        }


        setContent {
            var appearance by remember {
                mutableStateOf(
                    runCatching {
                        getSharedPreferences(
                            "delivery_preferences",
                            MODE_PRIVATE
                        ).getString(
                            "appearance",
                            DeliveryAppearance.SYSTEM.name
                        ) ?: DeliveryAppearance.SYSTEM.name
                    }.getOrDefault(
                        DeliveryAppearance.SYSTEM.name
                    )
                )
            }

            val darkTheme = when (
                runCatching {
                    DeliveryAppearance.valueOf(appearance)
                }.getOrDefault(DeliveryAppearance.SYSTEM)
            ) {
                DeliveryAppearance.DARK -> true
                DeliveryAppearance.LIGHT -> false
                DeliveryAppearance.SYSTEM ->
                    androidx.compose.foundation.isSystemInDarkTheme()
            }

            DeliveryTheme(
                darkTheme = darkTheme
            ) {
                SideEffect {
                    WindowCompat.getInsetsController(
                        window,
                        window.decorView
                    ).apply {
                        isAppearanceLightStatusBars = !darkTheme
                        isAppearanceLightNavigationBars = !darkTheme
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .statusBarsPadding()
                ) {
                    DeliveryRoot(
                        appearance = runCatching {
                            DeliveryAppearance.valueOf(appearance)
                        }.getOrDefault(DeliveryAppearance.SYSTEM),
                        onAppearanceChanged = { selected ->
                            appearance = selected.name

                            getSharedPreferences(
                                "delivery_preferences",
                                MODE_PRIVATE
                            ).edit()
                                .putString(
                                    "appearance",
                                    selected.name
                                )
                                .apply()
                        }
                    )
                }
            }
        }
    }
}



@Composable
private fun DeliveryStartupSplash(
    onFinished: () -> Unit
) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.admin_delivery_splash)
    )

    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = 1
    )

    LaunchedEffect(composition) {
        if (composition != null) {
            delay(1800)
            onFinished()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = Modifier.fillMaxSize()
        )
    }
}



@Composable
private fun DeliveryRoot(
    appearance: DeliveryAppearance,
    onAppearanceChanged: (DeliveryAppearance) -> Unit,
    viewModel: DeliveryViewModel = hiltViewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val auth = com.google.firebase.auth.FirebaseAuth.getInstance()

    var currentUser by remember {
        mutableStateOf(auth.currentUser)
    }

    androidx.compose.runtime.DisposableEffect(auth) {
        val listener =
            com.google.firebase.auth.FirebaseAuth.AuthStateListener {
                currentUser = it.currentUser
            }

        auth.addAuthStateListener(listener)

        onDispose {
            auth.removeAuthStateListener(listener)
        }
    }

    var showStartupSplash by remember {
        mutableStateOf(true)
    }

    if (showStartupSplash) {
        DeliveryStartupSplash(
            onFinished = {
                showStartupSplash = false
            }
        )
        return
    }

    if (currentUser == null) {
        LoginScreen(
            error = viewModel.loginError.collectAsState().value,
            onLogin = { email, password ->
                viewModel.login(
                    email = email,
                    password = password
                ) {}
            }
        )
        return
    }

    LaunchedEffect(currentUser?.uid) {
        if (currentUser != null) {
            DeliveryNotificationRegistrar().ensureChannel()
            DeliveryNotificationRegistrar().registerCurrentToken()
        }
    }

    var navigationOrderId by remember { mutableStateOf<String?>(null) }

    val orders by viewModel.orders.collectAsState()
    val history by viewModel.history.collectAsState()

    DeliveryAppShell(
        orders = orders,
        history = history,
        appearance = appearance,
        onAppearanceChanged = onAppearanceChanged,
        onPickedUp = { orderId ->
            viewModel.markPickedUp(
                orderId = orderId,
                onSuccess = {
                    navigationOrderId = orderId
                    DeliveryOrderSelection.selectedOrderId = null

                    runCatching {
                        val intent = Intent(
                            context,
                            DeliveryLocationService::class.java
                        )
                        context.startForegroundService(intent)
                    }

                    Toast.makeText(
                        context,
                        "Order picked up — navigation started",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                onFailure = { error ->
                    Toast.makeText(
                        context,
                        error.toString().ifBlank { "Unable to pick up order" },
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
        },
        onDelivered = { orderId ->
            viewModel.markDelivered(
                orderId = orderId,
                onSuccess = {
                    navigationOrderId = null

                    runCatching {
                        context.stopService(
                            Intent(
                                context,
                                DeliveryLocationService::class.java
                            )
                        )
                    }

                    Toast.makeText(
                        context,
                        "Order delivered",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                onFailure = { error ->
                    Toast.makeText(
                        context,
                        error.toString().ifBlank { "Unable to mark delivered" },
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
        },
        onCall = { phone ->
            val cleanPhone = phone.trim()

            if (cleanPhone.isNotBlank()) {
                runCatching {
                    context.startActivity(
                        Intent(
                            Intent.ACTION_DIAL,
                            Uri.parse("tel:$cleanPhone")
                        )
                    )
                }
            }
        },
        onDirections = { lat, lng ->
            if (
                lat in -90.0..90.0 &&
                lng in -180.0..180.0 &&
                (lat != 0.0 || lng != 0.0)
            ) {
                val googleIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("google.navigation:q=$lat,$lng")
                )

                val browserIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(
                        "https://www.google.com/maps/dir/?api=1&destination=$lat,$lng"
                    )
                )

                runCatching {
                    context.startActivity(googleIntent)
                }.onFailure {
                    runCatching {
                        context.startActivity(browserIntent)
                    }
                }
            }
        },
        onLogout = {
            viewModel.logout()
            currentUser = null
        },
        navigationOrderId = navigationOrderId,
        onCloseNavigation = {
            navigationOrderId = null
        },
        onNavigate = { orderId ->
            navigationOrderId = orderId
            DeliveryOrderSelection.selectedOrderId = null
        }
    )
}

@Composable
private fun DeliveryAppShell(
    orders: List<com.pizzatown.delivery.domain.model.DeliveryOrder>,
    history: List<com.pizzatown.delivery.domain.model.DeliveryOrder>,
    appearance: DeliveryAppearance,
    onAppearanceChanged: (DeliveryAppearance) -> Unit,
    onPickedUp: (String) -> Unit,
    onDelivered: (String) -> Unit,
    onCall: (String) -> Unit,
    onDirections: (Double, Double) -> Unit,
    onLogout: () -> Unit,
    navigationOrderId: String?,
    onCloseNavigation: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val selectedDeliveryOrder =
        DeliveryOrderSelection.selectedOrderId?.let { selectedId ->
            (orders + history).firstOrNull { it.orderId == selectedId }
        }

    if (navigationOrderId != null) {
        val navigationOrder = (orders + history)
            .firstOrNull { it.orderId == navigationOrderId }

        if (navigationOrder != null) {
            DeliveryNavigationScreen(
                order = navigationOrder,
                onBack = onCloseNavigation,
                onCall = onCall,
                onDirections = onDirections,
                onDelivered = {
                    onDelivered(navigationOrder.orderId)
                }
            )
            return
        }
    }

    if (selectedDeliveryOrder != null) {
        DeliveryOrderDetailScreen(
            order = selectedDeliveryOrder,
            onBack = {
                DeliveryOrderSelection.selectedOrderId = null
            },
            onCall = onCall,
            onPickUp = { orderId ->
                onPickedUp(orderId)
                DeliveryOrderSelection.selectedOrderId = null
            },
            onNavigate = { orderId ->
                DeliveryOrderSelection.selectedOrderId = null
                onNavigate(orderId)
            },
            onDelivered = { orderId ->
                onDelivered(orderId)
                DeliveryOrderSelection.selectedOrderId = null
            }
        )
        return
    }


    var tab by remember {
        mutableStateOf(0)
    }

    var internalPage by remember {
        mutableStateOf<String?>(null)
    }

    var password by remember {
        mutableStateOf("")
    }

    var confirmPassword by remember {
        mutableStateOf("")
    }

    var passwordMessage by remember {
        mutableStateOf<String?>(null)
    }

    var passwordSaving by remember {
        mutableStateOf(false)
    }

    val auth = com.google.firebase.auth.FirebaseAuth.getInstance()

    if (internalPage == "appearance") {
        SimpleSettingsPage(
            title = "Appearance",
            onBack = {
                internalPage = null
            }
        ) {
            Text(
                text = "Choose how PizzaTown Delivery looks",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(14.dp))

            AppearanceChoice(
                title = "System default",
                subtitle = "Follow your phone's theme",
                selected = appearance == DeliveryAppearance.SYSTEM,
                onClick = {
                    onAppearanceChanged(DeliveryAppearance.SYSTEM)
                    internalPage = null
                }
            )

            Spacer(Modifier.height(10.dp))

            AppearanceChoice(
                title = "Light",
                subtitle = "Soft light interface",
                selected = appearance == DeliveryAppearance.LIGHT,
                onClick = {
                    onAppearanceChanged(DeliveryAppearance.LIGHT)
                    internalPage = null
                }
            )

            Spacer(Modifier.height(10.dp))

            AppearanceChoice(
                title = "Dark",
                subtitle = "Comfortable dark interface",
                selected = appearance == DeliveryAppearance.DARK,
                onClick = {
                    onAppearanceChanged(DeliveryAppearance.DARK)
                    internalPage = null
                }
            )
        }

        return
    }

    if (internalPage == "password") {
        SimpleSettingsPage(
            title = "Change Password",
            onBack = {
                internalPage = null
                password = ""
                confirmPassword = ""
                passwordMessage = null
            }
        ) {

            Text(
                text = "Keep your delivery account secure.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(18.dp))

            androidx.compose.material3.OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    passwordMessage = null
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("New password") },
                visualTransformation =
                    androidx.compose.ui.text.input.PasswordVisualTransformation()
            )

            Spacer(Modifier.height(12.dp))

            androidx.compose.material3.OutlinedTextField(
                value = confirmPassword,
                onValueChange = {
                    confirmPassword = it
                    passwordMessage = null
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Confirm password") },
                visualTransformation =
                    androidx.compose.ui.text.input.PasswordVisualTransformation()
            )

            Spacer(Modifier.height(14.dp))

            if (passwordMessage != null) {
                Text(
                    text = passwordMessage.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (
                        passwordMessage.orEmpty().startsWith("Password updated")
                    ) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )

                Spacer(Modifier.height(10.dp))
            }

            Button(
                onClick = {
                    val user = auth.currentUser

                    when {
                        user == null -> {
                            passwordMessage = "Session expired. Please sign in again."
                        }

                        password.length < 6 -> {
                            passwordMessage =
                                "Password must be at least 6 characters."
                        }

                        password != confirmPassword -> {
                            passwordMessage =
                                "Passwords do not match."
                        }

                        else -> {
                            passwordSaving = true

                            user.updatePassword(password)
                                .addOnCompleteListener { task ->
                                    passwordSaving = false

                                    if (task.isSuccessful) {
                                        password = ""
                                        confirmPassword = ""
                                        passwordMessage =
                                            "Password updated successfully."
                                    } else {
                                        passwordMessage =
                                            task.exception?.message
                                                ?: "Unable to update password."
                                    }
                                }
                        }
                    }
                },
                enabled = !passwordSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(15.dp)
            ) {
                Text(
                    if (passwordSaving) {
                        "Updating..."
                    } else {
                        "Update Password"
                    },
                    fontWeight = FontWeight.Bold
                )
            }
        }

        return
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.background,
                tonalElevation = 0.dp
            ) {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = {
                        tab = 0
                    },
                    icon = {
                        Icon(
                            Icons.Filled.Home,
                            contentDescription = "Home"
                        )
                    },
                    label = {
                        Text("Home")
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                NavigationBarItem(
                    selected = tab == 1,
                    onClick = {
                        tab = 1
                    },
                    icon = {
                        Icon(
                            Icons.Filled.History,
                            contentDescription = "History"
                        )
                    },
                    label = {
                        Text("History")
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                NavigationBarItem(
                    selected = tab == 2,
                    onClick = {
                        tab = 2
                    },
                    icon = {
                        Icon(
                            Icons.Filled.Person,
                            contentDescription = "Profile"
                        )
                    },
                    label = {
                        Text("Profile")
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    ) { padding ->

        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            when (tab) {

                0 -> {
                    DeliveryDashboard(
                        orders = orders,
                        onOpenActive = {
                            tab = 0
                        }
                    )
                }

                1 -> {
                    DeliveryHistory(
                        orders = history
                    )
                }

                2 -> {
                    DeliveryProfileHome(
                        onPassword = {
                            internalPage = "password"
                        },
                        onAppearance = {
                            internalPage = "appearance"
                        },
                        onLogout = onLogout
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SimpleSettingsPage(
    title: String,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        title,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    androidx.compose.material3.IconButton(
                        onClick = onBack
                    ) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(18.dp)
                .then(Modifier)
                .let { it }
        ) {
            content()
        }
    }
}

@Composable
private fun AppearanceChoice(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor =
                if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surface
                }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        when (title) {
                            "Dark" -> Icons.Filled.DarkMode
                            "Light" -> Icons.Filled.Home
                            else -> Icons.Filled.Settings
                        },
                        contentDescription = null
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            androidx.compose.material3.RadioButton(
                selected = selected,
                onClick = onClick
            )
        }
    }
}

@Composable
private fun DeliveryDashboard(
    orders: List<com.pizzatown.delivery.domain.model.DeliveryOrder>,
    onOpenActive: () -> Unit
) {
    val ready = orders.count {
        it.status.equals("READY", true)
    }

    val onTheWay = orders.count {
        it.status.equals("ON_THE_WAY", true)
    }

    val cod = orders
        .filter {
            it.paymentMethod.equals("COD", true)
        }
        .sumOf {
            it.grandTotal
        }

    val totalValue = orders.sumOf {
        it.grandTotal
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        item {
            Spacer(Modifier.height(12.dp))

            Text(
                "Delivery Dashboard",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Text(
                "Your delivery activity at a glance",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.LocalShipping,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }

                    Spacer(Modifier.width(14.dp))

                    Column {
                        Text(
                            "Active deliveries",
                            style = MaterialTheme.typography.labelLarge
                        )

                        Text(
                            orders.size.toString(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            "Keep moving. New orders appear automatically.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DashboardTile(
                    Modifier.weight(1f),
                    "Ready",
                    ready.toString(),
                    Icons.Filled.LocalShipping
                )

                DashboardTile(
                    Modifier.weight(1f),
                    "On the way",
                    onTheWay.toString(),
                    Icons.Filled.TrendingUp
                )
            }
        }

        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DashboardTile(
                    Modifier.weight(1f),
                    "COD",
                    "₹${cod.toInt()}",
                    Icons.Filled.CheckCircle
                )

                DashboardTile(
                    Modifier.weight(1f),
                    "Order value",
                    "₹${totalValue.toInt()}",
                    Icons.Filled.TrendingUp
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    Modifier.padding(18.dp)
                ) {
                    Text(
                        "Delivery tools",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(12.dp))

                    ToolRow(
                        icon = Icons.Filled.LocalShipping,
                        title = "Active deliveries",
                        subtitle =
                            if (orders.isEmpty()) {
                                "No active deliveries right now"
                            } else {
                                "${orders.size} order(s) need your attention"
                            },
                        onClick = onOpenActive
                    )
                }
            }
        }

        item {
            Text(
                "Latest activity",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (orders.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        Modifier.padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Filled.LocalShipping,
                            contentDescription = null,
                            modifier = Modifier.size(42.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )

                        Spacer(Modifier.height(10.dp))

                        Text(
                            "You're ready for your next delivery",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(4.dp))

                        Text(
                            "Orders assigned to you after READY will appear here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(
                orders.take(5),
                key = { it.orderId }
            ) { order ->
                Card(
                    modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        DeliveryOrderSelection.selectedOrderId = order.orderId
                                    },
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Row(
                        Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.LocalShipping,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )

                        Spacer(Modifier.width(12.dp))

                        Column(
                            Modifier.weight(1f)
                        ) {
                            Text(
                                "Order #${order.orderId.takeLast(6)}",
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                order.customerName.ifBlank {
                                    "Customer"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            if (order.paymentMethod.equals("COD", true))
                                "₹${order.grandTotal.toInt()}"
                            else
                                "Paid online",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
private fun DashboardTile(
    modifier: Modifier,
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            Modifier.padding(15.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(8.dp))

            Text(
                title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ToolRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.width(12.dp))

            Column(
                Modifier.weight(1f)
            ) {
                Text(
                    title,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DeliveryHistory(
    orders: List<com.pizzatown.delivery.domain.model.DeliveryOrder>
) {
    val totalValue = orders.sumOf {
        it.grandTotal
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        item {
            Spacer(Modifier.height(14.dp))

            Text(
                "Delivery History",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Text(
                "Your completed deliveries",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(8.dp))
        }

        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DashboardTile(
                    Modifier.weight(1f),
                    "Completed",
                    orders.size.toString(),
                    Icons.Filled.CheckCircle
                )

                DashboardTile(
                    Modifier.weight(1f),
                    "Order value",
                    "₹${totalValue.toInt()}",
                    Icons.Filled.TrendingUp
                )
            }
        }

        if (orders.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Filled.History,
                            contentDescription = null,
                            modifier = Modifier.size(46.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(Modifier.height(10.dp))

                        Text(
                            "No completed deliveries yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        } else {
            items(
                orders,
                key = { it.orderId }
            ) { order ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(
                        Modifier.padding(16.dp)
                    ) {
                        Text(
                            "Order #${order.orderId.takeLast(6)}",
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(3.dp))

                        Text(
                            order.customerName.ifBlank {
                                "Customer"
                            }
                        )

                        Spacer(Modifier.height(4.dp))

                        Text(
                            order.customerAddress.ifBlank {
                                "Address unavailable"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            if (order.paymentMethod.equals("COD", true))
                                "Delivered • ₹${order.grandTotal.toInt()}"
                            else
                                "Delivered • Paid online",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
private fun DeliveryProfileHome(
    onPassword: () -> Unit,
    onAppearance: () -> Unit,
    onLogout: () -> Unit
) {
    val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
    val user = auth.currentUser

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        item {
            Spacer(Modifier.height(14.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Column(
                    Modifier.padding(22.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(68.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Box(
                            Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    Text(
                        user?.displayName
                            ?.takeIf { it.isNotBlank() }
                            ?: "Delivery Partner",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        user?.email.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        item {
            Spacer(Modifier.height(8.dp))

            Text(
                "Account & Preferences",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            ToolRow(
                Icons.Filled.Key,
                "Change Password",
                "Update your delivery account password",
                onPassword
            )
        }

        item {
            ToolRow(
                Icons.Filled.DarkMode,
                "Appearance",
                "Choose System, Light or Dark",
                onAppearance
            )
        }

        item {
            Spacer(Modifier.height(8.dp))

            OutlinedButton(
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(15.dp)
            ) {
                Text(
                    "Sign Out",
                    fontWeight = FontWeight.Bold
                )
            }
        }

        item {
            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
private fun LoginScreen(
    error: String?,
    onLogin: (String, String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val primary = Color(0xFFFF4D2D)
    val primaryDark = Color(0xFFE63F23)
    val plum = Color(0xFF7B3F61)

    val background = MaterialTheme.colorScheme.background
    val surface = MaterialTheme.colorScheme.surface
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val text = MaterialTheme.colorScheme.onSurface
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(Modifier.height(28.dp))

        /*
         * Brand mark
         */
        Surface(
            modifier = Modifier
                .size(104.dp)
                .clip(RoundedCornerShape(30.dp)),
            color = primary.copy(alpha = 0.10f),
            shape = RoundedCornerShape(30.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.size(76.dp),
                    shape = CircleShape,
                    color = primary
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocalShipping,
                            contentDescription = "PizzaTown Delivery",
                            modifier = Modifier.size(40.dp),
                            tint = Color.White
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(22.dp))

        Text(
            text = "PizzaTown",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = text
        )

        Spacer(Modifier.height(3.dp))

        Text(
            text = "DELIVERY",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = primary,
            letterSpacing = MaterialTheme.typography.labelLarge.letterSpacing
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = "Delivery Partner",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = text
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = "Sign in to manage your deliveries",
            style = MaterialTheme.typography.bodyMedium,
            color = muted
        )

        Spacer(Modifier.height(30.dp))

        /*
         * Login card
         */
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = surface,
            tonalElevation = 2.dp,
            shadowElevation = 1.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {

                Text(
                    text = "Welcome back",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = text
                )

                Spacer(Modifier.height(5.dp))

                Text(
                    text = "Use your delivery account credentials.",
                    style = MaterialTheme.typography.bodySmall,
                    color = muted
                )

                Spacer(Modifier.height(22.dp))

                Text(
                    text = "Email address",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = text
                )

                Spacer(Modifier.height(7.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(15.dp),
                    placeholder = {
                        Text("Enter your email")
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Email,
                            contentDescription = null
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = primary,
                        focusedLeadingIconColor = primary,
                        cursorColor = primary
                    )
                )

                Spacer(Modifier.height(17.dp))

                Text(
                    text = "Password",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = text
                )

                Spacer(Modifier.height(7.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(15.dp),
                    placeholder = {
                        Text("Enter your password")
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null
                        )
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                passwordVisible = !passwordVisible
                            }
                        ) {
                            Icon(
                                imageVector = if (passwordVisible) {
                                    Icons.Filled.VisibilityOff
                                } else {
                                    Icons.Filled.Visibility
                                },
                                contentDescription = if (passwordVisible) {
                                    "Hide password"
                                } else {
                                    "Show password"
                                }
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = primary,
                        focusedLeadingIconColor = primary,
                        focusedTrailingIconColor = primary,
                        cursorColor = primary
                    )
                )

                if (!error.isNullOrBlank()) {
                    Spacer(Modifier.height(16.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            text = error,
                            modifier = Modifier.padding(
                                horizontal = 14.dp,
                                vertical = 12.dp
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(Modifier.height(22.dp))

                Button(
                    onClick = {
                        onLogin(email.trim(), password)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primary,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocalShipping,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(Modifier.width(9.dp))

                    Text(
                        text = "Sign In",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(Modifier.height(22.dp))

        /*
         * Small trust / workflow hint
         */
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(8.dp),
                shape = CircleShape,
                color = primary
            ) {}

            Spacer(Modifier.width(8.dp))

            Text(
                text = "Your delivery account is managed by PizzaTown Admin",
                style = MaterialTheme.typography.labelSmall,
                color = muted
            )
        }

        Spacer(Modifier.height(28.dp))

        Text(
            text = "PizzaTown Delivery",
            style = MaterialTheme.typography.labelSmall,
            color = muted.copy(alpha = 0.75f)
        )

        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun OrdersScreen(
    orders: List<com.pizzatown.delivery.domain.model.DeliveryOrder>,
    onCall: (String) -> Unit,
    onDirections: (Double, Double) -> Unit,
    onPickedUp: (String) -> Unit,
    onDelivered: (String) -> Unit,
    onLogout: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    Column(
        Modifier.fillMaxSize()
    ) {

        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "My Deliveries",
                style = MaterialTheme.typography.headlineSmall
            )

            TextButton(onClick = onLogout) {
                Text("Logout")
            }
        }

        if (orders.isEmpty()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "No deliveries assigned",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "Orders will appear here when they are ready."
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                items(orders) { order ->

                    Card(
                        Modifier.fillMaxWidth()
                    ) {
                        Column(
                            Modifier.padding(16.dp)
                        ) {

                            Text(
                                "#${order.orderId.takeLast(6).uppercase()}",
                                style = MaterialTheme.typography.titleMedium
                            )

                            Spacer(Modifier.height(8.dp))

                            Text(
                                order.customerName.ifBlank {
                                    "Customer"
                                },
                                style = MaterialTheme.typography.titleLarge
                            )

                            Text(order.customerPhone)
                            Text(order.customerAddress)

                            Spacer(Modifier.height(12.dp))

                            order.items.forEach { item ->
                                Text(
                                    "${item.quantity} × ${item.name}" +
                                        (item.variantName?.let {
                                            " • $it"
                                        } ?: "")
                                )

                                if (item.customizationNames.isNotEmpty()) {
                                    Text(
                                        item.customizationNames.joinToString(
                                            prefix = "   ",
                                            separator = ", "
                                        )
                                    )
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            if (
                                order.paymentMethod.equals(
                                    "COD",
                                    ignoreCase = true
                                )
                            ) {
                                Text(
                                    "Collect on delivery: ₹${order.grandTotal.toInt()}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            if (
                                order.paymentMethod.equals(
                                    "ONLINE",
                                    ignoreCase = true
                                ) &&
                                order.paymentStatus.equals(
                                    "PAID",
                                    ignoreCase = true
                                )
                            ) {
                                Text("Online payment • Paid")
                            }

                            Spacer(Modifier.height(12.dp))

                            Row(
                                horizontalArrangement =
                                    Arrangement.spacedBy(8.dp)
                            ) {

                                OutlinedButton(
                                    onClick = {
                                        onCall(order.customerPhone)
                                    }
                                ) {
                                    Text("Call")
                                }

                                Button(
                                    onClick = {
                                        onDirections(
                                            order.deliveryLat,
                                            order.deliveryLng
                                        )
                                    },
                                    enabled =
                                        order.deliveryLat != 0.0 &&
                                        order.deliveryLng != 0.0
                                ) {
                                    Text("Directions")
                                }
                            }

                            Spacer(Modifier.height(8.dp))

                            when (order.status) {

                                "READY" -> {
                                    Button(
                                        onClick = {
                                            onPickedUp(order.orderId)
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Pick Up Order")
                                    }
                                }

                                "ON_THE_WAY",
                                "ON_THE_WAY" -> {
                                    Button(
                                        onClick = {
                                            onDelivered(order.orderId)
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Mark Delivered")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
