package com.example.tripjournal.domain

import com.example.tripjournal.data.database.entity.Trip

enum class TripStatusFilter { ALL, UPCOMING, ONGOING, PAST }

object TripFilter {

    private const val DAY_MS = 24L * 60 * 60 * 1000

    private fun day(millis: Long): Long = Math.floorDiv(millis, DAY_MS)

    /** Un voyage est « à venir » s'il commence après aujourd'hui, « terminé » s'il a fini avant, sinon « en cours ». */
    fun matches(trip: Trip, filter: TripStatusFilter, nowMillis: Long): Boolean {
        val today = day(nowMillis)
        return when (filter) {
            TripStatusFilter.ALL -> true
            TripStatusFilter.UPCOMING -> day(trip.startDate) > today
            TripStatusFilter.PAST -> day(trip.endDate) < today
            TripStatusFilter.ONGOING -> day(trip.startDate) <= today && day(trip.endDate) >= today
        }
    }
}
