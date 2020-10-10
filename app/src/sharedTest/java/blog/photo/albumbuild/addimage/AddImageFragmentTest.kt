/*
 * Copyright (c) 2018-2020 SAP and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 * Contributors:
 * SAP - initial API and implementation
 */

package blog.photo.albumbuild.addimage

import android.os.Bundle
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.rule.ActivityTestRule
import blog.photo.albumbuild.R
import blog.photo.albumbuild.ServiceLocator
import blog.photo.albumbuild.data.source.FakeImageRepository
import blog.photo.albumbuild.data.source.ImagesRepository
import blog.photo.albumbuild.images.ImagesActivity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.annotation.LooperMode
import org.robolectric.annotation.TextLayoutMode

/**
 * Integration test for the Add Image screen.
 */
@RunWith(AndroidJUnit4::class)
@MediumTest
@LooperMode(LooperMode.Mode.PAUSED)
@TextLayoutMode(TextLayoutMode.Mode.REALISTIC)
@ExperimentalCoroutinesApi
class AddImageFragmentTest {

    private lateinit var imageRepository: ImagesRepository

    @get:Rule
    val intentsTestRule: ActivityTestRule<ImagesActivity> =
        IntentsTestRule(ImagesActivity::class.java)

    @Before
    fun initRepository() {
        imageRepository = FakeImageRepository()
        ServiceLocator.imagesRepository = imageRepository
    }

    @After
    fun cleanupDb() = runBlockingTest {
        ServiceLocator.resetRepository()
    }

    private fun launchFragment() {
        val navController = mock(NavController::class.java)

        val bundle = Bundle()
        val scenario = launchFragmentInContainer<AddImageFragment>(bundle, R.style.AppTheme)
        scenario.onFragment {
            Navigation.setViewNavController(it.view!!, navController)
        }
    }

    @Test
    fun validAddWithCamera() {
        // TODO Test validAddWithCamera with Intent mock
//        // GIVEN - On the "Add New" screen.
//        launchFragment()
//
//        // Create a bitmap we can use for our simulated camera image
//        val bitmap: Bitmap = BitmapFactory.decodeResource(
//            InstrumentationRegistry.getInstrumentation().targetContext.resources,
//            R.mipmap.ic_launcher
//        )
//
//        // Build a result to return from the Camera app
//        val resultData = Intent()
//        resultData.putExtra("data", bitmap)
//        val result = Instrumentation.ActivityResult(Activity.RESULT_OK, resultData)
//
//        // Stub out the Camera. When an intent is sent to the Camera, this tells Espresso to respond
//        // with the ActivityResult we just created
//        intending(toPackage("com.android.camera")).respondWith(result)
//
//        // WHEN - Valid file and source combination and click save
//        onView(withText(R.string.capture_with_camera)).perform(click())
//
//        // Using a canned RecordedIntentMatcher to validate that an intent resolving
//        // to the "phone" activity has been sent.
//        intended(toPackage("com.android.camera"))
//
//        // THEN - Verify that the repository saved the images
//        val images = (imageRepository.getImagesBlocking(true) as Result.Success).data
//        assertEquals(images.size, 1)
    }

    @Test
    fun validAddFromGallery() {
        // TODO Test validAddFromGallery with Intent mock
//        // GIVEN - On the "Add New" screen.
//        launchFragment()
//
//        // Create a bitmap we can use for our simulated camera image
//        val bitmap: Bitmap = BitmapFactory.decodeResource(
//            InstrumentationRegistry.getInstrumentation().targetContext.resources,
//            R.mipmap.ic_launcher
//        )
//
//        // Build a result to return from the Camera app
//        val resultData = Intent()
//        resultData.putExtra("data", bitmap)
//        val result = Instrumentation.ActivityResult(Activity.RESULT_OK, resultData)
//
//        // Stub out the Camera. When an intent is sent to the Camera, this tells Espresso to respond
//        // with the ActivityResult we just created
//        intending(toPackage("com.android.gallery")).respondWith(result)
//
//        // WHEN - Valid file and source combination and click save
//        onView(withContentDescription(R.string.get_from_gallery)).perform(click())
//
//        // Using a canned RecordedIntentMatcher to validate that an intent resolving
//        // to the "phone" activity has been sent.
//        intended(toPackage("com.android.gallery"))
//
//        // THEN - Verify that the repository saved the images
//        val images = (imageRepository.getImagesBlocking(true) as Result.Success).data
//        assertEquals(images.size, 1)
    }

    @Test
    fun validAddFromCloud() {
        // TODO Test validDownloadFromPixabay with Intent mock
//        // GIVEN - On the "Add New" screen.
//        launchFragment()
//
//        Thread.sleep(1000)
//
//        // WHEN - Valid file and source combination and click save
//        onView(withContentDescription(R.string.add_from_pixabay)).perform(click())
//
//        Thread.sleep(3000)
//
//        // THEN - Verify that the repository saved the images
//        val images = (imageRepository.getImagesBlocking(true) as Result.Success).data
//        assertEquals(images.size, 20)
    }

    @Test
    fun validAddCanvases() {
        // TODO Test validDownloadCanvases with Intent mock
//        // GIVEN - On the "Add New" screen.
//        launchFragment()
//
//        Thread.sleep(1000)
//
//        // WHEN - Valid file and source combination and click save
//        onView(withContentDescription(R.string.add_canvases)).perform(click())
//
//        Thread.sleep(3000)
//
//        // THEN - Verify that the repository saved the canvases
//        val canvases = (imageRepository.getCanvasesBlocking(true) as Result.Success).data
//        assertEquals(canvases.size, 9)
    }
}
