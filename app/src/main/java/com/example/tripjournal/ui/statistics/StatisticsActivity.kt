package com.example.tripjournal.ui.statistics

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.tripjournal.R
import com.example.tripjournal.data.database.TripJournalDatabase
import com.example.tripjournal.data.repository.StatisticsRepository
import com.example.tripjournal.domain.TravelStats
import com.example.tripjournal.ui.places.applySystemBarPadding
import com.google.android.material.appbar.MaterialToolbar
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class StatisticsActivity : AppCompatActivity() {

    private val viewModel: StatisticsViewModel by viewModels {
        val db = TripJournalDatabase.get(this)
        StatisticsViewModel.Factory(StatisticsRepository(db.tripDao(), db.visitDao(), db.placeDao()))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()  // icônes de la barre d'état adaptées au thème clair / sombre
        setContentView(R.layout.activity_statistics)
        findViewById<ViewGroup>(android.R.id.content).getChildAt(0).applySystemBarPadding()

        findViewById<MaterialToolbar>(R.id.statsToolbar).setNavigationOnClickListener { finish() }

        val progress = findViewById<View>(R.id.statsProgress)
        val emptyText = findViewById<View>(R.id.statsEmptyText)
        val content = findViewById<View>(R.id.statsContent)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.stats.collect { stats ->
                    progress.visibility = if (stats == null) View.VISIBLE else View.GONE
                    emptyText.visibility = if (stats?.isEmpty == true) View.VISIBLE else View.GONE
                    content.visibility = if (stats != null && !stats.isEmpty) View.VISIBLE else View.GONE
                    if (stats != null && !stats.isEmpty) bind(stats)
                }
            }
        }
    }

    private fun bind(stats: TravelStats) {
        val decimal = NumberFormat.getNumberInstance(Locale.FRANCE).apply { maximumFractionDigits = 1 }

        bindTile(R.id.tileTrips, stats.totalTrips.toString(), R.string.total_trips)
        bindTile(R.id.tilePlaces, stats.totalPlaces.toString(), R.string.total_places)
        bindTile(R.id.tileVisits, stats.totalVisits.toString(), R.string.total_visits)
        bindTile(R.id.tileAverage, decimal.format(stats.averageVisitsPerTrip), R.string.avg_visits_per_trip)

        text(R.id.statsTotalDays).text = getString(R.string.stats_total_days, stats.totalTravelDays)
        text(R.id.statsLongestTrip).text = stats.longestTrip?.let {
            getString(R.string.stats_longest_trip, it.name, stats.longestTripDays)
        } ?: ""

        text(R.id.statsStatus).text = getString(
            R.string.stats_status_values,
            stats.ongoingTrips, stats.upcomingTrips, stats.pastTrips, stats.favoriteTrips
        )

        text(R.id.statsTopCountries).text = stats.topCountries
            .mapIndexed { index, item -> getString(R.string.stats_ranked_item, index + 1, item.label, item.count) }
            .joinToString("\n")

        text(R.id.statsMostVisitedPlace).text = stats.mostVisitedPlace?.let {
            getString(R.string.stats_place_visits, it.label, it.count)
        } ?: getString(R.string.stats_no_visits)
    }

    private fun text(id: Int): TextView = findViewById(id)

    private fun bindTile(tileId: Int, value: String, labelRes: Int) {
        val tile = findViewById<View>(tileId)
        tile.findViewById<TextView>(R.id.statValue).text = value
        tile.findViewById<TextView>(R.id.statLabel).setText(labelRes)
    }

    companion object {
        fun newIntent(context: Context): Intent = Intent(context, StatisticsActivity::class.java)
    }
}
