/*
 * Copyright (c) 2018-2020 SAP and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 * Contributors:
 * SAP - initial API and implementation
 */

package blog.photo.buildalbum.data.source

import blog.photo.buildalbum.MainCoroutineRule
import blog.photo.buildalbum.data.Image
import blog.photo.buildalbum.data.Result
import blog.photo.buildalbum.data.Result.Success
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for the implementation of the in-memory repository with cache.
 */
@ExperimentalCoroutinesApi
class DefaultImagesRepositoryTest {

    private val image1 = Image(file = "Title1", source = "Description1")
    private val image2 = Image(file = "Title2", source = "Description2")
    private val image3 = Image(file = "Title3", source = "Description3")
    private val newImage = Image(file = "Title new", source = "Description new")
    private val remoteImages = listOf(image1, image2).sortedBy { it.id }
    private val localImages = listOf(image3).sortedBy { it.id }
    private val newImages = listOf(image3).sortedBy { it.id }
    private lateinit var imagesRemoteDataSource: FakeImageDataSource
    private lateinit var imagesLocalDataSource: FakeImageDataSource

    // Class under test
    private lateinit var imagesRepository: DefaultImagesRepository

    // Set the main coroutines dispatcher for unit testing.
    @ExperimentalCoroutinesApi
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @ExperimentalCoroutinesApi
    @Before
    fun createRepository() {
        imagesRemoteDataSource = FakeImageDataSource(remoteImages.toMutableList())
        imagesLocalDataSource = FakeImageDataSource(localImages.toMutableList())
        // Get a reference to the class under test
        imagesRepository = DefaultImagesRepository(
            imagesRemoteDataSource, imagesLocalDataSource, Dispatchers.Main
        )
    }

    @ExperimentalCoroutinesApi
    @Test
    fun getImages_emptyRepositoryAndUninitializedCache() = mainCoroutineRule.runBlockingTest {
        val emptySource = FakeImageDataSource()
        val imagesRepository = DefaultImagesRepository(
            emptySource, emptySource, Dispatchers.Main
        )

        assertThat(imagesRepository.getImages() is Success).isTrue()
    }

    @Test
    fun getImages_repositoryCachesAfterFirstApiCall() = mainCoroutineRule.runBlockingTest {
        // Trigger the repository to load data, which loads from remote and caches
        val initial = imagesRepository.getImages()

        imagesRemoteDataSource.images = newImages.toMutableList()

        val second = imagesRepository.getImages()

        // Initial and second should match because we didn't force a refresh
        assertThat(second).isEqualTo(initial)
    }

    @Test
    fun getImages_requestsAllImagesFromRemoteDataSource() = mainCoroutineRule.runBlockingTest {
        // When images are requested from the images repository
        val images = imagesRepository.getImages(true) as Success

        // Then images are loaded from the remote data source
        // TODO getImages_requestsAllImagesFromRemoteDataSource
//        assertThat(images.data).isEqualTo(remoteImages)
    }

    @Test
    fun saveImage_savesToLocalAndRemote() = mainCoroutineRule.runBlockingTest {
        // Make sure newImage is not in the remote or local datasources
        assertThat(imagesRemoteDataSource.images).doesNotContain(newImage)
        assertThat(imagesLocalDataSource.images).doesNotContain(newImage)

        // When a image is saved to the images repository
        imagesRepository.saveImage(newImage)

        // Then the remote and local sources are called
        assertThat(imagesRemoteDataSource.images).contains(newImage)
        assertThat(imagesLocalDataSource.images).contains(newImage)
    }

    @Test
    fun getImages_WithDirtyCache_imagesAreRetrievedFromRemote() =
        mainCoroutineRule.runBlockingTest {
            // First call returns from REMOTE
            val images = imagesRepository.getImages()

            // Set a different list of images in REMOTE
            imagesRemoteDataSource.images = newImages.toMutableList()

            // But if images are cached, subsequent calls load from cache
            val cachedImages = imagesRepository.getImages()
            assertThat(cachedImages).isEqualTo(images)

            // Now force remote loading
            val refreshedImages = imagesRepository.getImages(true) as Success

            // Images must be the recently updated in REMOTE
            // TODO getImages_WithDirtyCache_imagesAreRetrievedFromRemote
//        assertThat(refreshedImages.data).isEqualTo(newImages)
        }

    @Test
    fun getImages_WithDirtyCache_remoteUnavailable_error() = mainCoroutineRule.runBlockingTest {
        // Make remote data source unavailable
        imagesRemoteDataSource.images = null

        // Load images forcing remote load
        val refreshedImages = imagesRepository.getImages(true)

        // Result should be an error
        assertThat(refreshedImages).isInstanceOf(Result.Error::class.java)
    }

    @Test
    fun getImages_WithRemoteDataSourceUnavailable_imagesAreRetrievedFromLocal() =
        mainCoroutineRule.runBlockingTest {
            // When the remote data source is unavailable
            imagesRemoteDataSource.images = null

            // The repository fetches from the local source
            assertThat((imagesRepository.getImages() as Success).data).isEqualTo(localImages)
        }

    @Test
    fun getImages_WithBothDataSourcesUnavailable_returnsError() =
        mainCoroutineRule.runBlockingTest {
            // When both sources are unavailable
            imagesRemoteDataSource.images = null
            imagesLocalDataSource.images = null

            // The repository returns an error
            assertThat(imagesRepository.getImages()).isInstanceOf(Result.Error::class.java)
        }

    @Test
    fun getImages_refreshesLocalDataSource() = mainCoroutineRule.runBlockingTest {
        val initialLocal = imagesLocalDataSource.images

        // First load will fetch from remote
        val newImages = (imagesRepository.getImages(true) as Success).data

        // TODO getImages_refreshesLocalDataSource
//        assertThat(newImages).isEqualTo(remoteImages)
        assertThat(newImages).isEqualTo(imagesLocalDataSource.images)
        assertThat(imagesLocalDataSource.images).isEqualTo(initialLocal)
    }

    @Test
    fun editImage_editsImageToServiceAPIUpdatesCache() = mainCoroutineRule.runBlockingTest {
        // Save a image
        imagesRepository.saveImage(newImage)

        // Make sure it's added
        assertThat((imagesRepository.getImage(newImage.id) as Success).data.isEdited).isFalse()

        // Mark is as edit
        imagesRepository.editImage(newImage.id)

        // Verify it's now edited
        assertThat((imagesRepository.getImage(newImage.id) as Success).data.isEdited).isTrue()
    }

    @Test
    fun editImage_addedImageToServiceAPIUpdatesCache() = mainCoroutineRule.runBlockingTest {
        // Save a image
        imagesRepository.saveImage(newImage)
        imagesRepository.editImage(newImage.id)

        // Make sure it's edited
        assertThat((imagesRepository.getImage(newImage.id) as Success).data.isAdded).isFalse()

        // Mark is as added
        imagesRepository.addImage(newImage.id)

        // Verify it's now added
        val result = imagesRepository.getImage(newImage.id) as Success
        assertThat(result.data.isAdded).isTrue()
    }

    @Test
    fun getImage_repositoryCachesAfterFirstApiCall() = mainCoroutineRule.runBlockingTest {
        // Trigger the repository to load data, which loads from remote
        imagesRemoteDataSource.images = mutableListOf(image1)
        imagesRepository.getImage(image1.id, true)

        // Configure the remote data source to store a different image
        imagesRemoteDataSource.images = mutableListOf(image2)

        val image1SecondTime = imagesRepository.getImage(image1.id, true) as Success
        val image2SecondTime = imagesRepository.getImage(image2.id, true) as Success

        // Both work because one is in remote and the other in cache
        assertThat(image1SecondTime.data.id).isEqualTo(image1.id)
        assertThat(image2SecondTime.data.id).isEqualTo(image2.id)
    }

    @Test
    fun getImage_forceRefresh() = mainCoroutineRule.runBlockingTest {
        // Trigger the repository to load data, which loads from remote and caches
        imagesRemoteDataSource.images = mutableListOf(image1)
        imagesRepository.getImage(image1.id)

        // Configure the remote data source to return a different image
        imagesRemoteDataSource.images = mutableListOf(image2)

        // Force refresh
        val image1SecondTime = imagesRepository.getImage(image1.id, true)
        val image2SecondTime = imagesRepository.getImage(image2.id, true)

        // Only image2 works because the cache and local were invalidated
        assertThat((image1SecondTime as? Success)?.data?.id).isNull()
        assertThat((image2SecondTime as? Success)?.data?.id).isEqualTo(image2.id)
    }

    @Test
    fun deleteEditedImages() = mainCoroutineRule.runBlockingTest {
        // TODO Test deleteEditedImages
        val editedItem = image1.copy().apply { isEdited = true }
        imagesRemoteDataSource.images = mutableListOf(editedItem, image2)
        imagesRepository.deleteEditedImages()

        val images = (imagesRepository.getImages(true) as? Success)?.data

//        assertThat(images).hasSize(1)
        assertThat(images).contains(image2)
        assertThat(images).doesNotContain(editedItem)
    }

    @Test
    fun clearFavoriteImages() = mainCoroutineRule.runBlockingTest {
        val favoriteItem = image1.copy().apply { isFavorite = true }
        imagesRemoteDataSource.images = mutableListOf(favoriteItem, image2)
        imagesRepository.clearFavorites()

        val images = (imagesRepository.getImages(true) as? Success)?.data

        assertThat(images).hasSize(2)
        assertThat(images).contains(image2)
        assertThat(images).doesNotContain(favoriteItem)
    }

    @Test
    fun deleteAllImages() = mainCoroutineRule.runBlockingTest {
        val initialImages = (imagesRepository.getImages() as? Success)?.data

        // Delete all images
        imagesRepository.deleteAllImages()

        // Fetch data again
        val afterDeleteImages = (imagesRepository.getImages() as? Success)?.data

        // Verify images are empty now
        assertThat(initialImages).isNotEmpty()
        assertThat(afterDeleteImages).isEmpty()
    }

    @Test
    fun deleteSingleImage() = mainCoroutineRule.runBlockingTest {
        // TODO Test deleteSingleImage
        val initialImages = (imagesRepository.getImages(true) as? Success)?.data

        // Delete first image
        imagesRepository.deleteImage(image1.id)

        // Fetch data again
        val afterDeleteImages = (imagesRepository.getImages(true) as? Success)?.data

        // Verify only one image was deleted
//        assertThat(afterDeleteImages?.size).isEqualTo(initialImages!!.size - 1)
        assertThat(afterDeleteImages).doesNotContain(image1)
    }
}
