package com.example.tripjournal.ui.settings

import android.os.Bundle
import android.widget.RadioGroup
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.tripjournal.R
import com.example.tripjournal.data.settings.SettingsManager
import com.example.tripjournal.ui.places.applySystemBarPadding
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    private val viewModel: SettingsViewModel by viewModels {
        SettingsViewModel.Factory(SettingsManager.getInstance(this))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()  // icônes de la barre d'état adaptées au thème clair / sombre
        setContentView(R.layout.activity_settings)
        findViewById<android.view.ViewGroup>(android.R.id.content).getChildAt(0).applySystemBarPadding()

        val toolbar = findViewById<MaterialToolbar>(R.id.settingsToolbar)
        toolbar.setNavigationOnClickListener { finish() }

        val themeRadioGroup = findViewById<RadioGroup>(R.id.themeRadioGroup)
        val sortRadioGroup = findViewById<RadioGroup>(R.id.sortRadioGroup)
        val unitRadioGroup = findViewById<RadioGroup>(R.id.unitRadioGroup)
        val confirmDeleteSwitch = findViewById<SwitchMaterial>(R.id.confirmDeleteSwitch)
        val resetDataButton = findViewById<MaterialButton>(R.id.resetDataButton)

        // Ecouteurs UI -> ViewModel
        themeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val mode = when (checkedId) {
                R.id.themeLight -> SettingsManager.THEME_LIGHT
                R.id.themeDark -> SettingsManager.THEME_DARK
                else -> SettingsManager.THEME_SYSTEM
            }
            if (viewModel.themeMode.value != mode) {
                viewModel.setThemeMode(mode)
            }
        }

        sortRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val sort = when (checkedId) {
                R.id.sortDateAsc -> SettingsManager.SORT_DATE_ASC
                R.id.sortNameAsc -> SettingsManager.SORT_NAME_ASC
                R.id.sortDestinationAsc -> SettingsManager.SORT_DESTINATION_ASC
                else -> SettingsManager.SORT_DATE_DESC
            }
            if (viewModel.defaultSort.value != sort) {
                viewModel.setDefaultSort(sort)
            }
        }

        unitRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val unit = if (checkedId == R.id.unitMiles) SettingsManager.UNIT_MILES else SettingsManager.UNIT_KM
            if (viewModel.distanceUnit.value != unit) {
                viewModel.setDistanceUnit(unit)
            }
        }

        confirmDeleteSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (viewModel.confirmDelete.value != isChecked) {
                viewModel.setConfirmDelete(isChecked)
            }
        }

        resetDataButton.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(R.string.reset_data)
                .setMessage(R.string.reset_data_confirm)
                .setPositiveButton(R.string.confirm) { _, _ ->
                    viewModel.resetData()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }

        // Observation des StateFlow
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.themeMode.filterNotNull().collect { mode ->
                        when (mode) {
                            SettingsManager.THEME_LIGHT -> themeRadioGroup.check(R.id.themeLight)
                            SettingsManager.THEME_DARK -> themeRadioGroup.check(R.id.themeDark)
                            else -> themeRadioGroup.check(R.id.themeSystem)
                        }
                    }
                }
                launch {
                    viewModel.defaultSort.filterNotNull().collect { sort ->
                        when (sort) {
                            SettingsManager.SORT_DATE_ASC -> sortRadioGroup.check(R.id.sortDateAsc)
                            SettingsManager.SORT_NAME_ASC -> sortRadioGroup.check(R.id.sortNameAsc)
                            SettingsManager.SORT_DESTINATION_ASC -> sortRadioGroup.check(R.id.sortDestinationAsc)
                            else -> sortRadioGroup.check(R.id.sortDateDesc)
                        }
                    }
                }
                launch {
                    viewModel.distanceUnit.filterNotNull().collect { unit ->
                        if (unit == SettingsManager.UNIT_MILES) {
                            unitRadioGroup.check(R.id.unitMiles)
                        } else {
                            unitRadioGroup.check(R.id.unitKm)
                        }
                    }
                }
                launch {
                    viewModel.confirmDelete.filterNotNull().collect { confirm ->
                        confirmDeleteSwitch.isChecked = confirm
                    }
                }
                launch {
                    viewModel.resetResult.collect { result ->
                        val count = result?.toInsert?.size ?: 0
                        Snackbar.make(
                            findViewById(android.R.id.content),
                            getString(R.string.trips_imported, count, result?.invalid ?: 0, result?.duplicates ?: 0),
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }
}
