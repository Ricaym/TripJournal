package com.example.tripjournal.ui.places

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
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
import com.example.tripjournal.data.repository.PlaceRepository
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

/** Écran « Lieux » : liste réactive, état vide, bouton + pour ajouter. */
class PlaceListActivity : AppCompatActivity() {

    private val viewModel: PlaceListViewModel by viewModels {
        PlaceListViewModel.Factory(PlaceRepository(TripJournalDatabase.get(this).placeDao()))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_place_list)
        findViewById<ViewGroup>(android.R.id.content).getChildAt(0).applySystemBarPadding()

        val adapter = PlaceAdapter { place -> startActivity(PlaceFormActivity.newIntent(this, place.id)) }
        findViewById<RecyclerView>(R.id.placeList).apply {
            layoutManager = LinearLayoutManager(this@PlaceListActivity)
            this.adapter = adapter
        }
        val emptyText = findViewById<TextView>(R.id.placeEmptyText)
        findViewById<FloatingActionButton>(R.id.addPlaceButton).setOnClickListener {
            startActivity(PlaceFormActivity.newIntent(this, 0L))
        }

        // La liste se met à jour toute seule à chaque ajout, modification ou suppression (Flow de Room).
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.places.collect { places ->
                    adapter.submitList(places)
                    emptyText.visibility = if (places.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }
}

