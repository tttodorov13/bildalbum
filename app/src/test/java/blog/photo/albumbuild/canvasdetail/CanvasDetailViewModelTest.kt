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

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import blog.photo.albumbuild.MainCoroutineRule
import blog.photo.albumbuild.data.Image
import blog.photo.albumbuild.data.Result.Success
import blog.photo.albumbuild.data.source.FakeImageRepository
import blog.photo.albumbuild.getOrAwaitValue
import blog.photo.albumbuild.observeForTesting
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for the implementation of [CanvasDetailViewModel]
 */
@ExperimentalCoroutinesApi
class CanvasDetailViewModelTest {

    // Subject under test
    private lateinit var canvasDetailViewModel: CanvasDetailViewModel

    // Use a fake repository to be injected into the viewmodel
    private lateinit var imagesRepository: FakeImageRepository

    // Set the main coroutines dispatcher for unit testing.
    @ExperimentalCoroutinesApi
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    // Executes each canvas synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private val canvas = Image(file = "Title1", source = "Description1")

    @Before
    fun setupViewModel() {
        imagesRepository = FakeImageRepository()
        imagesRepository.addImages(canvas)

        canvasDetailViewModel = CanvasDetailViewModel(imagesRepository)
    }

    @Test
    fun getAddedCanvasFromRepositoryAndLoadIntoView() {
        canvasDetailViewModel.start(canvas.id)

        // Then verify that the view was notified
        assertThat(canvasDetailViewModel.canvas.getOrAwaitValue()?.file).isEqualTo(canvas.file)
        assertThat(canvasDetailViewModel.canvas.getOrAwaitValue()?.source)
            .isEqualTo(canvas.source)
    }

    @Test
    fun addCanvas() {
        canvas.isEdited = true

        // Load the ViewModel
        canvasDetailViewModel.start(canvas.id)
        // Start observing to compute transformations
        canvasDetailViewModel.canvas.observeForTesting {

            // Verify that the canvas was edited initially
            assertThat(imagesRepository.imagesServiceData[canvas.id]?.isEdited).isTrue()

            // When the ViewModel is asked to edit the canvas
            canvasDetailViewModel.canvas.value!!.isEdited = false

            mainCoroutineRule.runBlockingTest {
                // Then the canvas is not edited and the snackbar shows the correct message
                val newCanvas = (imagesRepository.getImage(canvas.id) as Success).data
                assertTrue(newCanvas.isAdded)
            }
        }
    }

    @Test
    fun canvasDetailViewModel_repositoryError() {
        // Given a repository that returns errors
        imagesRepository.setReturnError(true)

        // Given an initialized ViewModel with an added canvas
        canvasDetailViewModel.start(canvas.id)
        // Get the computed LiveData value
        canvasDetailViewModel.canvas.observeForTesting {
            // Then verify that data is not available
            assertThat(canvasDetailViewModel.isDataAvailable.getOrAwaitValue()).isFalse()
        }
    }

    @Test
    fun updateSnackbar_nullValue() {
        // Before setting the Snackbar text, get its current value
        val snackbarText = canvasDetailViewModel.snackbarText.value

        // Check that the value is null
        assertThat(snackbarText).isNull()
    }

    @Test
    fun clickOnSaveCanvas_SetsEvent() {
        // When opening a new canvas
        canvasDetailViewModel.saveCanvas()

        // Then the event is triggered
        val value = canvasDetailViewModel.canvasSaveEvent.getOrAwaitValue()
        assertThat(value.getContentIfNotHandled()).isNotNull()
    }

    @Test
    fun deleteCanvas() {
        assertThat(imagesRepository.imagesServiceData.containsValue(canvas)).isTrue()
        canvasDetailViewModel.start(canvas.id)

        // When the deletion of a canvas is requested
        canvasDetailViewModel.deleteCanvas()

        assertThat(imagesRepository.imagesServiceData.containsValue(canvas)).isFalse()
    }

    @Test
    fun loadCanvas_loading() {
        // Pause dispatcher so we can verify initial values
        mainCoroutineRule.pauseDispatcher()

        // Load the canvas in the viewmodel
        canvasDetailViewModel.start(canvas.id)
        // Start observing to compute transformations
        canvasDetailViewModel.canvas.observeForTesting {
            // Force a refresh to show the loading indicator
            canvasDetailViewModel.refresh()

            // Then progress indicator is shown
            assertThat(canvasDetailViewModel.dataLoading.getOrAwaitValue()).isTrue()

            // Execute pending coroutines actions
            mainCoroutineRule.resumeDispatcher()

            // Then progress indicator is hidden
            assertThat(canvasDetailViewModel.dataLoading.getOrAwaitValue()).isFalse()
        }
    }
}
