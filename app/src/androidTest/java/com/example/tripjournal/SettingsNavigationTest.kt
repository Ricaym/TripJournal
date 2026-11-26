package com.example.tripjournal

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsNavigationTest {
    @Test
    fun settingsScreenAndPreferencesRemainAvailableAfterRecreation() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        try {
            onView(withText("Réglages")).perform(click())
            onView(withText("Tri par défaut des voyages")).check(matches(isDisplayed()))
            scenario.recreate()
            onView(withText("Tri par défaut des voyages")).check(matches(isDisplayed()))
            onView(withText("Format de date")).check(matches(isDisplayed()))
        } finally {
            scenario.close()
        }
    }
}
