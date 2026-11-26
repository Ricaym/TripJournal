package com.example.tripjournal

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.tripjournal.data.database.TripJournalDatabase
import com.example.tripjournal.data.importer.TripJsonImporter
import com.example.tripjournal.ui.catalog.CatalogActivity
import com.example.tripjournal.ui.places.PlaceListActivity
import com.example.tripjournal.ui.places.applySystemBarPadding
import com.example.tripjournal.ui.settings.SettingsActivity
import com.example.tripjournal.ui.statistics.StatisticsActivity
import com.example.tripjournal.ui.trips.TripListActivity
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

/** Menu principal : point d'entrée vers tous les écrans de l'application. */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        findViewById<View>(R.id.main).applySystemBarPadding()

        bindNav(R.id.navTrips, R.string.nav_trips, R.string.nav_trips_desc, TripListActivity::class.java)
        bindNav(R.id.navCatalog, R.string.nav_catalog, R.string.nav_catalog_desc, CatalogActivity::class.java)
        bindNav(R.id.navPlaces, R.string.nav_places, R.string.nav_places_desc, PlaceListActivity::class.java)
        bindNav(R.id.navStats, R.string.nav_stats, R.string.nav_stats_desc, StatisticsActivity::class.java)
        bindNav(R.id.navSettings, R.string.nav_settings, R.string.nav_settings_desc, SettingsActivity::class.java)

        // Import initial du JSON dès le premier lancement, quel que soit l'écran ouvert ensuite.
        // TripJsonImporter ne fait rien si l'import a déjà eu lieu.
        if (savedInstanceState == null) {
            lifecycleScope.launch {
                val result = TripJsonImporter(
                    applicationContext,
                    TripJournalDatabase.get(this@MainActivity).tripDao()
                ).importIfNeeded() ?: return@launch
                Snackbar.make(
                    findViewById(R.id.main),
                    getString(R.string.trips_imported, result.toInsert.size, result.invalid, result.duplicates),
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun bindNav(cardId: Int, titleRes: Int, descriptionRes: Int, target: Class<*>) {
        val card = findViewById<View>(cardId)
        card.findViewById<TextView>(R.id.navTitle).setText(titleRes)
        card.findViewById<TextView>(R.id.navDescription).setText(descriptionRes)
        card.setOnClickListener { startActivity(Intent(this, target)) }
    }
}
