package com.pizzatown.admin.presentation.orders

import com.pizzatown.admin.core.common.UiState
import com.pizzatown.admin.domain.model.Order
import com.pizzatown.admin.domain.model.OrderStatus
import com.pizzatown.admin.domain.repository.OrderRepository
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

private class FakeOrderRepository(initialOrders: List<Order> = emptyList()) : OrderRepository {
    private val ordersFlow = MutableStateFlow(initialOrders)
    val updatedStatuses = mutableListOf<Pair<String, OrderStatus>>()

    override fun observeOrders(): Flow<List<Order>> = ordersFlow

    override suspend fun updateOrderStatus(orderId: String, status: OrderStatus): Result<Unit> {
        updatedStatuses.add(orderId to status)
        val updated = ordersFlow.value.map { if (it.orderId == orderId) it.copy(status = status) else it }
        ordersFlow.value = updated
        return Result.success(Unit)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class OrdersViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() { Dispatchers.setMain(testDispatcher) }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    private fun sampleOrders() = listOf(
        Order(orderId = "1", status = OrderStatus.PENDING),
        Order(orderId = "2", status = OrderStatus.CONFIRMED),
        Order(orderId = "3", status = OrderStatus.PENDING)
    )

    @Test
    fun `no filter shows all orders`() = runTest {
        val repo = FakeOrderRepository(sampleOrders())
        val viewModel = OrdersViewModel(repo)
        // ordersState is a WhileSubscribed StateFlow — it only starts
        // collecting the underlying repository flow once something
        // actually subscribes, so keep a collector alive for the test.
        val collectJob = launch { viewModel.ordersState.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.ordersState.value
        assertTrue(state is UiState.Success)
        assertEquals(3, (state as UiState.Success).data.size)
        collectJob.cancel()
    }

    @Test
    fun `filtering by status only shows matching orders`() = runTest {
        val repo = FakeOrderRepository(sampleOrders())
        val viewModel = OrdersViewModel(repo)
        val collectJob = launch { viewModel.ordersState.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.setStatusFilter(OrderStatus.PENDING)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.ordersState.value
        assertTrue(state is UiState.Success)
        val orders = (state as UiState.Success).data
        assertEquals(2, orders.size)
        assertTrue(orders.all { it.status == OrderStatus.PENDING })
        collectJob.cancel()
    }

    @Test
    fun `advancing status calls repository with next status in workflow`() = runTest {
        val repo = FakeOrderRepository(listOf(Order(orderId = "1", status = OrderStatus.PENDING)))
        val viewModel = OrdersViewModel(repo)
        val collectJob = launch { viewModel.ordersState.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.advanceStatus(Order(orderId = "1", status = OrderStatus.PENDING))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf("1" to OrderStatus.CONFIRMED), repo.updatedStatuses)
        collectJob.cancel()
    }

    @Test
    fun `cancelling sets status to cancelled regardless of current status`() = runTest {
        val repo = FakeOrderRepository(listOf(Order(orderId = "1", status = OrderStatus.PREPARING)))
        val viewModel = OrdersViewModel(repo)
        val collectJob = launch { viewModel.ordersState.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.cancelOrder(Order(orderId = "1", status = OrderStatus.PREPARING))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf("1" to OrderStatus.CANCELLED), repo.updatedStatuses)
        collectJob.cancel()
    }

    @Test
    fun `filtering to a status with no matches shows empty state`() = runTest {
        val repo = FakeOrderRepository(sampleOrders())
        val viewModel = OrdersViewModel(repo)
        val collectJob = launch { viewModel.ordersState.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.setStatusFilter(OrderStatus.CANCELLED)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.ordersState.value is UiState.Empty)
        collectJob.cancel()
    }
}
