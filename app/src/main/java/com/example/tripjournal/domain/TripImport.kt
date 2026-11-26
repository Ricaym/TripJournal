package com.example.tripjournal.domain

import com.example.tripjournal.data.database.entity.Trip
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/** Un enregistrement brut lu dans le JSON (tout peut être vide ou faux). */
data class TripRecord(
    val name: String?,
    val destination: String?,
    val startDate: String?,
    val endDate: String?,
    val description: String?
)

data class ImportResult(
    val toInsert: List<Trip>,
    val invalid: Int,
    val duplicates: Int
)

object TripImport {

    /** "2026-03-14" → millis UTC, ou null si la date est absente ou invalide. */
    fun parseDate(text: String?): Long? {
        if (text.isNullOrBlank()) return null
        return try {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
                isLenient = false
            }.parse(text.trim())?.time
        } catch (e: ParseException) {
            null
        }
    }

    /** Convertit un enregistrement en Trip, ou null s'il est invalide. */
    fun toTrip(record: TripRecord): Trip? {
        val start = parseDate(record.startDate) ?: return null
        val end = parseDate(record.endDate) ?: return null
        val trip = Trip(
            name = record.name.orEmpty().trim(),
            destination = record.destination.orEmpty().trim(),
            startDate = start,
            endDate = end,
            description = record.description.orEmpty().trim()
        )
        return trip.takeIf { TripRules.isValid(it) }
    }

    /** Trie les enregistrements : valides à insérer, invalides, doublons (en base ou dans le fichier). */
    fun prepare(records: List<TripRecord>, existing: List<Trip>): ImportResult {
        val accepted = mutableListOf<Trip>()
        var invalid = 0
        var duplicates = 0
        for (record in records) {
            val trip = toTrip(record)
            when {
                trip == null -> invalid++
                TripRules.isDuplicate(trip, existing + accepted) -> duplicates++
                else -> accepted += trip
            }
        }
        return ImportResult(accepted, invalid, duplicates)
    }
}
