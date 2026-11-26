package com.example.tripjournal.ui.trips

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.tripjournal.data.database.entity.Trip
import com.example.tripjournal.data.repository.TripRepository
import com.example.tripjournal.data.repository.TripSaveResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed class TripFormEvent {
    object Saved : TripFormEvent()
    object Deleted : TripFormEvent()
    object Invalid : TripFormEvent()
    object Duplicate : TripFormEvent()
}

class TripFormViewModel(
    private val repository: TripRepository,
    tripId: Long
) : ViewModel() {

    val isEditing = tripId != 0L

    private val _trip = MutableStateFlow<Trip?>(null)
    val trip: StateFlow<Trip?> = _trip.asStateFlow()

    /** Dates choisies : gardées ici pour survivre à la rotation. */
    val startDate = MutableStateFlow<Long?>(null)
    val endDate = MutableStateFlow<Long?>(null)

    private val _events = MutableSharedFlow<TripFormEvent>()
    val events: SharedFlow<TripFormEvent> = _events.asSharedFlow()

    init {
        if (isEditing) {
            viewModelScope.launch {
                val loaded = repository.observeTrip(tripId).first() ?: return@launch
                _trip.value = loaded
                startDate.value = loaded.startDate
                endDate.value = loaded.endDate
            }
        }
    }

    fun save(name: String, destination: String, description: String) {
        viewModelScope.launch {
            val start = startDate.value
            val end = endDate.value
            if (start == null || end == null) {
                _events.emit(TripFormEvent.Invalid)
                return@launch
            }
            val base = _trip.value
                ?: Trip(name = name, destination = destination, startDate = start, endDate = end)
            val result = repository.save(
                base.copy(
                    name = name,
                    destination = destination,
                    description = description,
                    startDate = start,
                    endDate = end
                )
            )
            _events.emit(
                when (result) {
                    is TripSaveResult.Saved -> TripFormEvent.Saved
                    TripSaveResult.Invalid -> TripFormEvent.Invalid
                    TripSaveResult.Duplicate -> TripFormEvent.Duplicate
                }
            )
        }
    }

    fun delete() {
        viewModelScope.launch {
            val current = _trip.value ?: return@launch
            repository.delete(current)
            _events.emit(TripFormEvent.Deleted)
        }
    }

    class Factory(
        private val repository: TripRepository,
        private val tripId: Long
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            TripFormViewModel(repository, tripId) as T
    }
}