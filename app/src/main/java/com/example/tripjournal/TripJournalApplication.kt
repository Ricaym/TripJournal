package com.example.tripjournal

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.example.tripjournal.data.settings.SettingsManager
import com.example.tripjournal.ui.trips.TripDates
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class TripJournalApplication : Application() {

    /** Scope lié à la vie du processus, pour observer les préférences globales. */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        val settings = SettingsManager.getInstance(this)

        // Lecture initiale bloquante (petit fichier) : évite un « flash » du mauvais
        // thème au démarrage, avant que le premier écran ne soit affiché.
        val initialMode = runBlocking { settings.themeMode.first() }
        AppCompatDelegate.setDefaultNightMode(SettingsManager.toNightMode(initialMode))
        TripDates.setFormat(runBlocking { settings.dateFormat.first() })

        // Ensuite, chaque changement du thème dans DataStore est appliqué en direct.
        appScope.launch {
            settings.themeMode.collect { mode ->
                AppCompatDelegate.setDefaultNightMode(SettingsManager.toNightMode(mode))
            }
        }
        appScope.launch {
            settings.dateFormat.collect(TripDates::setFormat)
        }
    }
}
