/*
 * Copyright (c) 2018-2020 SAP and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 * Contributors:
 * SAP - initial API and implementation
 */

package blog.photo.albumbuild.imagedetail

import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
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
 * Integration test for the Image Details screen.
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class ImageDetailFragmentTest {

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
    fun addedImageDetails_DisplayedInUi() {
        // GIVEN - Add аdded (non-edited) image to the DB
        val image = Image(file = "added", source = "source", isEdited = false)
        repository.saveImageBlocking(image)

        // WHEN - Details fragment launched to display image
        val bundle = ImageDetailFragmentArgs(image.id).toBundle()
        launchFragmentInContainer<ImageDetailFragment>(bundle, R.style.AppTheme)

        // THEN - Image details are displayed on the screen
        // make sure that the id is correct
        onView(withId(R.id.imagedetail_bitmap)).check(matches(isDisplayed()))
        onView(withId(R.id.imagedetail_bitmap)).check(matches(withContentDescription(image.id)))
    }

    @Test
    fun editedImageDetails_DisplayedInUi() {
        // GIVEN - Add edited image to the DB
        val image = Image(file = "edited", source = "source", isEdited = true)
        repository.saveImageBlocking(image)

        // WHEN - Details fragment launched to display image
        val bundle = ImageDetailFragmentArgs(image.id).toBundle()
        launchFragmentInContainer<ImageDetailFragment>(bundle, R.style.AppTheme)

        // THEN - Image details are displayed on the screen
        // make sure that the file/source are both shown and correct
        onView(withId(R.id.imagedetail_bitmap)).check(matches(isDisplayed()))
        onView(withId(R.id.imagedetail_bitmap)).check(matches(withContentDescription(image.id)))
    }
}
