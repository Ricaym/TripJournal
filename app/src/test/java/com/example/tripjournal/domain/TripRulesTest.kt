package com.example.tripjournal.domain

import com.example.tripjournal.data.database.entity.Trip
import com.example.tripjournal.data.preferences.TripDateFormat
import com.example.tripjournal.data.preferences.TripSortOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TripRulesTest {
    @Test
    fun dateRangeAcceptsSameDayAndRejectsEndBeforeStart() {
        val day = 1_800_000_000_000L

        assertTrue(TripRules.hasValidDateRange(day, day))
        assertTrue(TripRules.hasValidDateRange(day, day + 86_400_000L))
        assertFalse(TripRules.hasValidDateRange(day + 1, day))
    }

    @Test
    fun preferencesControlTripSortingAndUtcDateDisplay() {
        val earlier = Trip(id = 1, name = "Zulu", destination = "Lyon", startDate = 100, endDate = 200)
        val later = Trip(id = 2, name = "alpha", destination = "Rome", startDate = 300, endDate = 400)

        assertEquals(listOf(later, earlier), TripRules.sortTrips(listOf(earlier, later), TripSortOrder.NEWEST_FIRST))
        assertEquals(listOf(later, earlier), TripRules.sortTrips(listOf(earlier, later), TripSortOrder.NAME))
        assertEquals("01/01/1970", TripRules.formatDateUtc(0, TripDateFormat.DAY_MONTH_YEAR))
    }
}
