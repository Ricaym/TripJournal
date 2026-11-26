package com.example.tripjournal.ui.trips

import com.example.tripjournal.data.database.dao.VisitWithPlace
import com.example.tripjournal.data.database.entity.Trip
import com.example.tripjournal.domain.VisitRules

sealed class ItineraryRow {
    data class DayHeader(val day: Int, val date: Long) : ItineraryRow()
    data class VisitItem(val item: VisitWithPlace) : ItineraryRow()

    companion object {
        fun build(visits: List<VisitWithPlace>, trip: Trip): List<ItineraryRow> =
            VisitRules.buildItinerary(
                visits.filter { VisitRules.isWithinTrip(it.visit.visitDate, trip) }, trip
            ).flatMap { (day, dayVisits) ->
                listOf(DayHeader(day, dayVisits.first().visit.visitDate)) +
                        dayVisits.map { VisitItem(it) }
            }
    }
}