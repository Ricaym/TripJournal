package com.example.tripjournal.ui.places

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.tripjournal.data.database.entity.Place
import com.example.tripjournal.data.repository.PlaceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** État de l'écran « Lieux » : la liste, observée en continu depuis Room. */
class PlaceListViewModel(repository: PlaceRepository) : ViewModel() {

    val places: StateFlow<List<Place>> = repository.observePlaces()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    class Factory(private val repository: PlaceRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            PlaceListViewModel(repository) as T
    }
}