package com.example.tripjournal

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.tripjournal.ui.settings.SettingsActivity
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsNavigationTest {

    @Test
    fun settingsScreenAndPreferencesRemainAvailableAfterRecreation() {
        // 1) Navigation : le menu principal ouvre bien l'écran Réglages
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withText("Réglages")).perform(click())
            onView(withText("Tri par défaut des voyages")).check(matches(isDisplayed()))
        }

        // 2) Rotation : l'écran Réglages est recréé sans perdre ses sections
        ActivityScenario.launch(SettingsActivity::class.java).use { scenario ->
            scenario.recreate()
            onView(withText("Tri par défaut des voyages")).check(matches(isDisplayed()))
            onView(withText("Format de date")).check(matches(isDisplayed()))
        }
    }
}
