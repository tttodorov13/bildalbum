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
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import blog.photo.albumbuild.MainCoroutineRule
import blog.photo.albumbuild.data.Image
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@SmallTest
class ImagesDaoTest {

    private lateinit var database: AlbumBuildDatabase

    // Set the main coroutines dispatcher for unit testing.
    @ExperimentalCoroutinesApi
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    // Executes each image synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun initDb() {
        // using an in-memory database because the information stored here disappears when the
        // process is killed
        database = Room.inMemoryDatabaseBuilder(
            getApplicationContext(),
            AlbumBuildDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun closeDb() = database.close()

    @Test
    fun insertImageAndGetById() = runBlockingTest {
        // GIVEN - insert a image
        val image = Image(file = "file", source = "source")
        database.imageDao().insertImage(image)

        // WHEN - Get the image by id from the database
        val loaded = database.imageDao().getImageById(image.id)

        // THEN - The loaded data contains the expected values
        assertThat<Image>(loaded as Image, notNullValue())
        assertThat(loaded.id, `is`(image.id))
        assertThat(loaded.file, `is`(image.file))
        assertThat(loaded.source, `is`(image.source))
        assertThat(loaded.isEdited, `is`(image.isEdited))
    }

    @Test
    fun insertImageReplacesOnConflict() = runBlockingTest {
        // Given that a image is inserted
        val image = Image(file = "file", source = "source")
        database.imageDao().insertImage(image)

        // When a image with the same id is inserted
        val newImage = Image(
            id = image.id,
            file = "file2",
            source = "source2",
            isEdited = true,
            isCanvas = false
        )
        database.imageDao().insertImage(newImage)

        // THEN - The loaded data contains the expected values
        val loaded = database.imageDao().getImageById(image.id)
        assertThat(loaded?.id, `is`(image.id))
        assertThat(loaded?.file, `is`("file2"))
        assertThat(loaded?.source, `is`("source2"))
        assertThat(loaded?.isEdited, `is`(true))
    }

    @Test
    fun insertImageAndGetImages() = runBlockingTest {
        // GIVEN - insert a image
        val image = Image(file = "file", source = "source")
        database.imageDao().insertImage(image)

        // WHEN - Get images from the database
        val images = database.imageDao().getImages()

        // THEN - There is only 1 image in the database, and contains the expected values
        assertThat(images.size, `is`(1))
        assertThat(images[0].id, `is`(image.id))
        assertThat(images[0].file, `is`(image.file))
        assertThat(images[0].source, `is`(image.source))
        assertThat(images[0].isEdited, `is`(image.isEdited))
    }

    @Test
    fun updateImageAndGetById() = runBlockingTest {
        // When inserting a image
        val originalImage = Image(file = "file", source = "source")
        database.imageDao().insertImage(originalImage)

        // When the image is updated
        val updatedImage = Image(
            id = originalImage.id,
            file = "new file",
            source = "new source",
            isEdited = true,
            isCanvas = false
        )
        database.imageDao().updateImage(updatedImage)

        // THEN - The loaded data contains the expected values
        val loaded = database.imageDao().getImageById(originalImage.id)
        assertThat(loaded?.id, `is`(originalImage.id))
        assertThat(loaded?.file, `is`("new file"))
        assertThat(loaded?.source, `is`("new source"))
        assertThat(loaded?.isEdited, `is`(true))
    }

    @Test
    fun updateEditedAndGetById() = runBlockingTest {
        // When inserting an image
        val image = Image(file = "file", source = "source", isEdited = true)
        database.imageDao().insertImage(image)

        // When the image is updated
        database.imageDao().updateEdited(image.id, false)

        // THEN - The loaded data contains the expected values
        val loaded = database.imageDao().getImageById(image.id)
        assertThat(loaded?.id, `is`(image.id))
        assertThat(loaded?.file, `is`(image.file))
        assertThat(loaded?.source, `is`(image.source))
        assertThat(loaded?.isEdited, `is`(false))
    }

    @Test
    fun updateFavoriteAndGetById() = runBlockingTest {
        // When inserting an image
        val image = Image(file = "file", source = "source", isFavorite = true)
        database.imageDao().insertImage(image)

        // When the image is updated as not favorite
        database.imageDao().updateFavorite(image.id, false)

        // THEN - The loaded data contains the expected values
        val loaded = database.imageDao().getImageById(image.id)
        assertThat(loaded?.id, `is`(image.id))
        assertThat(loaded?.file, `is`(image.file))
        assertThat(loaded?.source, `is`(image.source))
        assertThat(loaded?.isFavorite, `is`(false))
    }

    @Test
    fun deleteImageByIdAndGettingImages() = runBlockingTest {
        // Given a image inserted
        val image = Image(file = "file", source = "source")
        database.imageDao().insertImage(image)

        // When deleting a image by id
        database.imageDao().deleteImageById(image.id)

        // THEN - The list is empty
        val images = database.imageDao().getImages()
        assertThat(images.isEmpty(), `is`(true))
    }

    @Test
    fun deleteImagesAndGettingImages() = runBlockingTest {
        // Given a image inserted
        database.imageDao().insertImage(Image(file = "file", source = "source"))

        // When deleting all images
        database.imageDao().deleteAllImages()

        // THEN - The list is empty
        val images = database.imageDao().getImages()
        assertThat(images.isEmpty(), `is`(true))
    }

    @Test
    fun deleteEditedImagesAndGettingImages() = runBlockingTest {
        // Given a edited image inserted
        database.imageDao().insertImage(Image(file = "file", source = "source", isEdited = true))

        // When deleting edited images
        database.imageDao().deleteEditedImages()

        // THEN - The list is empty
        val images = database.imageDao().getImages()
        assertThat(images.isEmpty(), `is`(true))
    }

    @Test
    fun clearFavoriteCanvasesAndGettingCanvases() = runBlockingTest {
        // Given favorite canvas and image inserted
        val canvas = Image(
            file = "file",
            source = "source",
            isCanvas = true,
            isFavorite = true
        )
        database.imageDao().insertImage(
            canvas
        )

        // When deleting favorite images
        database.imageDao().clearFavoriteCanvases()

        // THEN - The list is empty
        val images = database.imageDao().getImages()
        assertThat(images.isEmpty(), `is`(false))
        assertThat(images.size, `is`(1))
    }

    @Test
    fun clearFavoriteImagesAndGettingImages() = runBlockingTest {
        // Given a favorite image inserted
        database.imageDao().insertImage(Image(file = "file", source = "source", isFavorite = true))

        // When deleting favorite images
        database.imageDao().clearFavoriteImages()

        // THEN - The list is empty
        val images = database.imageDao().getImages()
        assertThat(images.size, `is`(1))
    }
}
