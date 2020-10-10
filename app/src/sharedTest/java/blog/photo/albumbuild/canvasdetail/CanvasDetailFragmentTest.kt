/*
 * Copyright (c) 2018-2020 SAP and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 * Contributors:
 * SAP - initial API and implementation
 */

package blog.photo.albumbuild.canvasdetail

import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import blog.photo.albumbuild.R
import blog.photo.albumbuild.ServiceLocator
import blog.photo.albumbuild.data.Image
import blog.photo.albumbuild.data.source.FakeImageRepository
import blog.photo.albumbuild.data.source.ImagesRepository
import blog.photo.albumbuild.util.saveImageBlocking
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration test for the Canvas Details screen.
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class CanvasDetailFragmentTest {

    private lateinit var repository: ImagesRepository

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
    fun canvasDetails_DisplayedInUi() {
        // GIVEN - Add canvas to the DB
        val canvas = Image(file = "file", source = "source", isEdited = false)
        repository.saveImageBlocking(canvas)

        // WHEN - Canvas Details fragment launched to display canvas
        val bundle = CanvasDetailFragmentArgs(canvas.id).toBundle()
        launchFragmentInContainer<CanvasDetailFragment>(bundle, R.style.AppTheme)

        // THEN - Canvas is displayed on the screen
        // make sure that the id is correct
        onView(withContentDescription(canvas.id)).check(matches(isDisplayed()))
    }
}
