package com.pizzatown.customer.presentation.notifications

import com.pizzatown.customer.core.preferences.NotificationPreferences
import com.pizzatown.customer.domain.model.Broadcast
import com.pizzatown.customer.domain.repository.BroadcastRepository
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
import org.junit.Before
import org.junit.Test

private class FakeBroadcastRepository(initial: List<Broadcast> = emptyList()) : BroadcastRepository {
    val flow = MutableStateFlow(initial)
    override fun observeMyBroadcasts(): Flow<List<Broadcast>> = flow
}

private class FakeNotificationPreferences(initialLastSeen: Long = 0L) : NotificationPreferences {
    private val lastSeenFlow = MutableStateFlow(initialLastSeen)
    override val lastSeenAt: Flow<Long> = lastSeenFlow
    var markSeenCalls = 0
        private set

    override suspend fun markSeenNow() {
        markSeenCalls++
        lastSeenFlow.value = Long.MAX_VALUE // "now" — far enough ahead for test purposes
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationInboxViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() { Dispatchers.setMain(testDispatcher) }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `unread count is zero when all messages are older than last seen`() = runTest {
        val broadcastRepo = FakeBroadcastRepository(
            listOf(Broadcast(id = "1", title = "Old offer", message = "...", createdAt = 1000L))
        )
        val prefs = FakeNotificationPreferences(initialLastSeen = 5000L)
        val viewModel = NotificationInboxViewModel(broadcastRepo, prefs)
        val job = launch { viewModel.unreadCount.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(0, viewModel.unreadCount.value)
        job.cancel()
    }

    @Test
    fun `unread count reflects messages newer than last seen`() = runTest {
        val broadcastRepo = FakeBroadcastRepository(
            listOf(
                Broadcast(id = "1", title = "Old", message = "...", createdAt = 1000L),
                Broadcast(id = "2", title = "New", message = "...", createdAt = 9000L)
            )
        )
        val prefs = FakeNotificationPreferences(initialLastSeen = 5000L)
        val viewModel = NotificationInboxViewModel(broadcastRepo, prefs)
        val job = launch { viewModel.unreadCount.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.unreadCount.value)
        job.cancel()
    }

    @Test
    fun `markSeen resets unread count to zero`() = runTest {
        val broadcastRepo = FakeBroadcastRepository(
            listOf(Broadcast(id = "1", title = "New", message = "...", createdAt = 9000L))
        )
        val prefs = FakeNotificationPreferences(initialLastSeen = 0L)
        val viewModel = NotificationInboxViewModel(broadcastRepo, prefs)
        val job = launch { viewModel.unreadCount.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, viewModel.unreadCount.value)

        viewModel.markSeen()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(0, viewModel.unreadCount.value)
        assertEquals(1, prefs.markSeenCalls)
        job.cancel()
    }

    @Test
    fun `broadcast targeted to a specific customer is treated the same as a general one once it reaches this user`() = runTest {
        // Firestore security rules already ensure only the right user's
        // listener receives a targeted broadcast — by the time it's in
        // this repository's flow, the ViewModel treats it identically to
        // an all-customers broadcast.
        val broadcastRepo = FakeBroadcastRepository(
            listOf(Broadcast(id = "1", title = "Happy Birthday!", message = "20% off today", targetUserId = "user-42", createdAt = 9000L))
        )
        val prefs = FakeNotificationPreferences(initialLastSeen = 0L)
        val viewModel = NotificationInboxViewModel(broadcastRepo, prefs)
        val job = launch { viewModel.unreadCount.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.unreadCount.value)
        job.cancel()
    }
}
