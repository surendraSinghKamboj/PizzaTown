package com.pizzatown.customer.presentation.cart

import com.pizzatown.customer.domain.model.CartItem
import com.pizzatown.customer.domain.repository.CartRepository
import com.pizzatown.customer.domain.usecase.CalculateCartTotalUseCase
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

private class FakeCartRepository(initial: List<CartItem> = emptyList()) : CartRepository {
    private val items = MutableStateFlow(initial)

    override fun observeCart(): Flow<List<CartItem>> = items

    override suspend fun addOrIncrement(item: CartItem) {
        items.value = items.value + item
    }

    override suspend fun updateQuantity(cartItemId: String, quantity: Int) {
        items.value = if (quantity <= 0) {
            items.value.filterNot { it.cartItemId == cartItemId }
        } else {
            items.value.map { if (it.cartItemId == cartItemId) it.copy(quantity = quantity) else it }
        }
    }

    override suspend fun removeItem(cartItemId: String) {
        items.value = items.value.filterNot { it.cartItemId == cartItemId }
    }

    override suspend fun clearCart() {
        items.value = emptyList()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class CartViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() { Dispatchers.setMain(testDispatcher) }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    private fun burger(quantity: Int = 1) = CartItem(
        cartItemId = "burger-double-cheese",
        menuItemId = "burger",
        menuItemName = "Classic Cheese Burger",
        imageUrl = "",
        selectedVariantId = "double",
        selectedVariantName = "Double",
        basePrice = 219.0,
        quantity = quantity
    )

    @Test
    fun `empty cart has zero totals`() = runTest {
        val viewModel = CartViewModel(FakeCartRepository(), CalculateCartTotalUseCase())
        val collectJob = launch { viewModel.cartData.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()

        val data = viewModel.cartData.value
        assertTrue(data.items.isEmpty())
        assertEquals(0.0, data.totals.grandTotal, 0.001)
        collectJob.cancel()
    }

    @Test
    fun `incrementing updates quantity and recalculates total`() = runTest {
        val repo = FakeCartRepository(listOf(burger(quantity = 1)))
        val viewModel = CartViewModel(repo, CalculateCartTotalUseCase())
        val collectJob = launch { viewModel.cartData.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.increment(viewModel.cartData.value.items.first())
        testDispatcher.scheduler.advanceUntilIdle()

        val data = viewModel.cartData.value
        assertEquals(2, data.items.first().quantity)
        assertEquals(438.0, data.totals.subtotal, 0.001) // 219 * 2
        collectJob.cancel()
    }

    @Test
    fun `decrementing to zero removes the item`() = runTest {
        val repo = FakeCartRepository(listOf(burger(quantity = 1)))
        val viewModel = CartViewModel(repo, CalculateCartTotalUseCase())
        val collectJob = launch { viewModel.cartData.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.decrement(viewModel.cartData.value.items.first())
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.cartData.value.items.isEmpty())
        collectJob.cancel()
    }

    @Test
    fun `clearCart empties the cart`() = runTest {
        val repo = FakeCartRepository(listOf(burger(quantity = 3)))
        val viewModel = CartViewModel(repo, CalculateCartTotalUseCase())
        val collectJob = launch { viewModel.cartData.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.clearCart()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.cartData.value.items.isEmpty())
        assertEquals(0.0, viewModel.cartData.value.totals.grandTotal, 0.001)
        collectJob.cancel()
    }
}
