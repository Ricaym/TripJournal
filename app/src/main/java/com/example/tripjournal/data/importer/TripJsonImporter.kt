package com.example.tripjournal.data.importer

import android.content.Context
import android.util.Log
import com.example.tripjournal.data.database.dao.TripDao
import com.example.tripjournal.domain.ImportResult
import com.example.tripjournal.domain.TripImport
import com.example.tripjournal.domain.TripRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray

class TripJsonImporter(
    private val context: Context,
    private val tripDao: TripDao
) {

    /**
     * Importe le JSON une seule fois (au premier lancement). Retourne null si déjà fait.
     * Le verrou empêche deux écrans de lancer l'import en même temps : sans lui, les deux
     * appels voient une base vide et insèrent chacun les voyages (doublons).
     */
    suspend fun importIfNeeded(): ImportResult? = importLock.withLock { doImport() }

    private suspend fun doImport(): ImportResult? = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_DONE, false)) return@withContext null

        val json = context.assets.open(FILE).bufferedReader().use { it.readText() }
        val result = TripImport.prepare(parse(json), tripDao.getAllTrips().first())
        // Une seule transaction : les écrans voient les voyages arriver d'un coup, pas un par un.
        tripDao.insertTrips(result.toInsert)

        prefs.edit().putBoolean(KEY_DONE, true).commit()
        Log.i(TAG, "Import : ${result.toInsert.size} ajoutés, ${result.invalid} invalides, ${result.duplicates} doublons")
        result
    }

    private fun parse(json: String): List<TripRecord> {
        val array = JSONArray(json)
        return (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            TripRecord(
                name = o.optString("name"),
                destination = o.optString("destination"),
                startDate = o.optString("startDate"),
                endDate = o.optString("endDate"),
                description = o.optString("description")
            )
        }
    }

    companion object {
        private const val TAG = "TripJsonImporter"
        private const val FILE = "trips_seed.json"
        private const val PREFS = "trip_import"
        private const val KEY_DONE = "seed_imported"

        /** Partagé par toutes les instances : un seul import à la fois dans l'application. */
        private val importLock = Mutex()
    }
}