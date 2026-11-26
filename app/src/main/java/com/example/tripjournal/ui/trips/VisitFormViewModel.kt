package com.example.tripjournal.ui.trips

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.tripjournal.data.database.dao.TripDao
import com.example.tripjournal.data.database.entity.Place
import com.example.tripjournal.data.database.entity.Trip
import com.example.tripjournal.data.database.entity.Visit
import com.example.tripjournal.data.repository.PlaceRepository
import com.example.tripjournal.data.repository.VisitRepository
import com.example.tripjournal.domain.VisitRules
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class VisitFormEvent {
    object Saved : VisitFormEvent()
    object Deleted : VisitFormEvent()
    object Missing : VisitFormEvent()
    object OutsideTrip : VisitFormEvent()
}

class VisitFormViewModel(
    tripDao: TripDao,
    placeRepository: PlaceRepository,
    private val visitRepository: VisitRepository,
    private val tripId: Long,
    visitId: Long
) : ViewModel() {

    val isEditing = visitId != 0L

    val trip: StateFlow<Trip?> = tripDao.getTripById(tripId)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val places: StateFlow<List<Place>> = placeRepository.observePlaces()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Choix de l'utilisateur : gardés dans le ViewModel pour survivre à la rotation. */
    val selectedPlace = MutableStateFlow<Place?>(null)
    val selectedDate = MutableStateFlow<Long?>(null)

    private val _visit = MutableStateFlow<Visit?>(null)
    val visit: StateFlow<Visit?> = _visit.asStateFlow()

    private val _events = MutableSharedFlow<VisitFormEvent>()
    val events: SharedFlow<VisitFormEvent> = _events.asSharedFlow()

    init {
        if (isEditing) {
            viewModelScope.launch {
                val loaded = visitRepository.getVisit(visitId) ?: return@launch
                _visit.value = loaded
                selectedDate.value = loaded.visitDate
                selectedPlace.value = placeRepository.getPlace(loaded.placeId)
            }
        }
    }

    fun save(notes: String) {
        viewModelScope.launch {
            val currentTrip = trip.value ?: return@launch
            val place = selectedPlace.value
            val date = selectedDate.value
            if (place == null || date == null) {
                _events.emit(VisitFormEvent.Missing)
                return@launch
            }
            if (!VisitRules.isWithinTrip(date, currentTrip)) {
                _events.emit(VisitFormEvent.OutsideTrip)
                return@launch
            }
            val base = _visit.value
                ?: Visit(id = 0L, tripId = tripId, placeId = place.id, visitDate = date)
            visitRepository.save(
                base.copy(placeId = place.id, visitDate = date, notes = notes.trim()),
                currentTrip
            )
            _events.emit(VisitFormEvent.Saved)
        }
    }

    fun delete() {
        viewModelScope.launch {
            val current = _visit.value ?: return@launch
            visitRepository.delete(current)
            _events.emit(VisitFormEvent.Deleted)
        }
    }

    class Factory(
        private val tripDao: TripDao,
        private val placeRepository: PlaceRepository,
        private val visitRepository: VisitRepository,
        private val tripId: Long,
        private val visitId: Long
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            VisitFormViewModel(tripDao, placeRepository, visitRepository, tripId, visitId) as T
    }
}