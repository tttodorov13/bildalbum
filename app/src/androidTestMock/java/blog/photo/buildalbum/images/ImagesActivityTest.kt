package blog.photo.buildalbum.images

import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import blog.photo.buildalbum.R
import blog.photo.buildalbum.ServiceLocator
import blog.photo.buildalbum.data.Image
import blog.photo.buildalbum.data.source.ImagesRepository
import blog.photo.buildalbum.util.*
import org.hamcrest.CoreMatchers
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Large End-to-End test for the images module.
 *
 * UI tests usually use [ActivityTestRule] but there's no API to perform an action before
 * each test. The workaround is to use `ActivityScenario.launch()` and `ActivityScenario.close()`.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class ImagesActivityTest {

    private lateinit var repository: ImagesRepository

    // An Idling Resource that waits for Data Binding to have no pending bindings
    private val dataBindingIdlingResource = DataBindingIdlingResource()

    @Before
    fun init() {
        repository = ServiceLocator.provideImagesRepository(getApplicationContext())
        repository.deleteAllImagesBlocking()
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
    fun editImage() {
        // TODO: Test editImage
//        repository.saveImageBlocking(Image("file1", "source"))
//
//        // start up Images screen
//        val activityScenario = ActivityScenario.launch(ImagesActivity::class.java)
//        dataBindingIdlingResource.monitorActivity(activityScenario)
//
//        // Click on the image on the list and verify that all the data is correct
//        onView(withText("file1")).perform(click())
//        onView(withId(R.id.image_detail_file_text)).check(matches(withText("file1")))
//        onView(withId(R.id.image_detail_source_text)).check(matches(withText("source")))
//        onView(withId(R.id.image_detail_edit_checkbox)).check(matches(not(isChecked())))
//
//        // Click on the edit button, edit, and save
//        onView(withId(R.id.edit_image_fab)).perform(click())
////        onView(withId(R.id.add_image_file_edit_text)).perform(replaceText("new file"))
////        onView(withId(R.id.add_image_source_edit_text)).perform(replaceText("new source"))
////        onView(withId(R.id.save_image_fab)).perform(click())
//
//        // Verify image is displayed on screen in the image list.
//        onView(withText("new file")).check(matches(isDisplayed()))
//        // Verify previous image is not displayed
//        onView(withText("file1")).check(doesNotExist())
//        // Make sure the activity is closed before resetting the db:
//        activityScenario.close()
    }

    @Test
    fun createOneImage_deleteImage() {
        // Add 1 image
        val image = Image()
        repository.saveImageBlocking(image)

        // start up Images screen
        val activityScenario = ActivityScenario.launch(ImagesActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        // Open it in details view
        onView(withContentDescription(image.publicId)).perform(click())
        // Click delete image in menu
        onView(withId(R.id.menu_delete)).perform(click())

        // Verify it was deleted
        onView(withId(R.id.menu_filter)).perform(click())
        onView(withText(R.string.nav_all)).perform(click())
        onView(CoreMatchers.allOf(withId(R.id.images_list), hasChildCount(0)))

        // Make sure the activity is closed before resetting the db:
        activityScenario.close()
    }

    @Test
    fun createTwoImages_deleteOneImage() {
        // Add 2 images
        val image1 = Image()
        val image2 = Image()
        repository.saveImageBlocking(image1)
        repository.saveImageBlocking(image2)

        // start up Images screen
        val activityScenario = ActivityScenario.launch(ImagesActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        // Open it in details view
        onView(withContentDescription(image1.publicId)).perform(click())
        // Click delete image in menu
        onView(withId(R.id.menu_delete)).perform(click())

        // Verify it was deleted
        onView(withId(R.id.menu_filter)).perform(click())
        onView(withText(R.string.nav_all)).perform(click())
        onView(CoreMatchers.allOf(withId(R.id.images_list), hasChildCount(1)))

        // Make sure the activity is closed before resetting the db:
        activityScenario.close()
    }

    @Test
    fun saveImageOnDetailScreen_newImageIsInAllImagesListAndEditedImagesList() {
        // TODO: Test saveImageOnDetailScreen_newImageIsInAllImagesListAndEditedImagesList
//        // Add 1 edited image
//        val imageFile = "COMP-ACT"
//        repository.saveImageBlocking(Image(imageFile, "source", true))
//
//        // start up Images screen
//        val activityScenario = ActivityScenario.launch(ImagesActivity::class.java)
//        dataBindingIdlingResource.monitorActivity(activityScenario)
//
//        // Click on the image on the list
//        onView(withText(imageFile)).perform(click())
//        // Click on the checkbox in image details screen
//        onView(withId(R.id.image_detail_edit_checkbox)).perform(click())
//        // Click again to restore it to original state
//        onView(withId(R.id.image_detail_edit_checkbox)).perform(click())
//
//        // Click on the navigation up button to go back to the list
//        onView(
//            withContentDescription(
//                activityScenario.getToolbarNavigationContentDescription()
//            )
//        ).perform(click())
//
//        // Check that the image is marked as added
//        onView(allOf(withId(R.id.edit_checkbox), hasSibling(withText(imageFile))))
//            .check(matches(isChecked()))
//        // Make sure the activity is closed before resetting the db:
//        activityScenario.close()
    }

    @Test
    fun downloadImagesFromFlickr() {
        // start up Images screen
        val activityScenario = ActivityScenario.launch(ImagesActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        // Click on the "+" button
        onView(withId(R.id.images_add_new_fab)).perform(click())

        // Click on the "Latest From FLICKR" button
        onView(withId(R.id.download_from_flickr)).perform(click())

        // Then verify the images are displayed on the screen
        onView(CoreMatchers.allOf(withId(R.id.images_list), hasChildCount(20)))

        // Make sure the activity is closed before resetting the db:
        activityScenario.close()
    }

    @Test
    fun downloadImagesFromPixabay() {
        // start up Images screen
        val activityScenario = ActivityScenario.launch(ImagesActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        // Click on the "+" button
        onView(withId(R.id.images_add_new_fab)).perform(click())

        // Click on the "Latest From FLICKR" button
        onView(withId(R.id.download_from_pixabay)).perform(click())

        // Then verify the images are displayed on the screen
        onView(CoreMatchers.allOf(withId(R.id.images_list), hasChildCount(20)))

        // Make sure the activity is closed before resetting the db:
        activityScenario.close()
    }

    @Test
    fun downloadCanvases() {
        // TODO Test downloadCanvases
        // start up Images screen
        val activityScenario = ActivityScenario.launch(ImagesActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        // Click on the "+" button
        onView(withId(R.id.images_add_new_fab)).perform(click())

        // Click on the "Latest From FLICKR" button
        onView(withId(R.id.download_canvases)).perform(click())

        // navigate to Canvases screen
        // Then verify the images are displayed on the screen
//        onView(CoreMatchers.allOf(withId(R.id.images_list), hasChildCount(20)))

        // Make sure the activity is closed before resetting the db:
        activityScenario.close()
    }
}
