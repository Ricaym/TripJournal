package com.example.tripjournal.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.tripjournal.data.settings.SettingsManager
import com.example.tripjournal.data.preferences.TripDateFormat
import com.example.tripjournal.domain.ImportResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsManager: SettingsManager
) : ViewModel() {

    /**
     * Les valeurs sont `null` tant que DataStore n'a pas été lu : l'écran attend la
     * vraie valeur au lieu d'afficher (et d'enregistrer) une valeur par défaut.
     */
    val themeMode: StateFlow<Int?> = settingsManager.themeMode.asState()
    val defaultSort: StateFlow<String?> = settingsManager.defaultSort.asState()
    val confirmDelete: StateFlow<Boolean?> = settingsManager.confirmDelete.asState()
    val distanceUnit: StateFlow<String?> = settingsManager.distanceUnit.asState()
    val dateFormat: StateFlow<TripDateFormat?> = settingsManager.dateFormat.asState()

    private val _resetResult = MutableSharedFlow<ImportResult?>()
    val resetResult: SharedFlow<ImportResult?> = _resetResult

    fun setThemeMode(mode: Int) {
        viewModelScope.launch { settingsManager.setThemeMode(mode) }
    }

    fun setDefaultSort(sortOrder: String) {
        viewModelScope.launch { settingsManager.setDefaultSort(sortOrder) }
    }

    fun setConfirmDelete(enabled: Boolean) {
        viewModelScope.launch { settingsManager.setConfirmDelete(enabled) }
    }

    fun setDistanceUnit(unit: String) {
        viewModelScope.launch { settingsManager.setDistanceUnit(unit) }
    }

    fun setDateFormat(format: TripDateFormat) {
        viewModelScope.launch { settingsManager.setDateFormat(format) }
    }

    fun resetData() {
        viewModelScope.launch {
            val result = settingsManager.resetAndReimportData()
            _resetResult.emit(result)
        }
    }

    private fun <T> Flow<T>.asState(): StateFlow<T?> =
        stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    class Factory(private val settingsManager: SettingsManager) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
                return SettingsViewModel(settingsManager) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
