package com.example.tripjournal.ui.trips

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tripjournal.data.database.TripJournalDatabase
import com.example.tripjournal.data.database.entity.Trip
import com.example.tripjournal.data.preferences.TripJournalPreferences
import com.example.tripjournal.data.preferences.TripJournalPreferencesRepository
import com.example.tripjournal.domain.TripRules
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TripsUiState(
    val trips: List<Trip> = emptyList(),
    val preferences: TripJournalPreferences = TripJournalPreferences()
)

class TripsViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = TripJournalDatabase.get(application).tripDao()
    val preferencesRepository = TripJournalPreferencesRepository(application)

    val uiState = combine(dao.getAllTrips(), preferencesRepository.preferences) { trips, preferences ->
        TripsUiState(TripRules.sortTrips(trips, preferences.sortOrder), preferences)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TripsUiState())

    fun saveTrip(trip: Trip): Boolean {
        if (!TripRules.hasValidDateRange(trip.startDate, trip.endDate)) return false
        viewModelScope.launch {
            if (trip.id == 0L) dao.insertTrip(trip) else dao.updateTrip(trip)
        }
        return true
    }

    fun deleteTrip(trip: Trip) {
        viewModelScope.launch { dao.deleteTrip(trip) }
    }
}
