package com.example.tripjournal

import android.os.SystemClock
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.Matcher
import org.junit.Test
import org.junit.runner.RunWith
import android.view.View

@RunWith(AndroidJUnit4::class)
class MainNavigationTest {

    @Test
    fun menuOpensStatisticsScreen() {
        ActivityScenario.launch(MainActivity::class.java).use {
            // Act : toucher la carte « Statistiques » du menu
            onView(withId(R.id.navStats)).perform(scrollTo(), click())

            // Assert : l'écran des statistiques s'ouvre et affiche ses chiffres
            // (le JSON initial a été importé par le menu, donc il y a des voyages)
            onView(withText(R.string.stats_title)).check(matches(isDisplayed()))
            waitUntilDisplayed(withId(R.id.tileTrips))
            onView(withText(R.string.total_trips)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun menuOpensSettingsScreen() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.navSettings)).perform(scrollTo(), click())
            onView(withText(R.string.settings_title)).check(matches(isDisplayed()))
        }
    }

    /** Room et DataStore chargent en arrière-plan : on laisse jusqu'à 5 s à l'écran. */
    private fun waitUntilDisplayed(matcher: Matcher<View>) {
        val deadline = SystemClock.uptimeMillis() + 5_000
        while (true) {
            try {
                onView(matcher).check(matches(isDisplayed()))
                return
            } catch (error: Throwable) {
                if (SystemClock.uptimeMillis() > deadline) throw error
                SystemClock.sleep(100)
            }
        }
    }
}
