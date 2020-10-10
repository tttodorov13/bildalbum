/*
 * Copyright (c) 2018-2020 SAP and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 * Contributors:
 * SAP - initial API and implementation
 */

package blog.photo.albumbuild.images

import android.view.Gravity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.DrawerActions.open
import androidx.test.espresso.contrib.DrawerMatchers.isClosed
import androidx.test.espresso.contrib.DrawerMatchers.isOpen
import androidx.test.espresso.contrib.NavigationViewActions.navigateTo
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import blog.photo.albumbuild.R
import blog.photo.albumbuild.ServiceLocator
import blog.photo.albumbuild.data.Image
import blog.photo.albumbuild.data.source.ImagesRepository
import blog.photo.albumbuild.util.DataBindingIdlingResource
import blog.photo.albumbuild.util.EspressoIdlingResource
import blog.photo.albumbuild.util.monitorActivity
import blog.photo.albumbuild.util.saveImageBlocking
import org.hamcrest.CoreMatchers.allOf
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests for the [DrawerLayout] layout component in [ImagesActivity] which manages
 * navigation within the app.
 *
 * UI tests usually use [ActivityTestRule] but there's no API to perform an action before
 * each test. The workaround is to use `ActivityScenario.launch()` and `ActivityScenario.close()`.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class AppNavigationTest {

    private lateinit var imagesRepository: ImagesRepository

    // An Idling Resource that waits for Data Binding to have no pending bindings
    private val dataBindingIdlingResource = DataBindingIdlingResource()

    @Before
    fun init() {
        imagesRepository = ServiceLocator.provideImagesRepository(getApplicationContext())
    }

    @After
    fun reset() {
        ServiceLocator.resetRepository()
    }

    /**
     * Idling resources tell Espresso that the app is idle or busy. This is needed when operations
     * are not scheduled in the main Looper (for example when executed on a different thread).
     */
    @Before
    fun registerIdlingResource() {
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().register(dataBindingIdlingResource)
    }

    /**
     * Unregister your Idling Resource so it can be garbage collected and does not leak any memory.
     */
    @After
    fun unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().unregister(dataBindingIdlingResource)
    }

    @Test
    fun drawerNavigationFromImagesToStatistics() {
        // start up Images screen
        val activityScenario = ActivityScenario.launch(ImagesActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        onView(withId(R.id.image_drawer_layout))
            .check(matches(isClosed(Gravity.START))) // Left Drawer should be closed.
            .perform(open()) // Open Drawer

        // Start statistics screen.
        onView(withId(R.id.albumbuild_nav_view))
            .perform(navigateTo(R.id.statistics_fragment_dest))

        // Check that statistics screen was opened.
        onView(withId(R.id.image_statistics_layout)).check(matches(isDisplayed()))

        Thread.sleep(100)
        onView(withId(R.id.image_drawer_layout))
            .check(matches(isClosed(Gravity.START))) // Left Drawer should be closed.
            .perform(open()) // Open Drawer

        // Start images screen.
        onView(withId(R.id.albumbuild_nav_view))
            .perform(navigateTo(R.id.images_fragment_dest))

        // Check that images screen was opened.
        onView(withId(R.id.images_container_layout)).check(matches(isDisplayed()))

        // When using ActivityScenario.launch, always call close()
        activityScenario.close()
    }

    @Test
    fun imagesScreen_clickOnAndroidHomeIcon_OpensNavigation() {
        // start up Images screen
        val activityScenario = ActivityScenario.launch(ImagesActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        // Check that left drawer is closed at startup
        onView(withId(R.id.image_drawer_layout))
            .check(matches(isClosed(Gravity.START))) // Left Drawer should be closed.

        // Open Drawer
        onView(
            withContentDescription(
                activityScenario
                    .getToolbarNavigationContentDescription()
            )
        ).perform(click())

        // Check if image drawer is open
        onView(withId(R.id.image_drawer_layout))
            .check(matches(isOpen(Gravity.START))) // Left drawer is open.
        // When using ActivityScenario.launch, always call close()
        activityScenario.close()
    }

    @Test
    fun imagesScreen_clickOnAndroidHomeIcon_clickOnCanvasList_OpensCanvassScreen() {
        // start up Images screen
        val activityScenario = ActivityScenario.launch(ImagesActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        // Check that left drawer is closed at startup
        onView(withId(R.id.image_drawer_layout))
            .check(matches(isClosed(Gravity.START))) // Left Drawer should be closed.

        // Open Drawer
        onView(
            withContentDescription(
                activityScenario
                    .getToolbarNavigationContentDescription()
            )
        ).perform(click())

        // Check if image drawer is open
        onView(withId(R.id.image_drawer_layout))
            .check(matches(isOpen(Gravity.START))) // Left Drawer is open.

        // Then open Canvases List
        onView(withText(R.string.canvases)).perform(click())

        // Verify Canvases List is shown
        onView(withId(R.id.canvases_container_layout)).check(matches(isDisplayed()))

        // When using ActivityScenario.launch, always call close()
        activityScenario.close()
    }

    @Test
    fun imageStatsScreen_clickOnAndroidHomeIcon_OpensNavigation() {
        // start up Images screen
        val activityScenario = ActivityScenario.launch(ImagesActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        // When the user navigates to the stats screen
        activityScenario.onActivity {
            it.findNavController(R.id.nav_host_fragment).navigate(R.id.statistics_fragment_dest)
        }

        // Then check that left drawer is closed at startup
        onView(withId(R.id.image_drawer_layout))
            .check(matches(isClosed(Gravity.START))) // Left Drawer should be closed.

        // When the drawer is opened
        onView(
            withContentDescription(
                activityScenario
                    .getToolbarNavigationContentDescription()
            )
        ).perform(click())

        // Then check that the drawer is open
        onView(withId(R.id.image_drawer_layout))
            .check(matches(isOpen(Gravity.START))) // Left drawer is open open.

        // When using ActivityScenario.launch, always call close()
        activityScenario.close()
    }

    @Test
    fun imageDetailsScreen_clickOnBackButton() {
        // TODO Test imageDetailsScreen_clickOnBackButton
        val image = Image()
        imagesRepository.saveImageBlocking(image)

        // start up Images screen
        val activityScenario = ActivityScenario.launch(ImagesActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        // Click on the image on the list
//        onView(withContentDescription(image.id)).perform(click())
//
//        // Confirm that if we click "<-", we end up back at the home screen
//        onView(
//            withContentDescription(
//                activityScenario
//                    .getImagesToolbarNavigationContentDescription()
//            )
//        ).perform(click())
//        onView(withId(R.id.images_container_layout)).check(matches(isDisplayed()))

        // When using ActivityScenario.launch, always call close()
        activityScenario.close()
    }

    @Test
    fun imageDetailsScreen_clickOnDeleteButton() {
        // TODO Test imageDetailsScreen_clickOnDeleteButton
        val image = Image()
        imagesRepository.saveImageBlocking(image)

        // start up Images screen
        val activityScenario = ActivityScenario.launch(ImagesActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        // Click on the image on the list
        onView(withId(R.id.images_list)).check(matches(isDisplayed()))
        onView(allOf(withId(R.id.images_list), hasChildCount(1)))

//        onView(withContentDescription(image.id)).perform(click())
//
//        // Confirm that if we click button delete, we end up back at the home screen
//        onView(withId(R.id.menu_delete)).perform(click())
//        onView(withId(R.id.images_container_layout)).check(matches(isDisplayed()))
//        // and no image is show
//        onView(withContentDescription(R.string.no_images)).check(matches(isDisplayed()))

        // When using ActivityScenario.launch, always call close()
        activityScenario.close()
    }

    @Test
    fun imageDetailsScreen_clickOnFAB() {
        // TODO Test imageDetailsScreen_clickOnFAB
        val image = Image()
        imagesRepository.saveImageBlocking(image)

        // start up Images screen
        val activityScenario = ActivityScenario.launch(ImagesActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

//        // Click on the image on the list
//        onView(withContentDescription(image.id)).perform(click())
//
//        // Confirm that if we click "\/", we end up back at the home screen
//        onView(
//            withId(R.id.image_detail_fab)
//        ).perform(click())
//        onView(withId(R.id.images_container_layout)).check(matches(isDisplayed()))
//
//        // When using ActivityScenario.launch, always call close()
//        activityScenario.close()
    }
}
