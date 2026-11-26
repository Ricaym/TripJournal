package com.example.tripjournal.data.settings

import android.content.Context
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.tripjournal.data.database.TripJournalDatabase
import com.example.tripjournal.data.importer.TripJsonImporter
import com.example.tripjournal.domain.ImportResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.IOException

/** Instance unique du DataStore des préférences (une seule par fichier, sinon crash). */
private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = SettingsManager.DATASTORE_NAME
)

/**
 * Gestionnaire des préférences utilisateur, basé sur Preferences DataStore.
 *
 * Chaque préférence est exposée sous forme de [Flow] : toute modification est
 * propagée automatiquement aux écrans qui l'observent, sans rafraîchissement manuel,
 * et elle est persistée sur disque (conservée après redémarrage de l'application).
 */
class SettingsManager private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val dataStore = appContext.settingsDataStore

    /** Données brutes ; en cas d'erreur de lecture on repart des valeurs par défaut. */
    private val preferences: Flow<Preferences> = dataStore.data.catch { error ->
        if (error is IOException) {
            Log.e(TAG, "Lecture du DataStore impossible, valeurs par défaut utilisées", error)
            emit(emptyPreferences())
        } else {
            throw error
        }
    }

    val themeMode: Flow<Int> = preferences
        .map { it[Keys.THEME_MODE] ?: THEME_SYSTEM }
        .distinctUntilChanged()

    val defaultSort: Flow<String> = preferences
        .map { it[Keys.DEFAULT_SORT] ?: SORT_DATE_DESC }
        .distinctUntilChanged()

    val confirmDelete: Flow<Boolean> = preferences
        .map { it[Keys.CONFIRM_DELETE] ?: true }
        .distinctUntilChanged()

    val distanceUnit: Flow<String> = preferences
        .map { it[Keys.DISTANCE_UNIT] ?: UNIT_KM }
        .distinctUntilChanged()

    suspend fun setThemeMode(mode: Int) {
        dataStore.edit { it[Keys.THEME_MODE] = mode }
    }

    suspend fun setDefaultSort(sort: String) {
        dataStore.edit { it[Keys.DEFAULT_SORT] = sort }
    }

    suspend fun setConfirmDelete(confirm: Boolean) {
        dataStore.edit { it[Keys.CONFIRM_DELETE] = confirm }
    }

    suspend fun setDistanceUnit(unit: String) {
        dataStore.edit { it[Keys.DISTANCE_UNIT] = unit }
    }

    /**
     * Réinitialise les données de la base Room et réimporte le fichier trips_seed.json.
     * Les préférences utilisateur ne sont pas touchées.
     */
    suspend fun resetAndReimportData(): ImportResult? = withContext(Dispatchers.IO) {
        val db = TripJournalDatabase.get(appContext)
        db.clearAllTables()

        // Réinitialiser le marqueur « import déjà effectué » de TripJsonImporter
        appContext.getSharedPreferences("trip_import", Context.MODE_PRIVATE)
            .edit().clear().commit()

        TripJsonImporter(appContext, db.tripDao()).importIfNeeded()
    }

    private object Keys {
        val THEME_MODE = intPreferencesKey("pref_theme_mode")
        val DEFAULT_SORT = stringPreferencesKey("pref_default_sort")
        val CONFIRM_DELETE = booleanPreferencesKey("pref_confirm_delete")
        val DISTANCE_UNIT = stringPreferencesKey("pref_distance_unit")
    }

    companion object {
        private const val TAG = "SettingsManager"
        const val DATASTORE_NAME = "trip_journal_settings"

        const val THEME_SYSTEM = 0
        const val THEME_LIGHT = 1
        const val THEME_DARK = 2

        const val SORT_DATE_DESC = "DATE_DESC"
        const val SORT_DATE_ASC = "DATE_ASC"
        const val SORT_NAME_ASC = "NAME_ASC"
        const val SORT_DESTINATION_ASC = "DESTINATION_ASC"

        const val UNIT_KM = "KM"
        const val UNIT_MILES = "MILES"

        /** Convertit le mode de thème enregistré en mode nuit AppCompat. */
        fun toNightMode(mode: Int): Int = when (mode) {
            THEME_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            THEME_DARK -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }

        @Volatile
        private var INSTANCE: SettingsManager? = null

        fun getInstance(context: Context): SettingsManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SettingsManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
