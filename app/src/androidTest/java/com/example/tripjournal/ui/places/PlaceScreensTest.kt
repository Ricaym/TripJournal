package com.example.tripjournal.ui.places

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.tripjournal.R
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlaceScreensTest {

    @Test
    fun emptyFormShowsValidationMessage() {
        ActivityScenario.launch(PlaceFormActivity::class.java).use {
            // Act : enregistrer sans rien remplir
            onView(withId(R.id.savePlaceButton)).perform(scrollTo(), click())
            // Assert : le message d'erreur s'affiche
            onView(withText(R.string.place_invalid)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun addedPlaceAppearsInList() {
        // Arrange : nom unique, commençant par AAA pour être en haut de la liste triée
        val name = "AAA Espresso ${System.currentTimeMillis()}"

        ActivityScenario.launch(PlaceListActivity::class.java).use {
            // Act : ouvrir le formulaire, remplir, enregistrer
            onView(withId(R.id.addPlaceButton)).perform(click())
            onView(withId(R.id.placeNameInput)).perform(replaceText(name))
            onView(withId(R.id.placeCityInput)).perform(replaceText("Lisbonne"))
            onView(withId(R.id.placeCountryInput))
                .perform(replaceText("Portugal"), closeSoftKeyboard())
            onView(withId(R.id.savePlaceButton)).perform(scrollTo(), click())

            // Assert : retour à la liste, et le nouveau lieu y figure
            waitUntilDisplayed(name)
        }
    }

    /** Room écrit en arrière-plan : on laisse jusqu'à 3 s à la liste pour se mettre à jour. */
    private fun waitUntilDisplayed(text: String) {
        repeat(30) {
            try {
                onView(withText(text)).check(matches(isDisplayed()))
                return
            } catch (e: Throwable) {
                Thread.sleep(100)
            }
        }
        onView(withText(text)).check(matches(isDisplayed()))
    }
}