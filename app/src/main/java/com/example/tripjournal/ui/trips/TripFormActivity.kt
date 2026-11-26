package com.example.tripjournal.ui.trips

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.tripjournal.R
import com.example.tripjournal.data.database.TripJournalDatabase
import com.example.tripjournal.data.repository.TripRepository
import com.example.tripjournal.ui.places.applySystemBarPadding
import com.example.tripjournal.ui.settings.deleteWithOptionalConfirmation
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TripFormActivity : AppCompatActivity() {

    private val viewModel: TripFormViewModel by viewModels {
        TripFormViewModel.Factory(
            TripRepository(TripJournalDatabase.get(this).tripDao()),
            intent.getLongExtra(EXTRA_TRIP_ID, 0L)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trip_form)
        findViewById<ViewGroup>(android.R.id.content).getChildAt(0).applySystemBarPadding()

        val nameInput = findViewById<TextInputEditText>(R.id.tripNameInput)
        val destinationInput = findViewById<TextInputEditText>(R.id.tripDestinationInput)
        val descriptionInput = findViewById<TextInputEditText>(R.id.tripDescriptionInput)
        val datesButton = findViewById<Button>(R.id.tripDatesButton)
        val deleteButton = findViewById<Button>(R.id.deleteTripButton)

        deleteButton.visibility = if (viewModel.isEditing) View.VISIBLE else View.GONE
        datesButton.setOnClickListener { chooseDates() }
        deleteButton.setOnClickListener { confirmDelete() }
        findViewById<Button>(R.id.saveTripButton).setOnClickListener {
            viewModel.save(
                nameInput.text?.toString().orEmpty(),
                destinationInput.text?.toString().orEmpty(),
                descriptionInput.text?.toString().orEmpty()
            )
        }

        // Après une rotation, le calendrier est recréé : on le rebranche.
        @Suppress("UNCHECKED_CAST")
        (supportFragmentManager.findFragmentByTag(DATE_PICKER_TAG)
                as? MaterialDatePicker<androidx.core.util.Pair<Long, Long>>)
            ?.addOnPositiveButtonClickListener { setDates(it) }

        // Remplir les champs une seule fois (pas après une rotation).
        if (savedInstanceState == null && viewModel.isEditing) {
            lifecycleScope.launch {
                val trip = viewModel.trip.filterNotNull().first()
                nameInput.setText(trip.name)
                destinationInput.setText(trip.destination)
                descriptionInput.setText(trip.description)
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    combine(viewModel.startDate, viewModel.endDate) { s, e -> s to e }
                        .collect { (start, end) ->
                            datesButton.text = if (start != null && end != null) {
                                getString(R.string.trip_dates_value, TripDates.short(start), TripDates.short(end))
                            } else {
                                getString(R.string.choose_trip_dates)
                            }
                        }
                }
                launch {
                    viewModel.events.collect { event ->
                        when (event) {
                            TripFormEvent.Saved, TripFormEvent.Deleted -> finish()
                            TripFormEvent.Invalid -> snack(R.string.trip_invalid)
                            TripFormEvent.Duplicate -> snack(R.string.trip_duplicate)
                        }
                    }
                }
            }
        }
    }

    private fun chooseDates() {
        val builder = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText(R.string.choose_trip_dates)
        val start = viewModel.startDate.value
        val end = viewModel.endDate.value
        if (start != null && end != null) {
            builder.setSelection(androidx.core.util.Pair(start, end))
        }
        val picker = builder.build()
        picker.addOnPositiveButtonClickListener { setDates(it) }
        picker.show(supportFragmentManager, DATE_PICKER_TAG)
    }

    private fun setDates(selection: androidx.core.util.Pair<Long, Long>) {
        viewModel.startDate.value = selection.first
        viewModel.endDate.value = selection.second
    }

    private fun confirmDelete() {
        deleteWithOptionalConfirmation(R.string.delete_trip_confirm) { viewModel.delete() }
    }

    private fun snack(message: Int) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show()
    }

    companion object {
        private const val EXTRA_TRIP_ID = "trip_id"
        private const val DATE_PICKER_TAG = "trip_dates"

        fun newIntent(context: Context, tripId: Long): Intent =
            Intent(context, TripFormActivity::class.java).putExtra(EXTRA_TRIP_ID, tripId)
    }
}