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

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import blog.photo.albumbuild.*
import blog.photo.albumbuild.canvases.CANVAS_ADD_RESULT_OK
import blog.photo.albumbuild.data.Image
import blog.photo.albumbuild.data.source.FakeImageRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for the implementation of [ImagesViewModel]
 */
@ExperimentalCoroutinesApi
class ImagesViewModelTest {

    // Subject under test
    private lateinit var imagesViewModel: ImagesViewModel

    // Use a fake repository to be injected into the viewmodel
    private lateinit var imagesRepository: FakeImageRepository

    // Set the main coroutines dispatcher for unit testing.
    @ExperimentalCoroutinesApi
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    // Executes each image synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setupViewModel() {
        // We initialise the images to 3, with one added and two edited
        imagesRepository = FakeImageRepository()
        val image1 = Image(file = "file1", source = "source1")
        val image2 = Image(file = "file2", source = "source2", isEdited = true)
        val image3 = Image(file = "file3", source = "source3", isEdited = true)
        imagesRepository.addImages(image1, image2, image3)

        imagesViewModel = ImagesViewModel(imagesRepository)
    }

    @Test
    fun loadAllImagesFromRepository_loadingTogglesAndDataLoaded() {
        // Pause dispatcher so we can verify initial values
        mainCoroutineRule.pauseDispatcher()

        // Given an initialized ImagesViewModel with initialized images
        // When loading of Images is requested
        imagesViewModel.setFiltering(ImagesFilterType.ALL_IMAGES)

        // Trigger loading of images
        imagesViewModel.loadImages(true)
        // Observe the items to keep LiveData emitting
        imagesViewModel.images.observeForTesting {

            // Then progress indicator is shown
            assertThat(imagesViewModel.dataLoading.getOrAwaitValue()).isTrue()

            // Execute pending coroutines actions
            mainCoroutineRule.resumeDispatcher()

            // Then progress indicator is hidden
            assertThat(imagesViewModel.dataLoading.getOrAwaitValue()).isFalse()

            // And data correctly loaded
            assertThat(imagesViewModel.images.getOrAwaitValue()).hasSize(3)
        }
    }

    @Test
    fun loadAddedImagesFromRepositoryAndLoadIntoView() {
        // Given an initialized ImagesViewModel with initialized images
        // When loading of Images is requested
        imagesViewModel.setFiltering(ImagesFilterType.ADDED_IMAGES)

        // Load images
        imagesViewModel.loadImages(true)
        // Observe the items to keep LiveData emitting
        imagesViewModel.images.observeForTesting {

            // Then progress indicator is hidden
            assertThat(imagesViewModel.dataLoading.getOrAwaitValue()).isFalse()

            // And data correctly loaded
            assertThat(imagesViewModel.images.getOrAwaitValue()).hasSize(1)
        }
    }

    @Test
    fun loadEditedImagesFromRepositoryAndLoadIntoView() {
        // Given an initialized ImagesViewModel with initialized images
        // When loading of Images is requested
        imagesViewModel.setFiltering(ImagesFilterType.EDITED_IMAGES)

        // Load images
        imagesViewModel.loadImages(true)
        // Observe the items to keep LiveData emitting
        imagesViewModel.images.observeForTesting {

            // Then progress indicator is hidden
            assertThat(imagesViewModel.dataLoading.getOrAwaitValue()).isFalse()

            // And data correctly loaded
            assertThat(imagesViewModel.images.getOrAwaitValue()).hasSize(2)
        }
    }

    @Test
    fun loadImages_error() {
        // Make the repository return errors
        imagesRepository.setReturnError(true)

        // Load images
        imagesViewModel.loadImages(true)
        // Observe the items to keep LiveData emitting
        imagesViewModel.images.observeForTesting {

            // Then progress indicator is hidden
            assertThat(imagesViewModel.dataLoading.getOrAwaitValue()).isFalse()

            // And the list of items is empty
            assertThat(imagesViewModel.images.getOrAwaitValue()).isEmpty()

            // And the snackbar updated
            assertSnackbarMessage(imagesViewModel.snackbarText, R.string.loading_images_error)
        }
    }

    @Test
    fun clickOnFab_showsAddImageUi() {
        // When adding a new image
        imagesViewModel.addNewImage()

        // Then the event is triggered
        val value = imagesViewModel.newImageEvent.getOrAwaitValue()
        assertThat(value.getContentIfNotHandled()).isNotNull()
    }

    @Test
    fun clickOnOpenImage_setsEvent() {
        // When opening a new image
        val imageId = 42
        imagesViewModel.openImage(imageId)

        // Then the event is triggered
        assertImageLiveDataEventTriggered(imagesViewModel.openImageEvent, imageId)
    }

    @Test
    fun clearEditedImages_clearsImages() = mainCoroutineRule.runBlockingTest {
        // When edited images are cleared
        imagesViewModel.deleteEditedImages()

        // Fetch images
        imagesViewModel.loadImages(true)

        // Fetch images
        val allImages = imagesViewModel.images.getOrAwaitValue()
        val editedImages = allImages.filter { it.isEdited }

        // Verify there are no edited images left
        assertThat(editedImages).isEmpty()

        // Verify added image is not cleared
        assertThat(allImages).hasSize(1)

        // Verify snackbar is updated
        assertSnackbarMessage(
            imagesViewModel.snackbarText, R.string.edited_images_deleted
        )
    }

    @Test
    fun showEditResultMessages_editOk_snackbarUpdated() {
        // When the viewmodel receives a result from another destination
        imagesViewModel.showResultMessage(IMAGE_EDIT_RESULT_OK)

        // The snackbar is updated
        assertSnackbarMessage(
            imagesViewModel.snackbarText, R.string.successfully_image_edited_message
        )
    }

    @Test
    fun showEditResultMessages_addOk_snackbarUpdated() {
        // When the viewmodel receives a result from another destination
        imagesViewModel.showResultMessage(IMAGE_ADD_RESULT_OK)

        // The snackbar is updated
        assertSnackbarMessage(
            imagesViewModel.snackbarText, R.string.successfully_image_added_message
        )
    }

    @Test
    fun showAddResultMessages_addCanvasOk_snackbarUpdated() {
        // When the viewmodel receives a result from another destination
        imagesViewModel.showResultMessage(CANVAS_ADD_RESULT_OK)

        // The snackbar is updated
        assertSnackbarMessage(
            imagesViewModel.snackbarText, R.string.successfully_canvases_downloaded_message
        )
    }

    @Test
    fun showEditResultMessages_deleteOk_snackbarUpdated() {
        // When the viewmodel receives a result from another destination
        imagesViewModel.showResultMessage(IMAGE_DELETE_RESULT_OK)

        // The snackbar is updated
        assertSnackbarMessage(
            imagesViewModel.snackbarText,
            R.string.successfully_image_deleted_message
        )
    }

    @Test
    fun getImagesAddViewVisible() {
        // When the filter type is ALL_IMAGES
        imagesViewModel.setFiltering(ImagesFilterType.ALL_IMAGES)

        // Then the "Add image" action is visible
        assertThat(imagesViewModel.imagesAddViewVisible.getOrAwaitValue()).isTrue()
    }
}
