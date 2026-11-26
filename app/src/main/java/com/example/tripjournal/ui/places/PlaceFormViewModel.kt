package com.example.tripjournal.ui.places

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.tripjournal.data.database.entity.Place
import com.example.tripjournal.data.repository.PlaceDeleteResult
import com.example.tripjournal.data.repository.PlaceRepository
import com.example.tripjournal.domain.VisitRules
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Ce que l'écran doit faire après une action : fermer, afficher une erreur… */
sealed class PlaceFormEvent {
    object Saved : PlaceFormEvent()
    object Deleted : PlaceFormEvent()
    object Invalid : PlaceFormEvent()
    data class InUse(val visitCount: Int) : PlaceFormEvent()
}

/**
 * Formulaire d'un lieu. placeId = 0 : nouveau lieu ; sinon : modification d'un lieu existant.
 * Le ViewModel survit à la rotation : le lieu chargé n'est pas relu depuis la base.
 */
class PlaceFormViewModel(
    private val repository: PlaceRepository,
    private val placeId: Long
) : ViewModel() {

    private val _place = MutableStateFlow<Place?>(null)
    val place: StateFlow<Place?> = _place

    private val _events = MutableSharedFlow<PlaceFormEvent>()
    val events: SharedFlow<PlaceFormEvent> = _events

    val isEditing: Boolean get() = placeId != 0L

    init {
        if (isEditing) viewModelScope.launch { _place.value = repository.getPlace(placeId) }
    }

    fun save(name: String, city: String, country: String, description: String) {
        val candidate = (_place.value ?: Place(name = "", city = "", country = "")).copy(
            name = name.trim(), city = city.trim(),
            country = country.trim(), description = description.trim()
        )
        viewModelScope.launch {
            if (!VisitRules.isValidPlace(candidate)) {
                _events.emit(PlaceFormEvent.Invalid)
                return@launch
            }
            repository.save(candidate)
            _events.emit(PlaceFormEvent.Saved)
        }
    }

    fun delete() {
        val current = _place.value ?: return
        viewModelScope.launch {
            when (val result = repository.delete(current)) {
                PlaceDeleteResult.Deleted -> _events.emit(PlaceFormEvent.Deleted)
                is PlaceDeleteResult.InUse -> _events.emit(PlaceFormEvent.InUse(result.visitCount))
            }
        }
    }

    class Factory(
        private val repository: PlaceRepository,
        private val placeId: Long
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            PlaceFormViewModel(repository, placeId) as T
    }
}