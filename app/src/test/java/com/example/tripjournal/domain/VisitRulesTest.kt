package com.example.tripjournal.domain

import com.example.tripjournal.data.database.dao.VisitWithPlace
import com.example.tripjournal.data.database.entity.Place
import com.example.tripjournal.data.database.entity.Trip
import com.example.tripjournal.data.database.entity.Visit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class VisitRulesTest {

    /** Date UTC en millisecondes, avec une heure facultative. */
    private fun day(year: Int, month: Int, dayOfMonth: Int, hour: Int = 0): Long =
        LocalDate.of(year, month, dayOfMonth).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() +
                hour * 3_600_000L

    // Fixtures : un voyage de 3 jours (10 au 12 juillet) et deux lieux.
    private val trip = Trip(id = 1, name = "Lisbonne", destination = "Portugal",
        startDate = day(2026, 7, 10), endDate = day(2026, 7, 12))
    private val tower = Place(id = 1, name = "Tour de Belém", city = "Lisbonne", country = "Portugal")
    private val museum = Place(id = 2, name = "Musée des Azulejos", city = "Lisbonne", country = "Portugal")

    private fun visitAt(id: Long, place: Place, date: Long) =
        VisitWithPlace(Visit(id = id, tripId = trip.id, placeId = place.id, visitDate = date), place)

    @Test fun visitsOnFirstAndLastDayAreWithinTrip() {
        assertTrue(VisitRules.isWithinTrip(day(2026, 7, 10), trip))
        assertTrue(VisitRules.isWithinTrip(day(2026, 7, 12, hour = 18), trip))   // dernier jour, en soirée
    }

    @Test fun visitsBeforeOrAfterTripAreRejected() {
        assertFalse(VisitRules.isWithinTrip(day(2026, 7, 9, hour = 23), trip))
        assertFalse(VisitRules.isWithinTrip(day(2026, 7, 13), trip))
    }

    @Test fun dayNumberStartsAtOne() {
        assertEquals(1, VisitRules.dayNumber(day(2026, 7, 10, hour = 9), trip))
        assertEquals(3, VisitRules.dayNumber(day(2026, 7, 12), trip))
    }

    @Test fun itineraryGroupsVisitsByDayInChronologicalOrder() {
        // Arrange : visites volontairement dans le désordre.
        val visits = listOf(
            visitAt(3, museum, day(2026, 7, 12, hour = 10)),
            visitAt(2, museum, day(2026, 7, 10, hour = 15)),
            visitAt(1, tower, day(2026, 7, 10, hour = 9))
        )
        // Act
        val itinerary = VisitRules.buildItinerary(visits, trip)
        // Assert : jours 1 et 3 seulement, visites triées par heure.
        assertEquals(listOf(1, 3), itinerary.keys.toList())
        assertEquals(listOf(1L, 2L), itinerary.getValue(1).map { it.visit.id })
        assertEquals(listOf(3L), itinerary.getValue(3).map { it.visit.id })
    }

    @Test fun shorteningTripRevealsVisitsOutsideNewDates() {
        val visits = listOf(
            Visit(id = 1, tripId = 1, placeId = 1, visitDate = day(2026, 7, 10)),
            Visit(id = 2, tripId = 1, placeId = 2, visitDate = day(2026, 7, 12))
        )
        val shortened = trip.copy(endDate = day(2026, 7, 11))   // le voyage se termine un jour plus tôt
        assertEquals(listOf(2L), VisitRules.visitsOutsideTrip(visits, shortened).map { it.id })
    }

    @Test fun placeRequiresNameCityAndCountry() {
        assertTrue(VisitRules.isValidPlace(tower))
        assertFalse(VisitRules.isValidPlace(tower.copy(name = "   ")))
        assertFalse(VisitRules.isValidPlace(tower.copy(country = "")))
    }
}

