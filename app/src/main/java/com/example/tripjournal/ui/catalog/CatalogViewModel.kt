package com.example.tripjournal.ui.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.tripjournal.data.database.entity.Trip
import com.example.tripjournal.data.repository.TripRepository
import com.example.tripjournal.data.settings.SettingsManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class CatalogViewModel(
    private val repository: TripRepository,
    settingsManager: SettingsManager
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    /** Tri choisi via les chips de l'écran ; `null` = suivre la préférence DataStore. */
    private val sortOverride = MutableStateFlow<String?>(null)

    /**
     * Tri effectivement appliqué : celui choisi sur l'écran s'il existe, sinon le tri
     * par défaut des Réglages (mis à jour en direct si la préférence change).
     * `null` tant que DataStore n'a pas encore été lu.
     */
    val sortOrder: StateFlow<String?> = combine(settingsManager.defaultSort, sortOverride) { default, override ->
        override ?: default
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val pagedTrips: Flow<PagingData<Trip>> = combine(_searchQuery, sortOrder.filterNotNull()) { query, sort ->
        query.trim() to sort
    }.distinctUntilChanged()
        .flatMapLatest { (query, sort) -> repository.getTripsPaged(query, sort) }
        .cachedIn(viewModelScope)

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortOrder(sort: String) {
        if (sort != sortOrder.value) sortOverride.value = sort
    }

    class Factory(
        private val repository: TripRepository,
        private val settingsManager: SettingsManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CatalogViewModel::class.java)) {
                return CatalogViewModel(repository, settingsManager) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
