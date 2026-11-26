package com.example.tripjournal.ui.settings

import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.tripjournal.R
import com.example.tripjournal.data.settings.SettingsManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Suppression soumise à la préférence « Confirmer la suppression » (DataStore) :
 * si elle est activée, un dialogue demande confirmation ; sinon on supprime directement.
 * La valeur est lue au moment du clic, donc un changement dans les Réglages
 * s'applique immédiatement, sans redémarrer l'écran.
 */
fun AppCompatActivity.deleteWithOptionalConfirmation(
    @StringRes message: Int,
    onDelete: () -> Unit
) {
    lifecycleScope.launch {
        val mustConfirm = SettingsManager.getInstance(this@deleteWithOptionalConfirmation)
            .confirmDelete.first()
        if (!mustConfirm) {
            onDelete()
            return@launch
        }
        MaterialAlertDialogBuilder(this@deleteWithOptionalConfirmation)
            .setMessage(message)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.delete) { _, _ -> onDelete() }
            .show()
    }
}
