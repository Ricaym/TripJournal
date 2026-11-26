package com.example.tripjournal.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.tripJournalDataStore by preferencesDataStore(name = "tripjournal_preferences")

enum class ThemePreference {
    SYSTEM,
    LIGHT,
    DARK
}

enum class TripSortOrder {
    NEWEST_FIRST,
    OLDEST_FIRST,
    NAME
}

enum class TripDateFormat(val pattern: String) {
    DAY_MONTH_YEAR("dd/MM/yyyy"),
    MONTH_DAY_YEAR("MM/dd/yyyy"),
    ISO("yyyy-MM-dd")
}

data class TripJournalPreferences(
    val theme: ThemePreference = ThemePreference.SYSTEM,
    val sortOrder: TripSortOrder = TripSortOrder.NEWEST_FIRST,
    val dateFormat: TripDateFormat = TripDateFormat.DAY_MONTH_YEAR
)

class TripJournalPreferencesRepository(context: Context) {
    private val dataStore = context.applicationContext.tripJournalDataStore

    val preferences: Flow<TripJournalPreferences> = dataStore.data.map { values ->
        TripJournalPreferences(
            theme = values.enumValue(THEME, ThemePreference.SYSTEM),
            sortOrder = values.enumValue(SORT_ORDER, TripSortOrder.NEWEST_FIRST),
            dateFormat = values.enumValue(DATE_FORMAT, TripDateFormat.DAY_MONTH_YEAR)
        )
    }

    suspend fun setTheme(value: ThemePreference) = dataStore.edit { it[THEME] = value.name }

    suspend fun setSortOrder(value: TripSortOrder) = dataStore.edit { it[SORT_ORDER] = value.name }

    suspend fun setDateFormat(value: TripDateFormat) = dataStore.edit { it[DATE_FORMAT] = value.name }

    private inline fun <reified T : Enum<T>> Preferences.enumValue(
        key: Preferences.Key<String>,
        default: T
    ): T = get(key)?.let { stored -> enumValues<T>().firstOrNull { it.name == stored } } ?: default

    private companion object {
        val THEME = stringPreferencesKey("theme")
        val SORT_ORDER = stringPreferencesKey("trip_sort_order")
        val DATE_FORMAT = stringPreferencesKey("trip_date_format")
    }
}
