/*
 * Copyright (c) 2018-2020 SAP and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 * Contributors:
 * SAP - initial API and implementation
 */

package blog.photo.buildalbum.statistics

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import blog.photo.buildalbum.FakeFailingImagesRemoteDataSource
import blog.photo.buildalbum.MainCoroutineRule
import blog.photo.buildalbum.data.Image
import blog.photo.buildalbum.data.source.DefaultImagesRepository
import blog.photo.buildalbum.data.source.FakeImageRepository
import blog.photo.buildalbum.getOrAwaitValue
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for the implementation of [StatisticsViewModel]
 */
@ExperimentalCoroutinesApi
class ImageStatisticsViewModelTest {

    // Executes each image synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    // Subject under test
    private lateinit var imageStatisticsViewModel: StatisticsViewModel

    // Use a fake repository to be injected into the viewmodel
    private val imagesRepository = FakeImageRepository()

    // Set the main coroutines dispatcher for unit testing.
    @ExperimentalCoroutinesApi
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @Before
    fun setupStatisticsViewModel() {
        imageStatisticsViewModel = StatisticsViewModel(imagesRepository)
    }

    @Test
    fun loadEmptyImagesFromRepository_EmptyResults() = mainCoroutineRule.runBlockingTest {
        // Given an initialized StatisticsViewModel with no images

        // Then the results are empty
        assertThat(imageStatisticsViewModel.empty.getOrAwaitValue()).isTrue()
    }

    @Test
    fun loadNonEmptyImagesFromRepository_NonEmptyResults() {
        // We initialise the images to 3, with one added and two edited
        val image1 = Image(file = "0Title1", source = "Description1")
        val image2 = Image(file = "Title2", source = "Description2", isEdited = true)
        val image3 = Image(file = "Title3", source = "Description3", isEdited = true)
        val image4 = Image(file = "Title4", source = "Description4", isEdited = true)
        imagesRepository.addImages(image1, image2, image3, image4)

        // Then the results are not empty
        assertThat(imageStatisticsViewModel.empty.getOrAwaitValue())
            .isFalse()
        assertThat(imageStatisticsViewModel.addedImagesPercent.getOrAwaitValue())
            .isEqualTo(25f)
        assertThat(imageStatisticsViewModel.editedImagesPercent.getOrAwaitValue())
            .isEqualTo(75f)
    }

    @Test
    fun loadStatisticsWhenImagesAreUnavailable_CallErrorToDisplay() {
        val errorViewModel = StatisticsViewModel(
            DefaultImagesRepository(
                FakeFailingImagesRemoteDataSource,
                FakeFailingImagesRemoteDataSource,
                Dispatchers.Main // Main is set in MainCoroutineRule
            )
        )

        // Then an error message is shown
        assertThat(errorViewModel.empty.getOrAwaitValue()).isTrue()
        assertThat(errorViewModel.error.getOrAwaitValue()).isTrue()
    }

    @Test
    fun loadImages_loading() {
        // Pause dispatcher so we can verify initial values
        mainCoroutineRule.pauseDispatcher()

        // Load the image in the viewmodel
        imageStatisticsViewModel.refresh()

        // Then progress indicator is shown
        assertThat(imageStatisticsViewModel.dataLoading.getOrAwaitValue()).isTrue()

        // Execute pending coroutines actions
        mainCoroutineRule.resumeDispatcher()

        // Then progress indicator is hidden
        assertThat(imageStatisticsViewModel.dataLoading.getOrAwaitValue()).isFalse()
    }
}
