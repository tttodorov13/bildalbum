/*
 * Copyright (c) 2018-2020 SAP and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 * Contributors:
 * SAP - initial API and implementation
 */

package blog.photo.albumbuild.canvases

import android.os.Bundle
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import blog.photo.albumbuild.R
import blog.photo.albumbuild.ServiceLocator
import blog.photo.albumbuild.data.Image
import blog.photo.albumbuild.data.source.FakeImageRepository
import blog.photo.albumbuild.data.source.ImagesRepository
import blog.photo.albumbuild.util.DataBindingIdlingResource
import blog.photo.albumbuild.util.monitorFragment
import blog.photo.albumbuild.util.saveImageBlocking
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.allOf
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.annotation.LooperMode
import org.robolectric.annotation.TextLayoutMode

/**
 * Integration test for the Canvas List screen.
 */
@RunWith(AndroidJUnit4::class)
@MediumTest
@LooperMode(LooperMode.Mode.PAUSED)
@TextLayoutMode(TextLayoutMode.Mode.REALISTIC)
@ExperimentalCoroutinesApi
class CanvasesFragmentTest {

    private lateinit var repository: ImagesRepository

    // An Idling Resource that waits for Data Binding to have no pending bindings
    private val dataBindingIdlingResource = DataBindingIdlingResource()

    @Before
    fun initRepository() {
        repository = FakeImageRepository()
        ServiceLocator.imagesRepository = repository
    }

    @After
    fun cleanupDb() = runBlockingTest {
        ServiceLocator.resetRepository()
    }

    @Test
    fun displayCanvas_whenRepositoryHasData() {
        // TODO displayCanvas_whenRepositoryHasData
//        // GIVEN - Add canvas to the DB
//        val canvas = Image()
//        repository.saveImageBlocking(canvas)
//
//        // WHEN - Canvass fragment launched to display canvas
//        launchCanvasesFragment()
//
//        // THEN - Verify canvas is displayed on screen
//        onView(withContentDescription(canvas.publicId)).check(matches(isDisplayed()))
    }

    @Test
    fun deleteOneCanvas() {
        // TODO Test deleteOneCanvas
    }

    @Test
    fun deleteOneOfTwoCanvases() {
        // TODO Test deleteOneOfTwoCanvases
    }

    @Test
    fun showAllCanvases() {
        // TODO showAllCanvases
//        // GIVEN - Add 2 canvass to the DB
//        val canvas1 = Image()
//        val canvas2 = Image()
//        repository.apply {
//            saveImageBlocking(canvas1)
//            saveImageBlocking(canvas2)
//        }
//
//        // WHEN - Canvases fragment launched to display canvas
//        launchCanvasesFragment()
//
//        // THEN - Verify that both of the canvass are shown
//        onView(allOf(withId(R.id.canvases_list), hasChildCount(2)))
    }


    @Test
    fun clickAddNewButton() {
        // TODO clickAddNewButton
//        // GIVEN - On the home screen
//        val scenario = launchFragmentInContainer<CanvasesFragment>(Bundle(), R.style.AppTheme)
//        val navController = Mockito.mock(NavController::class.java)
//        scenario.onFragment {
//            Navigation.setViewNavController(it.view!!, navController)
//        }
//
//        // WHEN - Click on the "+" button
//        onView(withId(R.id.canvases_add_new_fab)).perform(ViewActions.click())
//
//        // THEN - Verify that we navigate to the add screen
//        Mockito.verify(navController).navigate(
//            CanvasesFragmentDirections.actionCanvasesFragmentToAddNewFragment()
//        )
    }

    private fun launchCanvasesFragment(): FragmentScenario<CanvasesFragment>? {
        val scenario = launchFragmentInContainer<CanvasesFragment>(Bundle(), R.style.AppTheme)
        dataBindingIdlingResource.monitorFragment(scenario)

        return scenario
    }
}
