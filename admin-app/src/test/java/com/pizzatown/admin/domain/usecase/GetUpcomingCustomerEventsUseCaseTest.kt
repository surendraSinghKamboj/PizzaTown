package com.pizzatown.admin.domain.usecase

import com.pizzatown.admin.domain.model.Customer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class GetUpcomingCustomerEventsUseCaseTest {

    private val useCase = GetUpcomingCustomerEventsUseCase()
    private val today = LocalDate.of(2026, 3, 15)

    private fun epochOf(year: Int, month: Int, day: Int): Long =
        LocalDate.of(year, month, day).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    @Test
    fun `birthday today is included with zero days until`() {
        val customer = Customer(userId = "1", fullName = "Alice", dateOfBirth = epochOf(1995, 3, 15))
        val events = useCase(listOf(customer), windowDays = 30, today = today)

        assertEquals(1, events.size)
        assertEquals(0, events.first().daysUntil)
        assertEquals(CustomerEventType.BIRTHDAY, events.first().type)
    }

    @Test
    fun `birthday next month within window is included`() {
        val customer = Customer(userId = "1", fullName = "Bob", dateOfBirth = epochOf(1990, 3, 25))
        val events = useCase(listOf(customer), windowDays = 30, today = today)

        assertEquals(1, events.size)
        assertEquals(10, events.first().daysUntil)
    }

    @Test
    fun `birthday outside window is excluded`() {
        val customer = Customer(userId = "1", fullName = "Carol", dateOfBirth = epochOf(1990, 6, 1))
        val events = useCase(listOf(customer), windowDays = 30, today = today)

        assertTrue(events.isEmpty())
    }

    @Test
    fun `birthday that already passed this year rolls over to next year`() {
        // Today is March 15; a Jan 1 birthday already passed this year,
        // so the next occurrence should be Jan 1 of next year (far outside a 30-day window).
        val customer = Customer(userId = "1", fullName = "Dave", dateOfBirth = epochOf(1988, 1, 1))
        val events = useCase(listOf(customer), windowDays = 30, today = today)

        assertTrue(events.isEmpty())
    }

    @Test
    fun `year wraparound is handled - late December date near year end`() {
        val nearYearEnd = LocalDate.of(2026, 12, 28)
        val customer = Customer(userId = "1", fullName = "Eve", dateOfBirth = epochOf(1992, 1, 3))
        val events = useCase(listOf(customer), windowDays = 30, today = nearYearEnd)

        assertEquals(1, events.size)
        assertEquals(6, events.first().daysUntil) // Dec 28 -> Jan 3 = 6 days
    }

    @Test
    fun `customer with no dates set produces no events`() {
        val customer = Customer(userId = "1", fullName = "Frank")
        val events = useCase(listOf(customer), windowDays = 30, today = today)

        assertTrue(events.isEmpty())
    }

    @Test
    fun `both birthday and anniversary within window produce two events`() {
        val customer = Customer(
            userId = "1", fullName = "Grace",
            dateOfBirth = epochOf(1995, 3, 20),
            anniversaryDate = epochOf(2015, 3, 18)
        )
        val events = useCase(listOf(customer), windowDays = 30, today = today)

        assertEquals(2, events.size)
        // sorted soonest-first: anniversary (3 days) before birthday (5 days)
        assertEquals(CustomerEventType.ANNIVERSARY, events.first().type)
        assertEquals(3, events.first().daysUntil)
    }

    @Test
    fun `results are sorted soonest first across multiple customers`() {
        val customers = listOf(
            Customer(userId = "1", fullName = "Far", dateOfBirth = epochOf(1990, 4, 10)),
            Customer(userId = "2", fullName = "Near", dateOfBirth = epochOf(1990, 3, 16))
        )
        val events = useCase(customers, windowDays = 30, today = today)

        assertEquals("Near", events.first().customer.fullName)
    }
}
