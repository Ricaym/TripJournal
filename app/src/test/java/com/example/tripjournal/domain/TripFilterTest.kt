package com.example.tripjournal.domain

import com.example.tripjournal.data.database.entity.Trip
import org.junit.Assert.assertEquals
import org.junit.Test

class TripFilterTest {

    private val day = 24L * 60 * 60 * 1000
    private val today = 20_000L * day // un jour fixe, pour un test reproductible

    private fun trip(name: String, startOffset: Long, endOffset: Long) = Trip(
        name = name, destination = "Paris",
        startDate = today + startOffset * day, endDate = today + endOffset * day
    )

    private val past = trip("Passé", -10, -5)
    private val ongoing = trip("En cours", -1, 1)
    private val upcoming = trip("À venir", 3, 6)
    private val all = listOf(past, ongoing, upcoming)

    private fun filtered(filter: TripStatusFilter) =
        all.filter { TripFilter.matches(it, filter, today) }.map { it.name }

    @Test
    fun eachStatusFilterKeepsOnlyMatchingTrips() {
        assertEquals(listOf("Passé", "En cours", "À venir"), filtered(TripStatusFilter.ALL))
        assertEquals(listOf("À venir"), filtered(TripStatusFilter.UPCOMING))
        assertEquals(listOf("En cours"), filtered(TripStatusFilter.ONGOING))
        assertEquals(listOf("Passé"), filtered(TripStatusFilter.PAST))
    }

    @Test
    fun tripStartingOrEndingTodayIsOngoing() {
        val startsToday = trip("Départ", 0, 2)
        val endsToday = trip("Retour", -2, 0)
        assertEquals(true, TripFilter.matches(startsToday, TripStatusFilter.ONGOING, today))
        assertEquals(true, TripFilter.matches(endsToday, TripStatusFilter.ONGOING, today))
    }
}
