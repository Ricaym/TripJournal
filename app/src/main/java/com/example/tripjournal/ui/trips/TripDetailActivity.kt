package com.example.tripjournal.ui.trips

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tripjournal.R
import com.example.tripjournal.data.database.TripJournalDatabase
import com.example.tripjournal.data.repository.VisitRepository
import com.example.tripjournal.ui.places.PlaceListActivity
import com.example.tripjournal.ui.places.applySystemBarPadding
import kotlinx.coroutines.launch

class TripDetailActivity : AppCompatActivity() {

    private var tripId = 0L

    private val viewModel: TripDetailViewModel by viewModels {
        val db = TripJournalDatabase.get(this)
        TripDetailViewModel.Factory(db.tripDao(), VisitRepository(db.visitDao()), tripId)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trip_detail)
        findViewById<ViewGroup>(android.R.id.content).getChildAt(0).applySystemBarPadding()

        val nameText = findViewById<TextView>(R.id.tripNameText)
        val infoText = findViewById<TextView>(R.id.tripInfoText)
        val warningText = findViewById<TextView>(R.id.tripWarningText)
        val emptyText = findViewById<TextView>(R.id.itineraryEmptyText)

        val adapter = ItineraryAdapter { item ->
            startActivity(VisitFormActivity.newIntent(this, tripId, item.visit.id))
        }
        findViewById<View>(R.id.addVisitButton).setOnClickListener {
            if (tripId != 0L) startActivity(VisitFormActivity.newIntent(this, tripId, 0L))
        }
        findViewById<RecyclerView>(R.id.itineraryList).apply {
            layoutManager = LinearLayoutManager(this@TripDetailActivity)
            this.adapter = adapter
        }

        findViewById<Button>(R.id.managePlacesButton).setOnClickListener {
            startActivity(Intent(this, PlaceListActivity::class.java))
        }

        findViewById<Button>(R.id.editTripButton).setOnClickListener {
            startActivity(TripFormActivity.newIntent(this, tripId))
        }
        var loaded = false

        lifecycleScope.launch {
            tripId = intent.getLongExtra(EXTRA_TRIP_ID, 0L)
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    if (state == null) {
                        if (loaded) finish()
                        return@collect
                    }
                    loaded = true
                    nameText.text = state.trip.name
                    infoText.text = getString(
                        R.string.trip_info,
                        state.trip.destination,
                        TripDates.short(state.trip.startDate),
                        TripDates.short(state.trip.endDate)
                    )
                    warningText.text = getString(R.string.visits_outside_trip, state.outsideCount)
                    warningText.visibility = if (state.outsideCount > 0) View.VISIBLE else View.GONE
                    adapter.submitList(state.rows)
                    emptyText.visibility = if (state.rows.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }

    companion object {
        const val EXTRA_TRIP_ID = "trip_id"

        fun newIntent(context: Context, tripId: Long): Intent =
            Intent(context, TripDetailActivity::class.java).putExtra(EXTRA_TRIP_ID, tripId)
    }
}