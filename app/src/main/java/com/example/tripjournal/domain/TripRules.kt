package com.example.tripjournal.domain

import com.example.tripjournal.data.database.entity.Trip
import com.example.tripjournal.data.preferences.TripDateFormat
import com.example.tripjournal.data.preferences.TripSortOrder
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

object TripRules {
    private const val DAY_MS = 24L * 60 * 60 * 1000

    /** Numéro du jour UTC correspondant à une date en millisecondes. */
    private fun day(millis: Long): Long = Math.floorDiv(millis, DAY_MS)

    /** Un voyage doit avoir un nom, une destination et une fin qui n'est pas avant le début. */
    fun isValid(trip: Trip): Boolean =
        trip.name.isNotBlank() &&
            trip.destination.isNotBlank() &&
            day(trip.endDate) >= day(trip.startDate)

    /** Doublon : même nom (sans tenir compte des majuscules) et même jour de départ. */
    fun isDuplicate(trip: Trip, existing: List<Trip>): Boolean =
        existing.any {
            (trip.id == 0L || it.id != trip.id) &&
                it.name.trim().equals(trip.name.trim(), ignoreCase = true) &&
                day(it.startDate) == day(trip.startDate)
        }

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
