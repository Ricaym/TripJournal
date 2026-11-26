package com.example.tripjournal.ui.catalog

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tripjournal.R
import com.example.tripjournal.data.database.TripJournalDatabase
import com.example.tripjournal.data.repository.TripRepository
import com.example.tripjournal.data.settings.SettingsManager
import com.example.tripjournal.ui.places.applySystemBarPadding
import com.example.tripjournal.ui.trips.TripDetailActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

class CatalogActivity : AppCompatActivity() {

    private val viewModel: CatalogViewModel by viewModels {
        val db = TripJournalDatabase.get(this)
        CatalogViewModel.Factory(
            repository = TripRepository(db.tripDao()),
            settingsManager = SettingsManager.getInstance(this)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()  // icônes de la barre d'état adaptées au thème clair / sombre
        setContentView(R.layout.activity_catalog)
        findViewById<android.view.ViewGroup>(android.R.id.content).getChildAt(0).applySystemBarPadding()

        val toolbar = findViewById<MaterialToolbar>(R.id.catalogToolbar)
        toolbar.setNavigationOnClickListener { finish() }

        val searchInput = findViewById<EditText>(R.id.catalogSearchInput)
        val chipGroup = findViewById<ChipGroup>(R.id.catalogSortChipGroup)
        val recyclerView = findViewById<RecyclerView>(R.id.catalogRecyclerView)
        val progressBar = findViewById<ProgressBar>(R.id.catalogProgressBar)
        val emptyState = findViewById<View>(R.id.catalogEmptyState)
        val errorState = findViewById<View>(R.id.catalogErrorState)
        val retryButton = findViewById<MaterialButton>(R.id.catalogRetryButton)

        val adapter = TripPagingAdapter { trip ->
            startActivity(TripDetailActivity.newIntent(this, trip.id))
        }

        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@CatalogActivity)
            this.adapter = adapter
        }

        // Ecouteur de recherche en temps réel
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.setSearchQuery(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Ecouteur de tri
        chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            val sort = when {
                checkedIds.contains(R.id.chipSortDateAsc) -> SettingsManager.SORT_DATE_ASC
                checkedIds.contains(R.id.chipSortName) -> SettingsManager.SORT_NAME_ASC
                checkedIds.contains(R.id.chipSortDestination) -> SettingsManager.SORT_DESTINATION_ASC
                else -> SettingsManager.SORT_DATE_DESC
            }
            viewModel.setSortOrder(sort)
        }

        retryButton.setOnClickListener { adapter.retry() }

        // Écoute des états de chargement Paging 3
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    adapter.loadStateFlow.collect { loadStates ->
                        val isRefreshLoading = loadStates.refresh is LoadState.Loading
                        val isRefreshError = loadStates.refresh is LoadState.Error
                        val isListEmpty = adapter.itemCount == 0 && !isRefreshLoading

                        progressBar.visibility = if (isRefreshLoading) View.VISIBLE else View.GONE
                        errorState.visibility = if (isRefreshError) View.VISIBLE else View.GONE
                        emptyState.visibility = if (isListEmpty && !isRefreshError) View.VISIBLE else View.GONE
                        recyclerView.visibility = if (!isRefreshLoading && !isRefreshError && !isListEmpty) View.VISIBLE else View.GONE
                    }
                }
                launch {
                    // La chip cochée reflète le tri actif (préférence DataStore ou choix local)
                    viewModel.sortOrder.filterNotNull().collect { sort ->
                        val chipId = when (sort) {
                            SettingsManager.SORT_DATE_ASC -> R.id.chipSortDateAsc
                            SettingsManager.SORT_NAME_ASC -> R.id.chipSortName
                            SettingsManager.SORT_DESTINATION_ASC -> R.id.chipSortDestination
                            else -> R.id.chipSortDate
                        }
                        if (chipGroup.checkedChipId != chipId) chipGroup.check(chipId)
                    }
                }
                launch {
                    viewModel.pagedTrips.collectLatest { pagingData ->
                        adapter.submitData(pagingData)
                    }
                }
            }
        }
    }
}
