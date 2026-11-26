package com.example.tripjournal

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.tripjournal.data.database.entity.Trip
import com.example.tripjournal.data.preferences.ThemePreference
import com.example.tripjournal.data.preferences.TripDateFormat
import com.example.tripjournal.data.preferences.TripJournalPreferences
import com.example.tripjournal.data.preferences.TripSortOrder
import com.example.tripjournal.domain.TripRules
import com.example.tripjournal.ui.trips.TripsUiState
import com.example.tripjournal.ui.trips.TripsViewModel
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class MainActivity : AppCompatActivity() {
    private enum class Screen { TRIPS, SETTINGS }

    private lateinit var viewModel: TripsViewModel
    private lateinit var content: LinearLayout
    private var screen = Screen.TRIPS
    private var latestState = TripsUiState()
    private var appliedTheme: ThemePreference? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        screen = savedInstanceState?.getString(STATE_SCREEN)
            ?.let { runCatching { Screen.valueOf(it) }.getOrNull() } ?: Screen.TRIPS
        viewModel = ViewModelProvider(this)[TripsViewModel::class.java]
        buildShell()
        render()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    latestState = state
                    applyTheme(state.preferences.theme)
                    render()
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(STATE_SCREEN, screen.name)
        super.onSaveInstanceState(outState)
    }

    private fun buildShell() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(8))
        }
        content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val navigation = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        navigation.addView(navButton("Voyages") { screen = Screen.TRIPS; render() }, weightedParams())
        navigation.addView(navButton("Paramètres") { screen = Screen.SETTINGS; render() }, weightedParams())
        root.addView(content, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(navigation, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        setContentView(root)
    }

    private fun navButton(label: String, action: () -> Unit) = Button(this).apply {
        text = label
        setOnClickListener { action() }
    }

    private fun render() {
        content.removeAllViews()
        when (screen) {
            Screen.TRIPS -> renderTrips()
            Screen.SETTINGS -> renderSettings()
        }
    }

    private fun renderTrips() {
        content.addView(title("Mes voyages"))
        content.addView(Button(this).apply {
            text = "Ajouter un voyage"
            setOnClickListener { showTripDialog() }
        })
        val scroll = ScrollView(this)
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        latestState.trips.forEach { trip -> list.addView(tripRow(trip)) }
        if (latestState.trips.isEmpty()) {
            list.addView(label("Aucun voyage pour le moment. Ajoute ton premier voyage."))
        }
        scroll.addView(list)
        content.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
    }

    private fun tripRow(trip: Trip): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), dp(8), dp(8), dp(8))
            setOnClickListener { showTripDialog(trip) }
        }
        row.addView(label(trip.name, 20f))
        row.addView(label(trip.destination))
        row.addView(label("${TripRules.formatDateUtc(trip.startDate, latestState.preferences.dateFormat)} – ${TripRules.formatDateUtc(trip.endDate, latestState.preferences.dateFormat)}"))
        if (trip.description.isNotBlank()) row.addView(label(trip.description))
        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        actions.addView(Button(this).apply {
            text = "Modifier"
            setOnClickListener { showTripDialog(trip) }
        }, weightedParams())
        actions.addView(Button(this).apply {
            text = "Supprimer"
            setOnClickListener {
                AlertDialog.Builder(this@MainActivity)
                    .setMessage("Supprimer « ${trip.name} » ?")
                    .setNegativeButton("Annuler", null)
                    .setPositiveButton("Supprimer") { _, _ -> viewModel.deleteTrip(trip) }
                    .show()
            }
        }, weightedParams())
        row.addView(actions)
        return row
    }

    private fun renderSettings() {
        content.addView(title("Paramètres"))
        addPreferenceSpinner(
            title = "Thème",
            values = ThemePreference.entries,
            labels = listOf("Système", "Clair", "Sombre"),
            selected = latestState.preferences.theme
        ) { value -> lifecycleScope.launch { viewModel.preferencesRepository.setTheme(value) } }
        addPreferenceSpinner(
            title = "Tri par défaut",
            values = TripSortOrder.entries,
            labels = listOf("Plus récents", "Plus anciens", "Nom"),
            selected = latestState.preferences.sortOrder
        ) { value -> lifecycleScope.launch { viewModel.preferencesRepository.setSortOrder(value) } }
        addPreferenceSpinner(
            title = "Format de date",
            values = TripDateFormat.entries,
            labels = listOf("JJ/MM/AAAA", "MM/JJ/AAAA", "AAAA-MM-JJ"),
            selected = latestState.preferences.dateFormat
        ) { value -> lifecycleScope.launch { viewModel.preferencesRepository.setDateFormat(value) } }
        content.addView(label("Les préférences sont enregistrées automatiquement."))
    }

    private fun <T : Enum<T>> addPreferenceSpinner(
        title: String,
        values: List<T>,
        labels: List<String>,
        selected: T,
        onChange: (T) -> Unit
    ) {
        content.addView(label(title, 16f))
        val spinner = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, labels)
            setSelection(values.indexOf(selected).coerceAtLeast(0), false)
        }
        spinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                values.getOrNull(position)?.takeIf { it != selected }?.let(onChange)
            }
        }
        content.addView(spinner)
    }

    private fun showTripDialog(existing: Trip? = null) {
        val name = input("Nom du voyage", existing?.name.orEmpty())
        val destination = input("Destination", existing?.destination.orEmpty())
        val description = input("Description (facultative)", existing?.description.orEmpty())
        var startDate = existing?.startDate ?: utcToday()
        var endDate = existing?.endDate ?: utcToday()
        val dateButtons = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val startButton = Button(this)
        val endButton = Button(this)
        fun updateDates() {
            startButton.text = "Début : ${TripRules.formatDateUtc(startDate, latestState.preferences.dateFormat)}"
            endButton.text = "Fin : ${TripRules.formatDateUtc(endDate, latestState.preferences.dateFormat)}"
        }
        startButton.setOnClickListener { showUtcDatePicker(startDate) { startDate = it; updateDates() } }
        endButton.setOnClickListener { showUtcDatePicker(endDate) { endDate = it; updateDates() } }
        updateDates()
        dateButtons.addView(startButton, weightedParams())
        dateButtons.addView(endButton, weightedParams())
        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), dp(4))
            addView(name)
            addView(destination)
            addView(description)
            addView(dateButtons)
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle(if (existing == null) "Nouveau voyage" else "Modifier le voyage")
            .setView(form)
            .setNegativeButton("Annuler", null)
            .setPositiveButton("Enregistrer", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val trip = (existing ?: Trip(name = "", destination = "", startDate = startDate, endDate = endDate)).copy(
                    name = name.text?.toString()?.trim().orEmpty(),
                    destination = destination.text?.toString()?.trim().orEmpty(),
                    description = description.text?.toString()?.trim().orEmpty(),
                    startDate = startDate,
                    endDate = endDate
                )
                when {
                    trip.name.isBlank() || trip.destination.isBlank() ->
                        Toast.makeText(this, "Le nom et la destination sont obligatoires.", Toast.LENGTH_SHORT).show()
                    !TripRules.hasValidDateRange(startDate, endDate) ->
                        Toast.makeText(this, "La date de fin doit être après la date de début.", Toast.LENGTH_SHORT).show()
                    !viewModel.saveTrip(trip) ->
                        Toast.makeText(this, "Les dates du voyage sont invalides.", Toast.LENGTH_SHORT).show()
                    else -> dialog.dismiss()
                }
            }
        }
        dialog.show()
    }

    private fun showUtcDatePicker(epochMillis: Long, onSelected: (Long) -> Unit) {
        val date = Instant.ofEpochMilli(epochMillis).atZone(ZoneOffset.UTC).toLocalDate()
        DatePickerDialog(this, { _, year, month, day ->
            onSelected(LocalDate.of(year, month + 1, day).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        }, date.year, date.monthValue - 1, date.dayOfMonth).show()
    }

    private fun applyTheme(theme: ThemePreference) {
        if (theme == appliedTheme) return
        appliedTheme = theme
        AppCompatDelegate.setDefaultNightMode(
            when (theme) {
                ThemePreference.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                ThemePreference.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                ThemePreference.DARK -> AppCompatDelegate.MODE_NIGHT_YES
            }
        )
    }

    private fun input(hintText: String, value: String) = TextInputEditText(this).apply {
        hint = hintText
        setText(value)
        textSize = 16f
        setPadding(dp(4), dp(8), dp(4), dp(8))
    }

    private fun title(text: String) = TextView(this).apply {
        this.text = text
        textSize = 24f
        setPadding(0, dp(8), 0, dp(12))
    }

    private fun label(text: String, size: Float = 15f) = TextView(this).apply {
        this.text = text
        textSize = size
        setPadding(0, dp(4), 0, dp(4))
    }

    private fun weightedParams() = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun utcToday(): Long = LocalDate.now(ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    private companion object {
        const val STATE_SCREEN = "selected_screen"
    }
}
