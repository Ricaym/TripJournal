package com.example.tripjournal.domain

import com.example.tripjournal.data.database.dao.VisitWithPlace
import com.example.tripjournal.data.database.entity.Place
import com.example.tripjournal.data.database.entity.Trip
import com.example.tripjournal.data.database.entity.Visit

/**
 * Règles métier des lieux, visites et itinéraires.
 * Logique pure, testable sans Android.
 * Convention : les dates sont des millisecondes UTC (comme celles renvoyées par MaterialDatePicker).
 */
object VisitRules {
    private const val DAY_MS = 24L * 60 * 60 * 1000

    /** Numéro du jour (UTC) correspondant à une date. Les heures sont ignorées. */
    private fun epochDay(millis: Long): Long = Math.floorDiv(millis, DAY_MS)

    /** Cohérence : une visite doit avoir lieu pendant le voyage (premier et dernier jour inclus). */
    fun isWithinTrip(visitDate: Long, trip: Trip): Boolean =
        epochDay(visitDate) in epochDay(trip.startDate)..epochDay(trip.endDate)

    /** Itinéraire : Jour 1 = premier jour du voyage. */
    fun dayNumber(visitDate: Long, trip: Trip): Int =
        (epochDay(visitDate) - epochDay(trip.startDate)).toInt() + 1

    /** Itinéraire : visites regroupées par jour, et triées par heure dans chaque jour. */
    fun buildItinerary(visits: List<VisitWithPlace>, trip: Trip): Map<Int, List<VisitWithPlace>> =
        visits.sortedWith(compareBy({ it.visit.visitDate }, { it.visit.id }))
            .groupBy { dayNumber(it.visit.visitDate, trip) }
            .toSortedMap()

    /** Cohérence : visites qui ne tombent plus dans les dates du voyage (après une modification du voyage). */
    fun visitsOutsideTrip(visits: List<Visit>, trip: Trip): List<Visit> =
        visits.filterNot { isWithinTrip(it.visitDate, trip) }

    /** Validation du formulaire d'un lieu. */
    fun isValidPlace(place: Place): Boolean =
        place.name.isNotBlank() && place.city.isNotBlank() && place.country.isNotBlank()
}

