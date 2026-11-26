package com.example.tripjournal.ui.places

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.tripjournal.R
import com.example.tripjournal.ui.settings.deleteWithOptionalConfirmation
import com.example.tripjournal.data.database.TripJournalDatabase
import com.example.tripjournal.data.repository.PlaceRepository
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Formulaire d'un lieu : ajout (id = 0) ou modification, avec suppression protégée. */
class PlaceFormActivity : AppCompatActivity() {

    private val placeId: Long by lazy { intent.getLongExtra(EXTRA_PLACE_ID, 0L) }

    private val viewModel: PlaceFormViewModel by viewModels {
        PlaceFormViewModel.Factory(PlaceRepository(TripJournalDatabase.get(this).placeDao()), placeId)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_place_form)
        val root = findViewById<ViewGroup>(android.R.id.content).getChildAt(0)
        root.applySystemBarPadding()

        val nameInput = findViewById<EditText>(R.id.placeNameInput)
        val cityInput = findViewById<EditText>(R.id.placeCityInput)
        val countryInput = findViewById<EditText>(R.id.placeCountryInput)
        val descriptionInput = findViewById<EditText>(R.id.placeDescriptionInput)
        val deleteButton = findViewById<Button>(R.id.deletePlaceButton)
        deleteButton.visibility = if (viewModel.isEditing) View.VISIBLE else View.GONE

        // Modification : on remplit les champs une seule fois.
        // Après une rotation, Android restaure lui-même le texte saisi : on ne l'écrase pas.
        if (savedInstanceState == null && viewModel.isEditing) {
            lifecycleScope.launch {
                val place = viewModel.place.filterNotNull().first()
                nameInput.setText(place.name)
                cityInput.setText(place.city)
                countryInput.setText(place.country)
                descriptionInput.setText(place.description)
            }
        }

        findViewById<Button>(R.id.savePlaceButton).setOnClickListener {
            viewModel.save(
                nameInput.text.toString(), cityInput.text.toString(),
                countryInput.text.toString(), descriptionInput.text.toString()
            )
        }
        deleteButton.setOnClickListener {
            deleteWithOptionalConfirmation(R.string.delete_place_confirm) { viewModel.delete() }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when (event) {
                        PlaceFormEvent.Saved, PlaceFormEvent.Deleted -> finish()
                        PlaceFormEvent.Invalid ->
                            Snackbar.make(root, R.string.place_invalid, Snackbar.LENGTH_LONG).show()
                        is PlaceFormEvent.InUse ->
                            Snackbar.make(root, getString(R.string.place_in_use, event.visitCount), Snackbar.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_PLACE_ID = "place_id"
        fun newIntent(context: Context, placeId: Long): Intent =
            Intent(context, PlaceFormActivity::class.java).putExtra(EXTRA_PLACE_ID, placeId)
    }
}