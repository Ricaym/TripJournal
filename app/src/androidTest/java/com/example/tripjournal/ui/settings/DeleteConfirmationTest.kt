package com.example.tripjournal.ui.settings

import android.os.SystemClock
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.tripjournal.R
import com.example.tripjournal.data.database.TripJournalDatabase
import com.example.tripjournal.data.database.entity.Place
import com.example.tripjournal.data.settings.SettingsManager
import com.example.tripjournal.ui.places.PlaceFormActivity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

/** La préférence DataStore « Confirmer la suppression » change le comportement du bouton Supprimer. */
@RunWith(AndroidJUnit4::class)
class DeleteConfirmationTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val settings = SettingsManager.getInstance(context)
    private val placeDao = TripJournalDatabase.get(context).placeDao()

    private fun insertPlace(): Long = runBlocking {
        placeDao.insert(Place(name = "Test suppression ${System.nanoTime()}", city = "Lyon", country = "France"))
    }

    @After
    fun restoreDefault() = runBlocking { settings.setConfirmDelete(true) }

    @Test
    fun confirmationEnabledShowsDialogAndKeepsPlace() {
        runBlocking { settings.setConfirmDelete(true) }
        val id = insertPlace()

        ActivityScenario.launch<PlaceFormActivity>(PlaceFormActivity.newIntent(context, id)).use {
            onView(withId(R.id.deletePlaceButton)).perform(scrollTo(), click())

            // Le dialogue s'affiche, et rien n'est supprimé tant qu'on n'a pas confirmé
            onView(withText(R.string.delete_place_confirm)).check(matches(isDisplayed()))
            assertNotNull(runBlocking { placeDao.getById(id) })
        }
        runBlocking { placeDao.getById(id)?.let { placeDao.delete(it) } }
    }

    @Test
    fun confirmationDisabledDeletesImmediately() {
        runBlocking { settings.setConfirmDelete(false) }
        val id = insertPlace()

        ActivityScenario.launch<PlaceFormActivity>(PlaceFormActivity.newIntent(context, id)).use {
            onView(withId(R.id.deletePlaceButton)).perform(scrollTo(), click())

            // Pas de dialogue : le lieu disparaît de la base directement
            val deadline = SystemClock.uptimeMillis() + 3_000
            while (runBlocking { placeDao.getById(id) } != null && SystemClock.uptimeMillis() < deadline) {
                SystemClock.sleep(100)
            }
            assertNull(runBlocking { placeDao.getById(id) })
        }
    }
}
