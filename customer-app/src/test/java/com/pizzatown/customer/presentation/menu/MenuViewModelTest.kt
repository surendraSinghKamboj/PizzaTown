package com.pizzatown.customer.presentation.menu

import com.pizzatown.customer.core.common.UiState
import com.pizzatown.customer.domain.model.Category
import com.pizzatown.customer.domain.model.CartItem
import com.pizzatown.customer.domain.model.DeliveryArea
import com.pizzatown.customer.domain.model.MenuItem
import com.pizzatown.customer.domain.model.RestaurantStatus
import com.pizzatown.customer.domain.model.UserProfile
import com.pizzatown.customer.domain.repository.AuthRepository
import com.pizzatown.customer.domain.repository.AuthResult
import com.pizzatown.customer.domain.repository.CartRepository
import com.pizzatown.customer.domain.repository.CategoryRepository
import com.pizzatown.customer.domain.repository.FavoriteRepository
import com.pizzatown.customer.domain.repository.MenuRepository
import com.pizzatown.customer.domain.repository.ProfileRepository
import com.pizzatown.customer.domain.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class FakeMenuRepository(items: List<MenuItem>) : MenuRepository {
    private val flow = MutableStateFlow(items)
    override fun observeMenuItems(): Flow<List<MenuItem>> = flow
    override suspend fun getMenuItem(id: String): Result<MenuItem> = Result.failure(NotImplementedError())
    override suspend fun refreshMenuItem(id: String): Result<MenuItem> = Result.failure(NotImplementedError())
}

private class FakeCategoryRepository(categories: List<Category>) : CategoryRepository {
    private val flow = MutableStateFlow(categories)
    override fun observeCategories(): Flow<List<Category>> = flow
}

private class FakeCartRepository : CartRepository {
    private val flow = MutableStateFlow<List<CartItem>>(emptyList())
    override fun observeCart(): Flow<List<CartItem>> = flow
    override suspend fun addOrIncrement(item: CartItem) { flow.value = flow.value + item }
    override suspend fun updateQuantity(cartItemId: String, quantity: Int) {}
    override suspend fun removeItem(cartItemId: String) {}
    override suspend fun clearCart() { flow.value = emptyList() }
}

private class FakeProfileRepository : ProfileRepository {
    override suspend fun getProfile(userId: String): Result<UserProfile> = Result.failure(NotImplementedError())
    override suspend fun updateProfile(profile: UserProfile): Result<Unit> = Result.success(Unit)
    override suspend fun uploadProfileImage(userId: String, imageBytes: ByteArray): Result<String> = Result.success("")
}

private class FakeAuthRepository : AuthRepository {
    override val isSignedIn: Flow<Boolean> = MutableStateFlow(true)
    override val currentUserId: String? = null
    override suspend fun login(email: String, password: String): AuthResult = AuthResult.Success
    override suspend fun register(fullName: String, mobile: String, email: String, password: String): AuthResult = AuthResult.Success
    override suspend fun sendPasswordReset(email: String): AuthResult = AuthResult.Success
    override fun logout() {}
}

private class FakeSettingsRepository : SettingsRepository {
    override fun observeRestaurantStatus(): Flow<RestaurantStatus> = MutableStateFlow(RestaurantStatus(isOpen = true))
    override fun observeDeliveryArea(): Flow<DeliveryArea> = MutableStateFlow(DeliveryArea())
}

private class FakeFavoriteRepository : FavoriteRepository {
    private val flow = MutableStateFlow<Set<String>>(emptySet())
    override fun observeFavoriteIds(): Flow<Set<String>> = flow
    override suspend fun toggleFavorite(menuItemId: String, currentlyFavorite: Boolean) {
        flow.value = if (currentlyFavorite) flow.value - menuItemId else flow.value + menuItemId
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MenuViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() { Dispatchers.setMain(testDispatcher) }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    private val pizzaCategory = Category(id = "pizza", name = "Pizza")
    private val drinksCategory = Category(id = "drinks", name = "Drinks")

    private fun sampleItems() = listOf(
        MenuItem(id = "1", name = "Margherita Pizza", description = "Classic cheese pizza", categoryId = "pizza"),
        MenuItem(id = "2", name = "Pepperoni Pizza", description = "Spicy pepperoni", categoryId = "pizza"),
        MenuItem(id = "3", name = "Coca Cola", description = "Chilled cold drink", categoryId = "drinks")
    )

    private fun buildViewModel(items: List<MenuItem>, categories: List<Category>) = MenuViewModel(
        menuRepository = FakeMenuRepository(items),
        categoryRepository = FakeCategoryRepository(categories),
        cartRepository = FakeCartRepository(),
        profileRepository = FakeProfileRepository(),
        authRepository = FakeAuthRepository(),
        favoriteRepository = FakeFavoriteRepository(),
        settingsRepository = FakeSettingsRepository()
    )

    @Test
    fun `no filters shows every item`() = runTest {
        val viewModel = buildViewModel(sampleItems(), listOf(pizzaCategory, drinksCategory))
        val job = launch { viewModel.screenState.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.screenState.value
        assertTrue(state is UiState.Success)
        assertEquals(3, (state as UiState.Success).data.items.size)
        job.cancel()
    }

    @Test
    fun `search query filters by name case-insensitively`() = runTest {
        val viewModel = buildViewModel(sampleItems(), listOf(pizzaCategory, drinksCategory))
        val job = launch { viewModel.screenState.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onSearchQueryChange("pepperoni")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.screenState.value as UiState.Success
        assertEquals(1, state.data.items.size)
        assertEquals("Pepperoni Pizza", state.data.items.first().name)
        job.cancel()
    }

    @Test
    fun `search query also matches description`() = runTest {
        val viewModel = buildViewModel(sampleItems(), listOf(pizzaCategory, drinksCategory))
        val job = launch { viewModel.screenState.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onSearchQueryChange("chilled")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.screenState.value as UiState.Success
        assertEquals(1, state.data.items.size)
        assertEquals("Coca Cola", state.data.items.first().name)
        job.cancel()
    }

    @Test
    fun `category filter narrows to that category only`() = runTest {
        val viewModel = buildViewModel(sampleItems(), listOf(pizzaCategory, drinksCategory))
        val job = launch { viewModel.screenState.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onCategorySelected("drinks")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.screenState.value as UiState.Success
        assertEquals(1, state.data.items.size)
        assertEquals("drinks", state.data.items.first().categoryId)
        job.cancel()
    }

    @Test
    fun `search and category filter combine`() = runTest {
        val viewModel = buildViewModel(sampleItems(), listOf(pizzaCategory, drinksCategory))
        val job = launch { viewModel.screenState.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onCategorySelected("pizza")
        viewModel.onSearchQueryChange("margherita")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.screenState.value as UiState.Success
        assertEquals(1, state.data.items.size)
        assertEquals("Margherita Pizza", state.data.items.first().name)
        job.cancel()
    }

    @Test
    fun `sorting by price low to high orders items ascending`() = runTest {
        val items = listOf(
            MenuItem(id = "1", name = "Expensive", categoryId = "pizza", basePrice = 500.0),
            MenuItem(id = "2", name = "Cheap", categoryId = "pizza", basePrice = 100.0)
        )
        val viewModel = buildViewModel(items, listOf(pizzaCategory))
        val job = launch { viewModel.screenState.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onSortSelected(SortOption.PRICE_LOW_HIGH)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.screenState.value as UiState.Success
        assertEquals("Cheap", state.data.items.first().name)
        job.cancel()
    }
}
