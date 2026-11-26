package com.example.tripjournal.ui.trips

import com.example.tripjournal.data.preferences.TripDateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object TripDates {
    @Volatile
    private var shortPattern = TripDateFormat.DAY_MONTH_YEAR.pattern

    fun setFormat(format: TripDateFormat) {
        shortPattern = format.pattern
    }

    private fun format(pattern: String, millis: Long): String =
        SimpleDateFormat(pattern, Locale.FRANCE)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
            .format(Date(millis))

    fun day(millis: Long): String = format("EEEE d MMMM", millis)
    fun short(millis: Long): String = format(shortPattern, millis)
}
