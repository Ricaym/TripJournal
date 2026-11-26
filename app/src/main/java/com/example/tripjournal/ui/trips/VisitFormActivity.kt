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
import com.example.tripjournal.ui.settings.deleteWithOptionalConfirmation
import com.example.tripjournal.data.database.TripJournalDatabase
import com.example.tripjournal.data.repository.PlaceRepository
import com.example.tripjournal.data.repository.VisitRepository
import com.example.tripjournal.ui.places.PlaceListActivity
import com.example.tripjournal.ui.places.applySystemBarPadding
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.CompositeDateValidator
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class VisitFormActivity : AppCompatActivity() {

    private val viewModel: VisitFormViewModel by viewModels {
        val db = TripJournalDatabase.get(this)
        VisitFormViewModel.Factory(
            db.tripDao(),
            PlaceRepository(db.placeDao()),
            VisitRepository(db.visitDao()),
            intent.getLongExtra(EXTRA_TRIP_ID, 0L),
            intent.getLongExtra(EXTRA_VISIT_ID, 0L)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_visit_form)
        findViewById<ViewGroup>(android.R.id.content).getChildAt(0).applySystemBarPadding()

        val placeButton = findViewById<Button>(R.id.visitPlaceButton)
        val dateButton = findViewById<Button>(R.id.visitDateButton)
        val notesInput = findViewById<TextInputEditText>(R.id.visitNotesInput)
        val deleteButton = findViewById<Button>(R.id.deleteVisitButton)

        deleteButton.visibility = if (viewModel.isEditing) View.VISIBLE else View.GONE
        placeButton.setOnClickListener { choosePlace() }
        dateButton.setOnClickListener { chooseDate() }
        findViewById<Button>(R.id.saveVisitButton).setOnClickListener {
            viewModel.save(notesInput.text?.toString().orEmpty())
        }
        deleteButton.setOnClickListener {
            deleteWithOptionalConfirmation(R.string.delete_visit_confirm) { viewModel.delete() }
        }

        // Après une rotation, le calendrier est recréé : on le rebranche.
        @Suppress("UNCHECKED_CAST")
        (supportFragmentManager.findFragmentByTag(DATE_PICKER_TAG) as? MaterialDatePicker<Long>)
            ?.addOnPositiveButtonClickListener { viewModel.selectedDate.value = it }

        // Remplir les notes une seule fois (pas après une rotation).
        if (savedInstanceState == null && viewModel.isEditing) {
            lifecycleScope.launch {
                notesInput.setText(viewModel.visit.filterNotNull().first().notes)
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.selectedPlace.collect { place ->
                        placeButton.text = place?.let {
                            getString(R.string.visit_place_value, it.name, it.city)
                        } ?: getString(R.string.choose_place)
                    }
                }
                launch {
                    viewModel.selectedDate.collect { date ->
                        dateButton.text = date?.let { TripDates.day(it) }
                            ?: getString(R.string.choose_date)
                    }
                }
                launch {
                    viewModel.events.collect { event ->
                        when (event) {
                            VisitFormEvent.Saved, VisitFormEvent.Deleted -> finish()
                            VisitFormEvent.Missing -> snack(R.string.visit_missing)
                            VisitFormEvent.OutsideTrip -> snack(R.string.visit_outside_trip)
                        }
                    }
                }
            }
        }
    }

    private fun snack(message: Int) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show()
    }

    private fun choosePlace() {
        val places = viewModel.places.value
        if (places.isEmpty()) {
            Snackbar.make(findViewById(android.R.id.content), R.string.no_places_for_visit, Snackbar.LENGTH_LONG)
                .setAction(R.string.manage_places) {
                    startActivity(Intent(this, PlaceListActivity::class.java))
                }
                .show()
            return
        }
        val labels = places.map { getString(R.string.visit_place_value, it.name, it.city) }
            .toTypedArray()
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.choose_place)
            .setItems(labels) { _, which -> viewModel.selectedPlace.value = places[which] }
            .show()
    }

    private fun chooseDate() {
        val trip = viewModel.trip.value ?: return
        val dayMs = 24L * 60 * 60 * 1000
        val start = Math.floorDiv(trip.startDate, dayMs) * dayMs
        val end = Math.floorDiv(trip.endDate, dayMs) * dayMs

        val constraints = CalendarConstraints.Builder()
            .setStart(start)
            .setEnd(end)
            .setOpenAt(viewModel.selectedDate.value ?: start)
            .setValidator(
                CompositeDateValidator.allOf(
                    listOf<CalendarConstraints.DateValidator>(
                        DateValidatorPointForward.from(start),
                        DateValidatorPointBackward.before(end)
                    )
                )
            )
            .build()

        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(R.string.choose_date)
            .setSelection(viewModel.selectedDate.value ?: start)
            .setCalendarConstraints(constraints)
            .build()
        picker.addOnPositiveButtonClickListener { viewModel.selectedDate.value = it }
        picker.show(supportFragmentManager, DATE_PICKER_TAG)
    }

    companion object {
        private const val EXTRA_TRIP_ID = "trip_id"
        private const val EXTRA_VISIT_ID = "visit_id"
        private const val DATE_PICKER_TAG = "visit_date"

        fun newIntent(context: Context, tripId: Long, visitId: Long): Intent =
            Intent(context, VisitFormActivity::class.java)
                .putExtra(EXTRA_TRIP_ID, tripId)
                .putExtra(EXTRA_VISIT_ID, visitId)
    }
}