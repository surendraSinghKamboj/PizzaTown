package com.pizzatown.admin.presentation.shell

import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material.icons.filled.Person

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pizzatown.admin.domain.model.RestaurantStatus
import kotlinx.coroutines.launch

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AdminShell(
    title: String,
    restaurantStatus: RestaurantStatus,
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    onToggleRestaurant: (Boolean) -> Unit,
    onOpenOrders: () -> Unit,
    onOpenProfile: () -> Unit,
    content: @Composable (androidx.compose.foundation.layout.PaddingValues) -> Unit
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                DrawerHeader(
                    isOpen = restaurantStatus.isOpen,
                    onToggle = onToggleRestaurant
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                DrawerItem(
                    label = "Dashboard",
                    icon = Icons.Filled.RestaurantMenu,
                    selected = currentRoute == "dashboard",
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigate("dashboard")
                    }
                )


                DrawerItem(
                    label = "Profile",
                    icon = Icons.Filled.Person,
                    selected = currentRoute == "profile",
                    onClick = {
                        scope.launch { drawerState.close() }
                        onOpenProfile()
                    }
                )

                DrawerItem(
                    label = "Orders",
                    icon = Icons.Filled.ReceiptLong,
                    selected = currentRoute == "orders",
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigate("orders")
                    }
                )

                DrawerItem(
                    label = "Analytics",
                    icon = Icons.Filled.BarChart,
                    selected = currentRoute == "analytics",
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigate("analytics")
                    }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                DrawerItem(
                    label = "Menu",
                    icon = Icons.Filled.RestaurantMenu,
                    selected = currentRoute == "menu_list",
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigate("menu_list")
                    }
                )

                DrawerItem(
                    label = "Categories",
                    icon = Icons.Filled.Category,
                    selected = currentRoute == "categories",
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigate("categories")
                    }
                )

                DrawerItem(
                    label = "Offers & Banners",
                    icon = Icons.Filled.Campaign,
                    selected = currentRoute == "offers_list",
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigate("offers_list")
                    }
                )

                DrawerItem(
                    label = "Coupons",
                    icon = Icons.Filled.LocalOffer,
                    selected = currentRoute == "coupons_list",
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigate("coupons_list")
                    }
                )

                DrawerItem(
                    label = "Events",
                    icon = Icons.Filled.Cake,
                    selected = currentRoute == "upcoming_events",
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigate("upcoming_events")
                    }
                )

                DrawerItem(
                    label = "Broadcast",
                    icon = Icons.Filled.Campaign,
                    selected = currentRoute == "broadcast",
                    onClick = {
                        scope.launch { drawerState.close() }
                        onNavigate("broadcast")
                    }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                DrawerItem(
                    label = "Profile",
                    icon = Icons.Filled.Person,
                    selected = currentRoute == "profile",
                    onClick = {
                        scope.launch { drawerState.close() }
                        onOpenProfile()
                    }
                )

            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                title,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = MaterialTheme.shapes.extraSmall,
                                    color = if (restaurantStatus.isOpen) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.errorContainer
                                    }
                                ) {
                                    Text(
                                        if (restaurantStatus.isOpen) "OPEN" else "CLOSED",
                                        modifier = Modifier.padding(
                                            horizontal = 7.dp,
                                            vertical = 3.dp
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (restaurantStatus.isOpen) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onErrorContainer
                                        }
                                    )
                                }

                                Spacer(Modifier.width(6.dp))

                                Text(
                                    if (restaurantStatus.isOpen) {
                                        "Accepting orders"
                                    } else {
                                        "Orders paused"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                scope.launch { drawerState.open() }
                            }
                        ) {
                            Icon(
                                Icons.Filled.Menu,
                                contentDescription = "Open menu"
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                onNavigate("shop_settings")
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = "Shop Settings"
                            )
                        }

                        IconButton(
                            onClick = onOpenOrders
                        ) {
                            Icon(
                                Icons.Filled.ReceiptLong,
                                contentDescription = "Orders"
                            )
                        }

                        androidx.compose.material3.Switch(
                            checked = restaurantStatus.isOpen,
                            onCheckedChange = onToggleRestaurant
                        )
                    }
                )
            }
        ) { padding ->
            content(padding)
        }
    }
}

@Composable
private fun DrawerHeader(
    isOpen: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            "Pizza Town",
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            "Restaurant control center",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Surface(
            shape = MaterialTheme.shapes.large,
            color = if (isOpen) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.errorContainer
            }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        if (isOpen) "Restaurant is open" else "Restaurant is closed",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        if (isOpen) "Customers can place orders."
                        else "Customers cannot checkout.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                androidx.compose.material3.Switch(
                    checked = isOpen,
                    onCheckedChange = onToggle
                )
            }
        }
    }
}

@Composable
private fun DrawerItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        label = { Text(label) },
        icon = {
            Icon(
                icon,
                contentDescription = null
            )
        },
        selected = selected,
        onClick = onClick,
        modifier = Modifier.padding(
            horizontal = 12.dp,
            vertical = 2.dp
        )
    )
}
