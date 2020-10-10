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

import android.os.Bundle
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ActivityScenario.launch
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
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
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.core.IsNot.not
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.robolectric.annotation.LooperMode
import org.robolectric.annotation.TextLayoutMode

/**
 * Integration test for the Image List screen.
 */
// TODO - Use FragmentScenario, see: https://github.com/android/android-test/issues/291
@RunWith(AndroidJUnit4::class)
@MediumTest
@LooperMode(LooperMode.Mode.PAUSED)
@TextLayoutMode(TextLayoutMode.Mode.REALISTIC)
@ExperimentalCoroutinesApi
class ImagesFragmentTest {

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
    fun displayImage_whenRepositoryHasData() {
        // GIVEN - One image already in the repository
        val image = Image()
        repository.saveImageBlocking(image)

        // WHEN - On startup
        launchActivity()

        // THEN - Verify image is displayed on screen
        onView(withContentDescription(image.publicId)).check(matches(isDisplayed()))
    }

    @Test
    fun displayAddedImage() {
        // GIVEN - One image already in the repository
        val image = Image()
        repository.saveImageBlocking(image)

        // WHEN - On startup
        launchActivity()

        // THEN - Verify image is displayed on screen
        onView(withContentDescription(image.publicId)).check(matches(isDisplayed()))

        // WHEN - click filter added
        onView(withId(R.id.menu_filter_images)).perform(click())
        onView(withText(R.string.nav_added)).perform(click())

        // THEN - Verify image is displayed on screen
        onView(withContentDescription(image.publicId)).check(matches(isDisplayed()))

        // WHEN - click filter edited
        onView(withId(R.id.menu_filter_images)).perform(click())
        onView(withText(R.string.nav_edited)).perform(click())

        // THEN - Verify image is not displayed on screen
        onView(withContentDescription(image.publicId)).check(matches(not(isDisplayed())))
    }

    @Test
    fun displayEditedImage() {
        // GIVEN - One image already in the repository
        val image = Image(file = "file", source = "source", isEdited = true)
        repository.saveImageBlocking(image)

        // WHEN - On startup
        launchActivity()

        // THEN - Verify image is displayed on screen
        onView(withContentDescription(image.publicId + ":edited")).check(matches(isDisplayed()))

        // WHEN - click filter added
        onView(withId(R.id.menu_filter_images)).perform(click())
        onView(withText(R.string.nav_added)).perform(click())

        // THEN - Verify image is not displayed on screen
        onView(withContentDescription(image.publicId + ":edited")).check(matches(not(isDisplayed())))

        // WHEN - click filter edited
        onView(withId(R.id.menu_filter_images)).perform(click())
        onView(withText(R.string.nav_edited)).perform(click())

        // THEN - Verify image is displayed on screen
        onView(withContentDescription(image.publicId + ":edited")).check(matches(isDisplayed()))
    }

    @Test
    fun deleteOneImage() {
        // GIVEN - One image already in the repository
        val image = Image()
        repository.saveImageBlocking(image)

        // WHEN - On startup
        launchActivity()

        // Open it in details view
        onView(withContentDescription(image.publicId)).perform(click())

        // Click delete image in menu
        onView(withId(R.id.menu_delete_image)).perform(click())

        // Verify it was deleted
        onView(withId(R.id.menu_filter_images)).perform(click())
        onView(withText(R.string.nav_all)).perform(click())
        onView(withContentDescription(image.publicId)).check(doesNotExist())
    }

    @Test
    fun deleteOneOfTwoImages() {
        // GIVEN - Two images already in the repository
        val image1 = Image(file = "file1", source = "source1")
        val image2 = Image(file = "file2", source = "source2")
        repository.saveImageBlocking(image1)
        repository.saveImageBlocking(image2)

        // WHEN - On startup
        launchActivity()

        // Open the first in details view
        onView(withContentDescription(image1.publicId)).perform(click())

        // Click delete image in menu
        onView(withId(R.id.menu_delete_image)).perform(click())

        // Verify it was deleted
        onView(withId(R.id.menu_filter_images)).perform(click())
        onView(withText(R.string.nav_all)).perform(click())
        onView(withContentDescription(image1.publicId)).check(doesNotExist())
        // but not the other one
        onView(withContentDescription(image2.publicId)).check(matches(isDisplayed()))
    }

    @Test
    fun showAllImages() {
        // Add one added image and one edited image
        repository.saveImageBlocking(Image())
        repository.saveImageBlocking(Image(file = "file", source = "source", isEdited = true))

        // WHEN - On startup
        launchActivity()

        // THEN - Select filter all images
        onView(withId(R.id.menu_filter_images)).perform(click())
        onView(withText(R.string.nav_all)).perform(click())

        // Verify that both images are shown
        onView(allOf(withId(R.id.images_list), hasChildCount(2)))
    }

    @Test
    fun showAddedImages() {
        // Add 2 added images and one edited image
        repository.saveImageBlocking(Image())
        repository.saveImageBlocking(Image())
        repository.saveImageBlocking(Image(file = "file", source = "source", isEdited = true))

        // WHEN - On startup
        launchActivity()

        // THEN - Select filter all images
        onView(withId(R.id.menu_filter_images)).perform(click())
        onView(withText(R.string.nav_added)).perform(click())

        // Verify that the added images (but not the edited image) are shown
        onView(allOf(withId(R.id.images_list), hasChildCount(2)))
    }

    @Test
    fun showEditedImages() {
        // Add one added image and 2 edited images
        repository.saveImageBlocking(Image())
        repository.saveImageBlocking(Image(file = "file1", source = "source1", isEdited = true))
        repository.saveImageBlocking(Image(file = "file2", source = "source2", isEdited = true))

        // WHEN - On startup
        launchActivity()

        // THEN - Select filter all images
        onView(withId(R.id.menu_filter_images)).perform(click())
        onView(withText(R.string.nav_edited)).perform(click())

        // Verify that the added images (but not the edited image) are shown
        onView(allOf(withId(R.id.images_list), hasChildCount(2)))
    }

    @Test
    fun clearEditedImages() {
        // Add one added image and one edited image
        repository.saveImageBlocking(Image())
        repository.saveImageBlocking(Image(file = "file", source = "source", isEdited = true))

        // WHEN - On startup
        launchActivity()

        // Click clear edited in menu
        openActionBarOverflowOrOptionsMenu(getApplicationContext())
        onView(withText(R.string.menu_delete_edited)).perform(click())

        // THEN - Select filter all images
        onView(withId(R.id.menu_filter_images)).perform(click())
        onView(withText(R.string.nav_all)).perform(click())

        // Verify that only the added image is shown
        onView(allOf(withId(R.id.images_list), hasChildCount(1)))
    }

    @Test
    fun noImages_AllImagesFilter_TitleVisible() {
        // WHEN - On startup
        launchActivity()

        // THEN - Select filter all images
        onView(withId(R.id.menu_filter_images)).perform(click())
        onView(withText(R.string.nav_all)).perform(click())

        // Verify the "You have no images!" text is shown
        onView(withText(R.string.no_images_all)).check(matches(isDisplayed()))
    }

    @Test
    fun noImages_AddedImagesFilter_TitleVisible() {
        // WHEN - On startup
        launchActivity()

        // THEN - Select filter added images
        onView(withId(R.id.menu_filter_images)).perform(click())
        onView(withText(R.string.nav_added)).perform(click())

        // Verify the "You have no added images!" text is shown
        onView(withText(R.string.no_images_added)).check(matches((isDisplayed())))
    }

    @Test
    fun noImages_EditedImagesFilter_TitleVisible() {
        // WHEN - On startup
        launchActivity()

        // THEN - Select filter edited images
        onView(withId(R.id.menu_filter_images)).perform(click())
        onView(withText(R.string.nav_edited)).perform(click())

        // Verify the "You have no edited images!" text is shown
        onView(withText(R.string.no_images_edited)).check(matches((isDisplayed())))
    }

    @Test
    fun clickAddNewButton_navigateToAddFragment() {
        // GIVEN - On the home screen
        val scenario = launchFragmentInContainer<ImagesFragment>(Bundle(), R.style.AppTheme)
        val navController = mock(NavController::class.java)
        scenario.onFragment {
            Navigation.setViewNavController(it.view!!, navController)
        }

        // WHEN - Click on the "+" button
        onView(withId(R.id.images_add_new_fab)).perform(click())

        // THEN - Verify that we navigate to the add screen
        verify(navController).navigate(
            ImagesFragmentDirections.actionImagesFragmentToAddNewFragment()
        )
    }

    private fun launchActivity(): ActivityScenario<ImagesActivity>? {
        val activityScenario = launch(ImagesActivity::class.java)
        activityScenario.onActivity { activity ->
            // Disable animations in RecyclerView
            (activity.findViewById(R.id.images_list) as RecyclerView).itemAnimator = null
        }
        return activityScenario
    }
}
