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

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import blog.photo.albumbuild.MainCoroutineRule
import blog.photo.albumbuild.R.string
import blog.photo.albumbuild.assertSnackbarMessage
import blog.photo.albumbuild.data.Image
import blog.photo.albumbuild.data.source.FakeImageRepository
import blog.photo.albumbuild.getOrAwaitValue
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for the implementation of [AddImageViewModel].
 */
@ExperimentalCoroutinesApi
class AddImageViewModelTest {

    // Subject under test
    private lateinit var addNewViewModel: AddImageViewModel

    // Use a fake images repository to be injected into the viewmodel
    private lateinit var imagesRepository: FakeImageRepository

    // Set the main coroutines dispatcher for unit testing.
    @ExperimentalCoroutinesApi
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    // Executes each image synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private val image = Image(file = "file1", source = "source1")

    @Before
    fun setupViewModel() {
        // We initialise the repository with no images
        imagesRepository = FakeImageRepository()

        // Create class under test
        addNewViewModel = AddImageViewModel(imagesRepository)
    }

    @Test
    fun loadImages_loading() {
        // Pause dispatcher so we can verify initial values
        mainCoroutineRule.pauseDispatcher()

        // Load the image in the viewmodel
        addNewViewModel.start()

        // Then progress indicator is shown
        assertThat(addNewViewModel.dataLoading.getOrAwaitValue()).isTrue()

        // Execute pending coroutines actions
        mainCoroutineRule.resumeDispatcher()

        // Then progress indicator is hidden
        assertThat(addNewViewModel.dataLoading.getOrAwaitValue()).isTrue()
    }

    @Test
    fun loadImages_imageShown() {
        // Add image to repository
        imagesRepository.addImages(image)

        // Load the image with the viewmodel
        addNewViewModel.start()

        // Verify a image is loaded
        assertThat(addNewViewModel.dataLoading.getOrAwaitValue()).isTrue()
    }

    @Test
    fun saveNewImageFromDeviceToRepository_showsSuccessMessageUi() {
        val file = "file"
        val source = "source"
        (addNewViewModel).apply {
            this.file.value = file
            this.source.value = source
        }
        addNewViewModel.imageAdd()

        val newImage = imagesRepository.imagesServiceData.values.first()

        // Then a image is saved in the repository and the view updated
        assertThat(newImage.file).isEqualTo(file)
        assertThat(newImage.source).isEqualTo(source)
    }

    @Test
    fun saveNewImageToRepository_showsSuccessMessageUi() {
        val file = "file"
        val source = "source"
        addNewViewModel.imageAdd(Image(file = file, source = source))

        val newImage = imagesRepository.imagesServiceData.values.first()

        // Then a image is saved in the repository and the view updated
        assertThat(newImage.file).isEqualTo(file)
        assertThat(newImage.source).isEqualTo(source)
    }

    @Test
    fun saveNewImageToRepository_emptyFile_error() {
        saveImageAndAssertSnackbarError("", "Some Image Source")
    }

    @Test
    fun saveNewImageToRepository_nullFile_error() {
        saveImageAndAssertSnackbarError(null, "Some Image Source")
    }

    @Test
    fun saveNewImageToRepository_emptySource_error() {
        saveImageAndAssertSnackbarError("file", "")
    }

    @Test
    fun saveNewImageToRepository_nullSource_error() {
        saveImageAndAssertSnackbarError("file", null)
    }

    @Test
    fun saveNewImageToRepository_nullSourceNullFile_error() {
        saveImageAndAssertSnackbarError(null, null)
    }

    @Test
    fun saveNewImageToRepository_emptySourceEmptyFile_error() {
        saveImageAndAssertSnackbarError("", "")
    }

    @Test
    fun saveNewImageToRepository_emptyImage_error() {
        // When saving an empty image
        addNewViewModel.imageAdd(Image())

        // Then the snackbar shows an error
        assertSnackbarMessage(addNewViewModel.snackBarText, string.empty_image_message)
    }

    private fun saveImageAndAssertSnackbarError(file: String?, source: String?) {
        (addNewViewModel).apply {
            this.file.value = file
            this.source.value = source
        }

        // When saving a non-edited image
        addNewViewModel.imageAdd()

        // Then the snackbar shows an error
        assertSnackbarMessage(addNewViewModel.snackBarText, string.empty_image_message)
    }

    @Test
    fun saveImageEmptyAndAssertSnackbarError() {
        // When saving an empty image
        addNewViewModel.imageAdd(Image())

        // Then the snackbar shows an error
        assertSnackbarMessage(addNewViewModel.snackBarText, string.empty_image_message)
    }
}
