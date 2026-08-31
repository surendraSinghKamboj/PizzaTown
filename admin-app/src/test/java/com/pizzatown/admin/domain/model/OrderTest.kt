package com.pizzatown.admin.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderTest {

    private fun orderWith(status: OrderStatus) = Order(orderId = "1", status = status)

    @Test
    fun `pending order advances to confirmed`() {
        assertEquals(OrderStatus.CONFIRMED, orderWith(OrderStatus.PENDING).nextStatus())
    }

    @Test
    fun `full happy path progresses through every stage in order`() {
        var status = OrderStatus.PENDING
        val expectedSequence = listOf(
            OrderStatus.CONFIRMED, OrderStatus.PREPARING, OrderStatus.READY, OrderStatus.COMPLETED
        )
        for (expected in expectedSequence) {
            val next = orderWith(status).nextStatus()
            assertEquals(expected, next)
            status = next!!
        }
    }

    @Test
    fun `completed order has no next status`() {
        assertNull(orderWith(OrderStatus.COMPLETED).nextStatus())
    }

    @Test
    fun `cancelled order has no next status`() {
        assertNull(orderWith(OrderStatus.CANCELLED).nextStatus())
    }

    @Test
    fun `can cancel any order except completed or already cancelled`() {
        assertTrue(orderWith(OrderStatus.PENDING).canCancel())
        assertTrue(orderWith(OrderStatus.CONFIRMED).canCancel())
        assertTrue(orderWith(OrderStatus.PREPARING).canCancel())
        assertTrue(orderWith(OrderStatus.READY).canCancel())
        assertFalse(orderWith(OrderStatus.COMPLETED).canCancel())
        assertFalse(orderWith(OrderStatus.CANCELLED).canCancel())
    }
}
