package com.example.tripjournal.ui.trips

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object TripDates {
    private fun format(pattern: String, millis: Long): String =
        SimpleDateFormat(pattern, Locale.FRANCE)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
            .format(Date(millis))

    fun day(millis: Long): String = format("EEEE d MMMM", millis)
    fun short(millis: Long): String = format("d MMM yyyy", millis)
}