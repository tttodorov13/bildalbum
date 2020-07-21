/*
 * Copyright (c) 2018-2020 SAP and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 * Contributors:
 * SAP - initial API and implementation
 */

package blog.photo.buildalbum.statistics

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import blog.photo.buildalbum.R
import blog.photo.buildalbum.ServiceLocator
import blog.photo.buildalbum.data.Image
import blog.photo.buildalbum.data.source.FakeImageRepository
import blog.photo.buildalbum.data.source.ImagesRepository
import blog.photo.buildalbum.util.DataBindingIdlingResource
import blog.photo.buildalbum.util.monitorFragment
import blog.photo.buildalbum.util.saveImageBlocking
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration test for the statistics screen.
 */
@RunWith(AndroidJUnit4::class)
@MediumTest
@ExperimentalCoroutinesApi
class StatisticsFragmentTest {
    private lateinit var imageRepository: ImagesRepository

    // An Idling Resource that waits for Data Binding to have no pending bindings
    private val dataBindingIdlingResource = DataBindingIdlingResource()

    @Before
    fun initRepository() {
        imageRepository = FakeImageRepository()
        ServiceLocator.imagesRepository = imageRepository
    }

    @After
    fun cleanupDb() = runBlockingTest {
        ServiceLocator.resetRepository()
    }

    /**
     * Idling resources tell Espresso that the app is idle or busy. This is needed when operations
     * are not scheduled in the main Looper (for example when executed on a different thread).
     */
    @Before
    fun registerIdlingResource() {
        IdlingRegistry.getInstance().register(dataBindingIdlingResource)
    }

    /**
     * Unregister your Idling Resource so it can be garbage collected and does not leak any memory.
     */
    @After
    fun unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(dataBindingIdlingResource)
    }

    @Test
    fun images_showsNonEmptyMessage() {
        // Given some images
        imageRepository.apply {
            saveImageBlocking(Image(file = "file1", source = "source1", isEdited = false))
            saveImageBlocking(Image(file = "file2", source = "source2", isEdited = true))
        }

        val scenario =
            launchFragmentInContainer<StatisticsFragment>(Bundle(), R.style.AppTheme)
        dataBindingIdlingResource.monitorFragment(scenario)

        val expectedAddedImageText = getApplicationContext<Context>()
            .getString(R.string.statistics_images_added, 50.0f)
        val expectedEditedImageText = getApplicationContext<Context>()
            .getString(R.string.statistics_images_edited, 50.0f)

        // check that both info boxes are displayed and contain the correct info
        onView(withId(R.id.stats_added_text)).check(matches(isDisplayed()))
        onView(withId(R.id.stats_added_text)).check(matches(withText(expectedAddedImageText)))
        onView(withId(R.id.stats_edited_text)).check(matches(isDisplayed()))
        onView(withId(R.id.stats_edited_text))
            .check(matches(withText(expectedEditedImageText)))
    }
}
