package com.pizzatown.admin.domain.usecase

import com.pizzatown.admin.domain.model.Customer
import java.time.Instant
import java.time.LocalDate
import java.time.MonthDay
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject

enum class CustomerEventType { BIRTHDAY, ANNIVERSARY }

data class UpcomingCustomerEvent(
    val customer: Customer,
    val type: CustomerEventType,
    val daysUntil: Int,
    val occurrenceDate: LocalDate
)

/**
 * Finds customers whose birthday or anniversary falls within the next
 * [windowDays] days, ignoring the year (only month/day matters), sorted
 * soonest-first. Lets the seller proactively reach out with a birthday/
 * anniversary offer — see spec item #6/#7.
 */
class GetUpcomingCustomerEventsUseCase @Inject constructor() {

    operator fun invoke(
        customers: List<Customer>,
        windowDays: Int = 30,
        today: LocalDate = LocalDate.now()
    ): List<UpcomingCustomerEvent> {
        val events = mutableListOf<UpcomingCustomerEvent>()

        for (customer in customers) {
            nextOccurrence(customer.dateOfBirth, today)?.let { occurrence ->
                val days = ChronoUnit.DAYS.between(today, occurrence).toInt()
                if (days in 0..windowDays) {
                    events += UpcomingCustomerEvent(customer, CustomerEventType.BIRTHDAY, days, occurrence)
                }
            }
            nextOccurrence(customer.anniversaryDate, today)?.let { occurrence ->
                val days = ChronoUnit.DAYS.between(today, occurrence).toInt()
                if (days in 0..windowDays) {
                    events += UpcomingCustomerEvent(customer, CustomerEventType.ANNIVERSARY, days, occurrence)
                }
            }
        }

        return events.sortedBy { it.daysUntil }
    }

    /** Next calendar occurrence (this year or next) of the month/day encoded in [epochMillis], or null if unset. */
    private fun nextOccurrence(epochMillis: Long, today: LocalDate): LocalDate? {
        if (epochMillis <= 0L) return null
        val original = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        val monthDay = MonthDay.of(original.month, original.dayOfMonth)
        var candidate = monthDay.atYear(today.year)
        if (candidate.isBefore(today)) {
            candidate = monthDay.atYear(today.year + 1)
        }
        return candidate
    }
}
