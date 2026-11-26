package com.example.tripjournal.ui.trips

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.tripjournal.data.database.dao.TripDao
import com.example.tripjournal.data.database.entity.Trip
import com.example.tripjournal.data.repository.VisitRepository
import com.example.tripjournal.domain.VisitRules
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class TripDetailState(
    val trip: Trip,
    val rows: List<ItineraryRow>,
    val outsideCount: Int
)

class TripDetailViewModel(
    tripDao: TripDao,
    visitRepository: VisitRepository,
    tripId: Long
) : ViewModel() {

    val state: StateFlow<TripDetailState?> = combine(
        tripDao.getTripById(tripId),
        visitRepository.observeItinerary(tripId)
    ) { trip, visits ->
        trip?.let {
            TripDetailState(
                trip = it,
                rows = ItineraryRow.build(visits, it),
                outsideCount = VisitRules.visitsOutsideTrip(visits.map { v -> v.visit }, it).size
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    class Factory(
        private val tripDao: TripDao,
        private val visitRepository: VisitRepository,
        private val tripId: Long
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            TripDetailViewModel(tripDao, visitRepository, tripId) as T
    }
}