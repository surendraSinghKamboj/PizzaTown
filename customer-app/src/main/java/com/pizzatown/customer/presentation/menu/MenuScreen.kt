package com.pizzatown.customer.presentation.menu

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.pizzatown.customer.R
import com.pizzatown.customer.core.common.UiState
import com.pizzatown.customer.domain.model.Category
import com.pizzatown.customer.domain.model.MenuItem
import com.pizzatown.customer.domain.model.PricingMode
import com.pizzatown.customer.presentation.components.BestsellerTag
import com.pizzatown.customer.presentation.components.DeliveryAddressBar
import com.pizzatown.customer.presentation.components.EmptyView
import com.pizzatown.customer.presentation.components.ErrorView
import com.pizzatown.customer.presentation.components.FadeSlideInItem
import com.pizzatown.customer.presentation.components.FavoriteHeartButton
import com.pizzatown.customer.presentation.components.QuantityStepper
import com.pizzatown.customer.presentation.components.RatingBadge
import com.pizzatown.customer.presentation.components.RestaurantClosedBanner
import com.pizzatown.customer.presentation.components.SectionHeader
import com.pizzatown.customer.presentation.components.SkeletonCard
import com.pizzatown.customer.presentation.components.SoldOutChip
import com.pizzatown.customer.presentation.notifications.NotificationInboxViewModel
import com.pizzatown.customer.presentation.offers.OffersCarousel
import com.pizzatown.customer.presentation.settings.ThemeSettingRow
import com.pizzatown.customer.ui.theme.Dimens
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Home / browse screen. Kept as a single route (MENU) per the existing
 * navigation architecture — this screen carries the "home" framing
 * (greeting, delivery address, restaurant status, quick menu) on top of
 * what used to be a plain menu grid, matching the approved reference
 * design, rather than introducing a second, near-duplicate bottom-nav
 * destination.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen(
    onItemClick: (String) -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenProfile: () -> Unit = {},
    onOpenAddAddress: () -> Unit = {},
    onOpenOrderHistory: () -> Unit = {},
    onLoggedOut: () -> Unit = {},
    viewModel: MenuViewModel = hiltViewModel(),
    notificationViewModel: NotificationInboxViewModel = hiltViewModel()
) {
    val state by viewModel.screenState.collectAsStateWithLifecycle()
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategoryId.collectAsStateWithLifecycle()
    val sortOption by viewModel.sortOption.collectAsStateWithLifecycle()
    val unreadCount by notificationViewModel.unreadCount.collectAsStateWithLifecycle()
    val firstName by viewModel.customerFirstName.collectAsStateWithLifecycle()
    val defaultAddress by viewModel.defaultAddress.collectAsStateWithLifecycle()
    val profileLoaded by viewModel.profileLoaded.collectAsStateWithLifecycle()
    val restaurantStatus by viewModel.restaurantStatus.collectAsStateWithLifecycle()

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshProfile()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    val cartQuantities by viewModel.simpleCartQuantities.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()

    var showQuickMenu by remember { mutableStateOf(false) }
    var showSortSheet by remember { mutableStateOf(false) }
    var showAllCategories by remember { mutableStateOf(false) }
    val gridState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()

    val successData = (state as? UiState.Success)?.data
    // Precomputed 0-based index of the "Menu" section header item, so
    // Best Sellers' "View all" can scroll straight to the full catalog
    // below it. Recomputed only when the pieces that shift it change.
    val menuSectionIndex = remember(successData?.bestsellers?.isNotEmpty(), restaurantStatus.isOpen, successData != null) {
        var idx = 3 // HomeHeader, GreetingRow, DeliveryAddressBar
        if (!restaurantStatus.isOpen) idx++
        idx += 2 // OffersCarousel, SearchBar
        if (successData != null) {
            idx += 2 // Categories header, CategoryRail
            if (successData.bestsellers.isNotEmpty()) idx += 2 // Best Sellers header, BestsellerRow
        }
        idx
    }

    Box(Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(
                start = Dimens.screenHPadding,
                end = Dimens.screenHPadding,
                top = Dimens.spaceS,
                bottom = Dimens.spaceS + WindowInsets.navigationBars.getBottom(LocalDensity.current).dp
            ),
            verticalArrangement = Arrangement.spacedBy(Dimens.spaceM),
            horizontalArrangement = Arrangement.spacedBy(Dimens.spaceM),
            modifier = Modifier.fillMaxSize()
        ) {
            fullWidth {
                HomeHeader(
                    unreadCount = unreadCount,
                    onOpenMenu = { showQuickMenu = true },
                    onOpenNotifications = onOpenNotifications
                )
            }

            fullWidth { GreetingRow(firstName = firstName) }

            fullWidth {
                DeliveryAddressBar(
                    label = defaultAddress?.label.orEmpty(),
                    addressLine =
                        defaultAddress?.fullAddress?.takeIf {
                            it.isNotBlank()
                        } ?: "Add a delivery address",
                    onClick = {
                        if (profileLoaded && defaultAddress == null) {
                            onOpenAddAddress()
                        } else {
                            onOpenProfile()
                        }
                    }
                )
            }

            if (!restaurantStatus.isOpen) {
                fullWidth { RestaurantClosedBanner() }
            }

            fullWidth { OffersCarousel() }

            fullWidth {
                SearchBar(
                    query = query,
                    onQueryChange = viewModel::onSearchQueryChange,
                    onFilterClick = { showSortSheet = true }
                )
            }

            when (val current = state) {
                is UiState.Loading -> {
                    items(6) { SkeletonCard(modifier = Modifier.height(180.dp)) }
                }
                is UiState.Error -> fullWidth { ErrorView(current.message) }
                is UiState.Empty -> fullWidth { EmptyView("The menu is empty right now. Please check back soon!") }
                is UiState.Success -> {
                    val data = current.data

                    fullWidth {
                        SectionHeader(
                            title = "Categories",
                            actionLabel = "View all",
                            onAction = { showAllCategories = true },
                            modifier = Modifier.padding(
                                top = Dimens.spaceM,
                                bottom = Dimens.spaceXS
                            )
                        )
                    }
                    fullWidth {
                        CategoryRail(
                            categories = data.categories,
                            selectedCategoryId = selectedCategory,
                            onSelect = viewModel::onCategorySelected
                        )
                    }

                    if (data.bestsellers.isNotEmpty()) {
                        fullWidth {
                            SectionHeader(
                                title = "Best Sellers",
                                actionLabel = "View all",
                                onAction = {
                                    coroutineScope.launch { gridState.animateScrollToItem(menuSectionIndex) }
                                },
                                modifier = Modifier.padding(
                                    top = Dimens.spaceL,
                                    bottom = Dimens.spaceS
                                )
                            )
                        }
                        fullWidth {
                            BestsellerRow(
                                items = data.bestsellers,
                                favoriteIds = favoriteIds,
                                cartQuantities = cartQuantities,
                                onClick = { onItemClick(it.id) },
                                onQuickAdd = { if (!viewModel.quickAdd(it)) onItemClick(it.id) },
                                onIncrement = { viewModel.increment(it) },
                                onDecrement = { viewModel.decrement(it) },
                                onToggleFavorite = { viewModel.toggleFavorite(it) }
                            )
                        }
                    }

                    fullWidth {
                        SectionHeader(
                            title = "Menu",
                            modifier = Modifier.padding(
                                top = Dimens.spaceL,
                                bottom = Dimens.spaceS
                            )
                        )
                    }

                    if (data.items.isEmpty()) {
                        fullWidth { EmptyView("No items match your search.") }
                    } else {
                        items(data.items, key = { it.id }) { item ->
                            FadeSlideInItem {
                                FoodCard(
                                    item = item,
                                    quantity = cartQuantities[item.id] ?: 0,
                                    isFavorite = favoriteIds.contains(item.id),
                                    onClick = { onItemClick(item.id) },
                                    onQuickAdd = { if (!viewModel.quickAdd(item)) onItemClick(item.id) },
                                    onIncrement = { viewModel.increment(item.id) },
                                    onDecrement = { viewModel.decrement(item.id) },
                                    onToggleFavorite = { viewModel.toggleFavorite(item.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showQuickMenu) {
        QuickMenuSheet(
            onDismiss = { showQuickMenu = false },
            onOpenProfile = { showQuickMenu = false; onOpenProfile() },
            onOpenOrders = { showQuickMenu = false; onOpenOrderHistory() },
            onOpenNotifications = { showQuickMenu = false; onOpenNotifications() },
            onLogout = { showQuickMenu = false; viewModel.logout(); onLoggedOut() }
        )
    }

    if (showSortSheet) {
        SortSheet(
            current = sortOption,
            onSelect = { viewModel.onSortSelected(it); showSortSheet = false },
            onDismiss = { showSortSheet = false }
        )
    }

    if (showAllCategories) {
        val categories = (state as? UiState.Success)?.data?.categories.orEmpty()
        AllCategoriesSheet(
            categories = categories,
            onSelect = { viewModel.onCategorySelected(it); showAllCategories = false },
            onDismiss = { showAllCategories = false }
        )
    }
}

/** Convenience for a grid item that should span both columns. */
private fun androidx.compose.foundation.lazy.grid.LazyGridScope.fullWidth(content: @Composable () -> Unit) {
    item(span = { GridItemSpan(maxLineSpan) }) { content() }
}

@Composable
private fun HomeHeader(unreadCount: Int, onOpenMenu: () -> Unit, onOpenNotifications: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = Dimens.spaceXS),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                .clickable(onClick = onOpenMenu),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Menu,
                contentDescription = "Menu",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(22.dp)
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.pizza_town_logo),
                contentDescription = null,
                modifier = Modifier.size(40.dp)
            )

            Spacer(Modifier.width(Dimens.spaceS))

            Text(
                "Pizza Town",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        BadgedBox(
            badge = {
                if (unreadCount > 0) {
                    Badge {
                        Text(unreadCount.toString())
                    }
                }
            }
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                    .clickable(onClick = onOpenNotifications),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Notifications,
                    contentDescription = "Notifications",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun GreetingRow(firstName: String) {
    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val timeOfDay = when {
        hour < 12 -> "morning"
        hour < 17 -> "afternoon"
        else -> "evening"
    }

    Column(
        modifier = Modifier.padding(
            top = Dimens.spaceS,
            bottom = Dimens.spaceXS
        )
    ) {
        Text(
            "Good $timeOfDay,",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (firstName.isNotBlank()) {
            Spacer(Modifier.height(2.dp))

            Text(
                "$firstName 👋",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SearchBar(query: String, onQueryChange: (String) -> Unit, onFilterClick: () -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = {
            Text(
                "Search pizza, burger, drinks...",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingIcon = {
            Icon(
                Icons.Filled.Search,
                contentDescription = "Search",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingIcon = {
            IconButton(onClick = onFilterClick) {
                Icon(
                    Icons.Filled.Tune,
                    contentDescription = "Sort & filter",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(18.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
    )
}

@Composable
private fun CategoryRail(
    categories: List<Category>,
    selectedCategoryId: String?,
    onSelect: (String?) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(Dimens.spaceM)) {
        item {
            com.pizzatown.customer.presentation.components.CategoryIconChip(
                label = "All",
                icon = Icons.Filled.Apps,
                selected = selectedCategoryId == null,
                onClick = { onSelect(null) }
            )
        }
        items(categories, key = { it.id }) { category ->
            com.pizzatown.customer.presentation.components.CategoryIconChip(
                label = category.name,
                icon = iconForCategory(category.name),
                selected = selectedCategoryId == category.id,
                onClick = { onSelect(category.id) }
            )
        }
    }
}

/**
 * Picks a representative icon purely for visual polish based on the
 * category's (dynamic, admin-entered) name. This never invents or hides
 * category data — every category from Firestore is always shown with its
 * real name; only the icon glyph next to it is chosen heuristically, and
 * falls back to a generic dish icon when nothing matches.
 */
private fun iconForCategory(name: String): ImageVector {
    val n = name.lowercase()
    return when {
        "pizza" in n -> Icons.Filled.LocalPizza
        "burger" in n -> Icons.Filled.LunchDining
        "drink" in n || "beverage" in n || "juice" in n -> Icons.Filled.LocalDrink
        "shake" in n || "coffee" in n || "tea" in n -> Icons.Filled.LocalCafe
        "dessert" in n || "ice" in n || "sweet" in n -> Icons.Filled.Icecream
        "side" in n || "fries" in n || "snack" in n -> Icons.Filled.Fastfood
        "combo" in n || "meal" in n -> Icons.Filled.RamenDining
        else -> Icons.Filled.RestaurantMenu
    }
}

@Composable
private fun BestsellerRow(
    items: List<MenuItem>,
    favoriteIds: Set<String>,
    cartQuantities: Map<String, Int>,
    onClick: (MenuItem) -> Unit,
    onQuickAdd: (MenuItem) -> Unit,
    onIncrement: (String) -> Unit,
    onDecrement: (String) -> Unit,
    onToggleFavorite: (String) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceM),
        contentPadding = PaddingValues(horizontal = Dimens.spaceXS)
    ) {
        items(items, key = { "bs-" + it.id }) { item ->
            Box(Modifier.width(184.dp)) {
                FoodCard(
                    item = item,
                    quantity = cartQuantities[item.id] ?: 0,
                    isFavorite = favoriteIds.contains(item.id),
                    onClick = { onClick(item) },
                    onQuickAdd = { onQuickAdd(item) },
                    onIncrement = { onIncrement(item.id) },
                    onDecrement = { onDecrement(item.id) },
                    onToggleFavorite = { onToggleFavorite(item.id) }
                )
            }
        }
    }
}

@Composable
private fun FoodCard(
    item: MenuItem,
    quantity: Int,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onQuickAdd: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 3.dp
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(156.dp)
                    .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
            ) {
                AsyncImage(
                    model = item.imageUrl.ifBlank { null },
                    contentDescription = item.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )

                FavoriteHeartButton(
                    isFavorite = isFavorite,
                    onToggle = onToggleFavorite,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(Dimens.spaceS)
                )
            }
            Column(
                Modifier.padding(
                    start = Dimens.spaceM,
                    end = Dimens.spaceM,
                    top = Dimens.spaceM,
                    bottom = Dimens.spaceS
                )
            ) {
                Text(
                    item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (item.rating != null || item.isBestseller) {
                    Spacer(Modifier.height(Dimens.spaceXS))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceS)
                    ) {
                        if (item.rating != null) {
                            RatingBadge(
                                rating = item.rating,
                                reviewCount = item.reviewCount
                            )
                        }
                        if (item.isBestseller) {
                            BestsellerTag()
                        }
                    }
                } else if (item.description.isNotBlank()) {
                    Spacer(Modifier.height(Dimens.spaceXS))
                    Text(
                        item.description,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(Dimens.spaceM))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val priceLabel = when (item.pricingMode) {
                        PricingMode.FIXED -> "₹${item.basePrice.toInt()}"
                        PricingMode.VARIANTS -> "From ₹${item.displayFromPrice().toInt()}"
                    }

                    Text(
                        priceLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    if (!item.available) {
                        SoldOutChip()
                    } else {
                        QuantityStepper(
                            quantity = quantity,
                            onIncrement = {
                                if (quantity == 0) onQuickAdd() else onIncrement()
                            },
                            onDecrement = onDecrement
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickMenuSheet(
    onDismiss: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenOrders: () -> Unit,
    onOpenNotifications: () -> Unit,
    onLogout: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = Dimens.spaceL).padding(bottom = Dimens.spaceXL)) {
            ThemeSettingRow()
            Spacer(Modifier.height(Dimens.spaceM))
            HorizontalDivider()
            ListItem(
                headlineContent = { Text("Profile") },
                leadingContent = { Icon(Icons.Filled.Person, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onOpenProfile)
            )
            ListItem(
                headlineContent = { Text("My Orders") },
                leadingContent = { Icon(Icons.Filled.Receipt, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onOpenOrders)
            )
            ListItem(
                headlineContent = { Text("Notifications") },
                leadingContent = { Icon(Icons.Filled.Notifications, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onOpenNotifications)
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text("Logout", color = MaterialTheme.colorScheme.error) },
                leadingContent = { Icon(Icons.Filled.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                modifier = Modifier.clickable(onClick = onLogout)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SortSheet(current: SortOption, onSelect: (SortOption) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = Dimens.spaceL).padding(bottom = Dimens.spaceXL)) {
            Text("Sort by", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(Dimens.spaceS))
            SortOption.entries.forEach { option ->
                ListItem(
                    headlineContent = { Text(option.label) },
                    trailingContent = { if (option == current) Icon(Icons.Filled.Check, contentDescription = null) },
                    modifier = Modifier.clickable { onSelect(option) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AllCategoriesSheet(categories: List<Category>, onSelect: (String?) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = Dimens.spaceL).padding(bottom = Dimens.spaceXL)) {
            Text("All Categories", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(Dimens.spaceS))
            ListItem(
                headlineContent = { Text("All") },
                leadingContent = { Icon(Icons.Filled.Apps, contentDescription = null) },
                modifier = Modifier.clickable { onSelect(null) }
            )
            categories.forEach { category ->
                ListItem(
                    headlineContent = { Text(category.name) },
                    leadingContent = { Icon(iconForCategory(category.name), contentDescription = null) },
                    modifier = Modifier.clickable { onSelect(category.id) }
                )
            }
        }
    }
}
