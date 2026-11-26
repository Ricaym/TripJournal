package com.example.tripjournal.ui.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import com.example.tripjournal.data.database.entity.Trip
import com.example.tripjournal.data.repository.TripRepository
import com.example.tripjournal.data.settings.SettingsManager
import com.example.tripjournal.domain.TripFilter
import com.example.tripjournal.domain.TripStatusFilter
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
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalCoroutinesApi::class)
class CatalogViewModel(
    private val repository: TripRepository,
    settingsManager: SettingsManager
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    /** Filtre par statut choisi dans le catalogue (gardé à la rotation). */
    private val _statusFilter = MutableStateFlow(TripStatusFilter.ALL)
    val statusFilter: StateFlow<TripStatusFilter> = _statusFilter.asStateFlow()

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

    val pagedTrips: Flow<PagingData<Trip>> = combine(
        _searchQuery, sortOrder.filterNotNull(), _statusFilter
    ) { query, sort, filter ->
        Triple(query.trim(), sort, filter)
    }.distinctUntilChanged()
        .flatMapLatest { (query, sort, filter) ->
            repository.getTripsPaged(query, sort).map { pagingData ->
                pagingData.filter { trip -> TripFilter.matches(trip, filter, System.currentTimeMillis()) }
            }
        }
        .cachedIn(viewModelScope)

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setStatusFilter(filter: TripStatusFilter) {
        _statusFilter.value = filter
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
