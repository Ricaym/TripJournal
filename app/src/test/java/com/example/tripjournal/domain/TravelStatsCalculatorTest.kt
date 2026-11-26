package com.example.tripjournal.domain

import com.example.tripjournal.data.database.dao.VisitWithPlace
import com.example.tripjournal.data.database.entity.Place
import com.example.tripjournal.data.database.entity.Trip
import com.example.tripjournal.data.database.entity.Visit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class TravelStatsCalculatorTest {

    /** Date UTC en millisecondes, avec une heure facultative. */
    private fun day(year: Int, month: Int, dayOfMonth: Int, hour: Int = 0): Long =
        LocalDate.of(year, month, dayOfMonth).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() +
                hour * 3_600_000L

    // « Aujourd'hui » fixé pour que les tests ne dépendent pas de la date réelle.
    private val now = day(2026, 7, 11, hour = 15)

    private val pastTrip = Trip(id = 1, name = "Barcelone", destination = "Barcelone, Espagne",
        startDate = day(2026, 4, 12), endDate = day(2026, 4, 16))                  // 5 jours, terminé
    private val ongoingTrip = Trip(id = 2, name = "Lisbonne", destination = "Lisbonne, Portugal",
        startDate = day(2026, 7, 10), endDate = day(2026, 7, 12), isFavorite = true) // 3 jours, en cours
    private val futureTrip = Trip(id = 3, name = "Madrid", destination = "Madrid, Espagne",
        startDate = day(2026, 9, 1), endDate = day(2026, 9, 10))                    // 10 jours, à venir

    private val tower = Place(id = 1, name = "Tour de Belém", city = "Lisbonne", country = "Portugal")
    private val museum = Place(id = 2, name = "Musée des Azulejos", city = "Lisbonne", country = "Portugal")

    private fun visit(id: Long, trip: Trip, place: Place) =
        VisitWithPlace(Visit(id = id, tripId = trip.id, placeId = place.id, visitDate = trip.startDate), place)

    private val trips = listOf(pastTrip, ongoingTrip, futureTrip)

    @Test fun emptyJournalGivesZeroStatsWithoutCrashing() {
        val stats = TravelStatsCalculator.compute(emptyList(), emptyList(), placeCount = 0, now = now)

        assertTrue(stats.isEmpty)
        assertEquals(0.0, stats.averageVisitsPerTrip, 0.0)   // pas de division par zéro
        assertNull(stats.longestTrip)
        assertNull(stats.mostVisitedPlace)
        assertTrue(stats.topCountries.isEmpty())
    }

    @Test fun durationCountsFirstAndLastDayAndIgnoresHours() {
        val oneDay = Trip(name = "Aller-retour", destination = "Lyon, France",
            startDate = day(2026, 5, 1, hour = 8), endDate = day(2026, 5, 1, hour = 22))
        assertEquals(1, TravelStatsCalculator.durationInDays(oneDay))
        assertEquals(5, TravelStatsCalculator.durationInDays(pastTrip))
    }

    @Test fun totalDaysAndLongestTripAreComputedFromAllTrips() {
        val stats = TravelStatsCalculator.compute(trips, emptyList(), placeCount = 0, now = now)

        assertEquals(5 + 3 + 10, stats.totalTravelDays)
        assertEquals(futureTrip, stats.longestTrip)
        assertEquals(10, stats.longestTripDays)
    }

    @Test fun tripsAreClassifiedRelativeToToday() {
        val stats = TravelStatsCalculator.compute(trips, emptyList(), placeCount = 0, now = now)

        assertEquals(1, stats.pastTrips)
        assertEquals(1, stats.ongoingTrips)     // le voyage qui contient « aujourd'hui »
        assertEquals(1, stats.upcomingTrips)
        assertEquals(1, stats.favoriteTrips)
    }

    @Test fun countryIsTakenAfterTheLastComma() {
        assertEquals("Espagne", TravelStatsCalculator.countryOf("Barcelone, Espagne"))
        assertEquals("Portugal", TravelStatsCalculator.countryOf("Portugal"))
        assertEquals("Japon", TravelStatsCalculator.countryOf("Kyoto, Kansai, Japon "))
    }

    @Test fun topCountriesAreRankedByNumberOfTrips() {
        val stats = TravelStatsCalculator.compute(trips, emptyList(), placeCount = 0, now = now)

        assertEquals(RankedItem("Espagne", 2), stats.topCountries[0])
        assertEquals(RankedItem("Portugal", 1), stats.topCountries[1])
    }

    @Test fun visitStatsUseAverageAndMostVisitedPlace() {
        val visits = listOf(
            visit(1, ongoingTrip, tower),
            visit(2, ongoingTrip, museum),
            visit(3, pastTrip, tower),
            visit(4, futureTrip, tower)
        )
        val stats = TravelStatsCalculator.compute(trips, visits, placeCount = 2, now = now)

        assertEquals(4, stats.totalVisits)
        assertEquals(2, stats.totalPlaces)
        assertEquals(1.3, stats.averageVisitsPerTrip, 0.0)   // 4 / 3 arrondi à une décimale
        assertEquals(RankedItem("Tour de Belém", 3), stats.mostVisitedPlace)
    }
}
