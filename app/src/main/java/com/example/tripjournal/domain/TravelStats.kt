package com.example.tripjournal.domain

import com.example.tripjournal.data.database.dao.VisitWithPlace
import com.example.tripjournal.data.database.entity.Trip

/** Un libellé et le nombre d'occurrences associé (pays, lieu…). */
data class RankedItem(val label: String, val count: Int)

/** Statistiques de voyage affichées à l'écran Statistiques. */
data class TravelStats(
    val totalTrips: Int,
    val totalPlaces: Int,
    val totalVisits: Int,
    /** Visites par voyage, arrondi à une décimale. */
    val averageVisitsPerTrip: Double,
    /** Somme des durées des voyages, en jours (premier et dernier jour inclus). */
    val totalTravelDays: Int,
    val longestTrip: Trip?,
    val longestTripDays: Int,
    val upcomingTrips: Int,
    val ongoingTrips: Int,
    val pastTrips: Int,
    val favoriteTrips: Int,
    /** Pays les plus fréquents parmi les destinations des voyages (top 3). */
    val topCountries: List<RankedItem>,
    /** Lieu qui revient le plus dans les visites, s'il y en a. */
    val mostVisitedPlace: RankedItem?
) {
    val isEmpty: Boolean get() = totalTrips == 0
}

/**
 * Calcul des statistiques de voyage.
 * Logique pure, testable sans Android.
 * Convention : dates en millisecondes UTC, comparées au jour près (comme [VisitRules]).
 */
object TravelStatsCalculator {
    private const val DAY_MS = 24L * 60 * 60 * 1000
    private const val TOP_COUNT = 3

    private fun epochDay(millis: Long): Long = Math.floorDiv(millis, DAY_MS)

    /** Durée d'un voyage en jours, premier et dernier jour inclus (minimum 1). */
    fun durationInDays(trip: Trip): Int =
        (epochDay(trip.endDate) - epochDay(trip.startDate) + 1).toInt().coerceAtLeast(1)

    /** Pays d'une destination « Ville, Pays » ; la destination entière s'il n'y a pas de virgule. */
    fun countryOf(destination: String): String =
        destination.substringAfterLast(',').trim().ifEmpty { destination.trim() }

    fun compute(
        trips: List<Trip>,
        visits: List<VisitWithPlace>,
        placeCount: Int,
        now: Long
    ): TravelStats {
        val today = epochDay(now)
        val upcoming = trips.count { epochDay(it.startDate) > today }
        val past = trips.count { epochDay(it.endDate) < today }
        val ongoing = trips.size - upcoming - past

        // À durée égale, le voyage le plus ancien l'emporte : résultat stable.
        val longest = trips.maxWithOrNull(
            compareBy<Trip> { durationInDays(it) }.thenByDescending { it.startDate }
        )

        val average = if (trips.isEmpty()) 0.0
        else Math.round(visits.size * 10.0 / trips.size) / 10.0

        return TravelStats(
            totalTrips = trips.size,
            totalPlaces = placeCount,
            totalVisits = visits.size,
            averageVisitsPerTrip = average,
            totalTravelDays = trips.sumOf { durationInDays(it) },
            longestTrip = longest,
            longestTripDays = longest?.let { durationInDays(it) } ?: 0,
            upcomingTrips = upcoming,
            ongoingTrips = ongoing,
            pastTrips = past,
            favoriteTrips = trips.count { it.isFavorite },
            topCountries = rank(trips.map { countryOf(it.destination) }).take(TOP_COUNT),
            mostVisitedPlace = rank(visits.map { it.place.name }).firstOrNull()
        )
    }

    /** Compte les occurrences, triées par fréquence décroissante puis par ordre alphabétique. */
    private fun rank(labels: List<String>): List<RankedItem> =
        labels.filter { it.isNotBlank() }
            .groupingBy { it }
            .eachCount()
            .map { (label, count) -> RankedItem(label, count) }
            .sortedWith(compareByDescending<RankedItem> { it.count }.thenBy { it.label.lowercase() })
}
