/*
 * Copyright (c) 2018-2020 SAP and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 * Contributors:
 * SAP - initial API and implementation
 */

package blog.photo.albumbuild.data.source.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import blog.photo.albumbuild.MainCoroutineRule
import blog.photo.albumbuild.data.Image
import blog.photo.albumbuild.data.Result.Success
import blog.photo.albumbuild.data.source.ImagesDataSource
import blog.photo.albumbuild.data.succeeded
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.junit.After
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration test for the [ImagesDataSource].
 */
@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@MediumTest
class ImagesLocalDataSourceTest {

    private lateinit var localDataSource: ImagesLocalDataSource
    private lateinit var database: AlbumBuildDatabase

    // Set the main coroutines dispatcher for unit testing.
    @ExperimentalCoroutinesApi
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    // Executes each image synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setup() {
        // using an in-memory database for testing, since it doesn't survive killing the process
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AlbumBuildDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()

        localDataSource = ImagesLocalDataSource(database.imageDao(), Dispatchers.Main)
    }

    @After
    fun cleanUp() {
        database.close()
    }

    @Test
    fun saveImage_retrievesImage() = runBlockingTest {
        // GIVEN - a new image saved in the database
        val newImage = Image(file = "file", source = "source", isEdited = true)
        localDataSource.saveImage(newImage)

        // WHEN  - Image retrieved by ID
        val result = localDataSource.getImage(newImage.id)

        // THEN - Same image is returned
        assertThat(result.succeeded, `is`(true))
        result as Success
        assertThat(result.data.file, `is`("file"))
        assertThat(result.data.source, `is`("source"))
        assertThat(result.data.isEdited, `is`(true))
    }

    @Test
    fun editImage_retrievedImageIsEdited() = runBlockingTest {
        // Given a new image in the persistent repository
        val newImage = Image(file = "file")
        localDataSource.saveImage(newImage)

        // When edited in the persistent repository
        localDataSource.editImage(newImage)
        val result = localDataSource.getImage(newImage.id)

        // Then the image can be retrieved from the persistent repository and is edit
        assertThat(result.succeeded, `is`(true))
        result as Success
        assertThat(result.data.file, `is`(newImage.file))
        assertThat(result.data.isEdited, `is`(true))
    }

    @Test
    fun favoriteImage_retrievedImageIsFavorite() = runBlockingTest {
        // Given a new image in the persistent repository
        val newImage = Image(file = "file")
        localDataSource.saveImage(newImage)

        // When edited in the persistent repository
        localDataSource.favoriteImage(newImage)
        val result = localDataSource.getImage(newImage.id)

        // Then the image can be retrieved from the persistent repository and is edit
        assertThat(result.succeeded, `is`(true))
        result as Success
        assertThat(result.data.file, `is`(newImage.file))
        assertThat(result.data.isFavorite, `is`(true))
    }

    @Test
    fun addImage_retrievedImageIsAdded() = runBlockingTest {
        // Given a new edited image in the persistent repository
        val newImage = Image(file = "Some file", source = "Some source", isEdited = true)
        localDataSource.saveImage(newImage)

        localDataSource.addImage(newImage)

        // Then the image can be retrieved from the persistent repository and is added
        val result = localDataSource.getImage(newImage.id)

        assertThat(result.succeeded, `is`(true))
        result as Success

        assertThat(result.data.file, `is`("Some file"))
        assertThat(result.data.isEdited, `is`(false))
    }

    @Test
    fun clearEditedImage_imageNotRetrievable() = runBlockingTest {
        // Given 2 new edited images and 1 added image in the persistent repository
        val newImage1 = Image(file = "file1")
        val newImage2 = Image(file = "file2")
        val newImage3 = Image(file = "file3")
        localDataSource.saveImage(newImage1)
        localDataSource.editImage(newImage1)
        localDataSource.saveImage(newImage2)
        localDataSource.editImage(newImage2)
        localDataSource.saveImage(newImage3)
        // When edited images are cleared in the repository
        localDataSource.deleteEditedImages()

        // Then the edited images cannot be retrieved and the added one can
        assertThat(localDataSource.getImage(newImage1.id).succeeded, `is`(false))
        assertThat(localDataSource.getImage(newImage2.id).succeeded, `is`(false))

        val result3 = localDataSource.getImage(newImage3.id)

        assertThat(result3.succeeded, `is`(true))
        result3 as Success

        assertThat(result3.data, `is`(newImage3))
    }

    @Test
    fun clearFavoriteImage_imageNotRetrievable() = runBlockingTest {
        // Given 2 new edited images and 1 added image in the persistent repository
        val newImage1 = Image(file = "file1")
        val newImage2 = Image(file = "file2")
        val newImage3 = Image(file = "file3")
        localDataSource.saveImage(newImage1)
        localDataSource.favoriteImage(newImage1)
        localDataSource.saveImage(newImage2)
        localDataSource.favoriteImage(newImage2)
        localDataSource.saveImage(newImage3)
        // When edited images are cleared in the repository
        localDataSource.deleteEditedImages()

        // Then the edited images cannot be retrieved and the added one can
        assertThat(localDataSource.getImage(newImage1.id).succeeded, `is`(false))
        assertThat(localDataSource.getImage(newImage2.id).succeeded, `is`(false))

        val result3 = localDataSource.getImage(newImage3.id)

        assertThat(result3.succeeded, `is`(true))
        result3 as Success

        assertThat(result3.data, `is`(newImage3))
    }

    @Test
    fun deleteAllImages_emptyListOfRetrievedImage() = runBlockingTest {
        // Given a new image in the persistent repository and a mocked callback
        val newImage = Image(file = "file")

        localDataSource.saveImage(newImage)

        // When all images are deleted
        localDataSource.deleteAllImages()

        // Then the retrieved images is an empty list
        val result = localDataSource.getImages() as Success
        assertThat(result.data.isEmpty(), `is`(true))
    }

    @Test
    fun getImages_retrieveSavedImages() = runBlockingTest {
        // Given 2 new images in the persistent repository
        val newImage1 = Image(file = "file")
        val newImage2 = Image(file = "file")

        localDataSource.saveImage(newImage1)
        localDataSource.saveImage(newImage2)
        // Then the images can be retrieved from the persistent repository
        val results = localDataSource.getImages() as Success<List<Image>>
        val images = results.data
        assertThat(images.size, `is`(2))
    }
}
