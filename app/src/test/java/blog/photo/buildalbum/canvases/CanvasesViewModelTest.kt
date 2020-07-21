/*
 * Copyright (c) 2018-2020 SAP and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 * Contributors:
 * SAP - initial API and implementation
 */

package blog.photo.buildalbum.canvases

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import blog.photo.buildalbum.*
import blog.photo.buildalbum.data.Image
import blog.photo.buildalbum.data.source.FakeImageRepository
import blog.photo.buildalbum.images.CANVAS_DELETE_RESULT_OK
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for the implementation of [CanvasesViewModel]
 */
@ExperimentalCoroutinesApi
class CanvasesViewModelTest {

    // Subject under test
    private lateinit var canvasesViewModel: CanvasesViewModel

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
        val canvas1 = Image(file = "file1", source = "Description1")
        val canvas2 = Image(file = "file2", source = "Description2", isEdited = true)
        val canvas3 = Image(file = "file3", source = "Description3", isEdited = true)
        imagesRepository.addImages(canvas1, canvas2, canvas3)

        canvasesViewModel = CanvasesViewModel(imagesRepository)
    }

    @Test
    fun loadAllCanvasesFromRepository_loadingTogglesAndDataLoaded() {
        // TODO: loadAllCanvasesFromRepository_loadingTogglesAndDataLoaded
        // Pause dispatcher so we can verify initial values
        mainCoroutineRule.pauseDispatcher()

        // Given an initialized CanvasesViewModel with initialized canvass
        // When loading of Canvases is requested
        // Trigger loading of canvass
        canvasesViewModel.loadCanvases(true)
        // Observe the items to keep LiveData emitting
        canvasesViewModel.canvases.observeForTesting {

            // Then progress indicator is shown
            assertThat(canvasesViewModel.dataLoading.getOrAwaitValue()).isTrue()

            // Execute pending coroutines actions
            mainCoroutineRule.resumeDispatcher()

            // Then progress indicator is hidden
            assertThat(canvasesViewModel.dataLoading.getOrAwaitValue()).isFalse()

            // And data correctly loaded
            assertThat(canvasesViewModel.canvases.getOrAwaitValue()).hasSize(0)
        }
    }

    @Test
    fun loadCanvases_error() {
        // Make the repository return errors
        imagesRepository.setReturnError(true)

        // Load canvass
        canvasesViewModel.loadCanvases(true)
        // Observe the items to keep LiveData emitting
        canvasesViewModel.canvases.observeForTesting {

            // Then progress indicator is hidden
            assertThat(canvasesViewModel.dataLoading.getOrAwaitValue()).isFalse()

            // And the list of items is empty
            assertThat(canvasesViewModel.canvases.getOrAwaitValue()).isEmpty()

            // And the snackbar updated
            assertSnackbarMessage(canvasesViewModel.snackBarText, R.string.loading_canvases_error)
        }
    }

    @Test
    fun clickOnOpenCanvas_setsEvent() {
        // When opening a new canvas
        val canvasId = 42
        canvasesViewModel.openCanvas(canvasId)

        // Then the event is triggered
        assertCanvasLiveDataEventTriggered(canvasesViewModel.canvasOpenEvent, canvasId)
    }

    @Test
    fun showEditResultMessages_deleteOk_snackbarUpdated() {
        // When the viewmodel receives a result from another destination
        canvasesViewModel.showResultMessage(CANVAS_DELETE_RESULT_OK)

        // The snackbar is updated
        assertSnackbarMessage(
            canvasesViewModel.snackBarText,
            R.string.successfully_canvas_deleted_message
        )
    }
}
