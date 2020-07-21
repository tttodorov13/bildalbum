/*
 * Copyright (c) 2018-2020 SAP and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 * Contributors:
 * SAP - initial API and implementation
 */

package blog.photo.buildalbum.imagedetail

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import blog.photo.buildalbum.MainCoroutineRule
import blog.photo.buildalbum.data.Image
import blog.photo.buildalbum.data.source.FakeImageRepository
import blog.photo.buildalbum.getOrAwaitValue
import blog.photo.buildalbum.observeForTesting
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for the implementation of [ImageDetailViewModel]
 */
@ExperimentalCoroutinesApi
class ImageDetailViewModelTest {

    // Subject under test
    private lateinit var imageDetailViewModel: ImageDetailViewModel

    // Use a fake images repository to be injected into the viewmodel
    private lateinit var imagesRepository: FakeImageRepository

    // Set the main coroutines dispatcher for unit testing.
    @ExperimentalCoroutinesApi
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    // Executes each image synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    val image = Image(file = "file1", source = "source1", isCanvas = false)

    @Before
    fun setupViewModel() {
        imagesRepository = FakeImageRepository()
        imagesRepository.addImages(image)

        imageDetailViewModel = ImageDetailViewModel(imagesRepository)
    }

    @Test
    fun getAddedImageFromRepositoryAndLoadIntoView() {
        imageDetailViewModel.start(image.id)

        // Then verify that the view was notified
        assertThat(imageDetailViewModel.image.getOrAwaitValue()?.file).isEqualTo(image.file)
        assertThat(imageDetailViewModel.image.getOrAwaitValue()?.source)
            .isEqualTo(image.source)
    }

    @Test
    fun editImage() {
        // TODO Test editImage
        // Load the ViewModel
        imageDetailViewModel.start(image.id)
        // Start observing to compute transformations
        imageDetailViewModel.image.getOrAwaitValue()

        // Verify that the image was added initially
        assertThat(imagesRepository.imagesServiceData[image.id]?.isEdited).isFalse()

        // Then the image is edited and the snackbar shows the correct message
//        assertSnackbarMessage(imageDetailViewModel.snackbarText, R.string.successfully_saved_image_message)
    }

    @Test
    fun editImageCancel() {
        // TODO Test editImageCancel
        // Load the ViewModel
        imageDetailViewModel.start(image.id)
        // Start observing to compute transformations
        imageDetailViewModel.image.getOrAwaitValue()

        // Verify that the image was added initially
        assertThat(imagesRepository.imagesServiceData[image.id]?.isEdited).isFalse()

        // Then the image is edited and the snackbar shows the correct message
//        assertSnackbarMessage(imageDetailViewModel.snackbarText, R.string.edit_image_cancelled_message)
    }

    @Test
    fun imageDetailViewModel_repositoryError() {
        // Given a repository that returns errors
        imagesRepository.setReturnError(true)

        // Given an initialized ViewModel with an added image
        imageDetailViewModel.start(image.id)
        // Get the computed LiveData value
        imageDetailViewModel.image.observeForTesting {
            // Then verify that data is not available
            assertThat(imageDetailViewModel.isDataAvailable.getOrAwaitValue()).isFalse()
        }
    }

    @Test
    fun updateSnackbar_nullValue() {
        // Before setting the Snackbar text, get its current value
        val snackbarText = imageDetailViewModel.snackBarText.value

        // Check that the value is null
        assertThat(snackbarText).isNull()
    }

    @Test
    fun deleteImage() {
        assertThat(imagesRepository.imagesServiceData.containsValue(image)).isTrue()
        imageDetailViewModel.start(image.id)

        // When the deletion of a image is requested
        imageDetailViewModel.deleteImage()

        assertThat(imagesRepository.imagesServiceData.containsValue(image)).isFalse()
    }

    @Test
    fun loadImage_loading() {
        // Pause dispatcher so we can verify initial values
        mainCoroutineRule.pauseDispatcher()

        // Load the image in the viewmodel
        imageDetailViewModel.start(image.id)
        // Start observing to compute transformations
        imageDetailViewModel.image.observeForTesting {
            // Force a refresh to show the loading indicator
            imageDetailViewModel.refresh()

            // Then progress indicator is shown
            assertThat(imageDetailViewModel.dataLoading.getOrAwaitValue()).isTrue()

            // Execute pending coroutines actions
            mainCoroutineRule.resumeDispatcher()

            // Then progress indicator is hidden
            assertThat(imageDetailViewModel.dataLoading.getOrAwaitValue()).isFalse()
        }
    }
}
