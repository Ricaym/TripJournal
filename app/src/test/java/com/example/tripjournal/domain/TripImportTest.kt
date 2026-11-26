package com.example.tripjournal.domain

import com.example.tripjournal.data.database.entity.Trip
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class TripImportTest {

    private fun record(
        name: String = "Vacances à Rome",
        destination: String = "Rome, Italie",
        start: String = "2025-06-07",
        end: String = "2025-06-14"
    ) = TripRecord(name, destination, start, end, "")

    @Test
    fun validRecordIsConvertedToTrip() {
        val trip = TripImport.toTrip(record())

        assertNotNull(trip)
        assertEquals("Vacances à Rome", trip!!.name)
        assertEquals(TripImport.parseDate("2025-06-07"), trip.startDate)
    }

    @Test
    fun invalidRecordsAreRejected() {
        // Arrange : nom vide, fin avant début, date impossible, date absente
        val records = listOf(
            record(name = "   "),
            record(start = "2026-07-10", end = "2026-07-05"),
            record(start = "2026-13-45"),
            record(end = "")
        )

        // Act
        val result = TripImport.prepare(records, existing = emptyList())

        // Assert
        assertEquals(0, result.toInsert.size)
        assertEquals(4, result.invalid)
    }

    @Test
    fun duplicatesInDatabaseAndInFileAreSkipped() {
        // Arrange : "Vacances à Rome" existe déjà en base ; le fichier contient
        // ce même voyage (en minuscules) + 2 fois "Week-end à Berlin".
        val existing = listOf(
            Trip(
                id = 1,
                name = "Vacances à Rome",
                destination = "Rome, Italie",
                startDate = TripImport.parseDate("2025-06-07")!!,
                endDate = TripImport.parseDate("2025-06-14")!!
            )
        )
        val records = listOf(
            record(name = "vacances à rome"),
            record(name = "Week-end à Berlin", destination = "Berlin", start = "2026-11-06", end = "2026-11-08"),
            record(name = "Week-end à Berlin", destination = "Berlin", start = "2026-11-06", end = "2026-11-08")
        )

        // Act
        val result = TripImport.prepare(records, existing)

        // Assert : seul Berlin est importé, une seule fois
        assertEquals(1, result.toInsert.size)
        assertEquals("Week-end à Berlin", result.toInsert.first().name)
        assertEquals(2, result.duplicates)
        assertNull(TripImport.parseDate("pas une date"))
    }
}
