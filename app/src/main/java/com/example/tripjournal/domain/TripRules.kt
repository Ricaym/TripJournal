package com.example.tripjournal.domain

import com.example.tripjournal.data.database.entity.Trip
import com.example.tripjournal.data.preferences.TripDateFormat
import com.example.tripjournal.data.preferences.TripSortOrder
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

object TripRules {
    fun hasValidDateRange(startDateUtcMillis: Long, endDateUtcMillis: Long): Boolean =
        startDateUtcMillis <= endDateUtcMillis

    fun sortTrips(trips: List<Trip>, order: TripSortOrder): List<Trip> = when (order) {
        TripSortOrder.NEWEST_FIRST -> trips.sortedWith(compareByDescending<Trip> { it.startDate }.thenBy { it.id })
        TripSortOrder.OLDEST_FIRST -> trips.sortedWith(compareBy<Trip> { it.startDate }.thenBy { it.id })
        TripSortOrder.NAME -> trips.sortedWith(compareBy<Trip> { it.name.lowercase() }.thenBy { it.id })
    }

    fun formatDateUtc(epochMillis: Long, format: TripDateFormat): String =
        DateTimeFormatter.ofPattern(format.pattern)
            .withZone(ZoneOffset.UTC)
            .format(Instant.ofEpochMilli(epochMillis))
}
