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

import blog.photo.buildalbum.data.Image
import org.hamcrest.core.Is.`is`
import org.junit.Assert.assertThat
import org.junit.Test

/**
 * Unit tests for [getImageAddedAndEditedStats].
 */
class ImageStatisticsUtilsTest {

    @Test
    fun getAddedAndEditedStats_noEdited() {
        val images = listOf(
            Image(file = "file", source = "source", isEdited = false)
        )
        // When the list of images is computed with an added image
        val result = getImageAddedAndEditedStats(images)

        // Then the percentages are 100 and 0
        assertThat(result.addedImagesPercent, `is`(100f))
        assertThat(result.editedImagesPercent, `is`(0f))
    }

    @Test
    fun getAddedAndEditedStats_noAdded() {
        val images = listOf(
            Image(file = "file", source = "source", isEdited = true)
        )
        // When the list of images is computed with a edited image
        val result = getImageAddedAndEditedStats(images)

        // Then the percentages are 0 and 100
        assertThat(result.addedImagesPercent, `is`(0f))
        assertThat(result.editedImagesPercent, `is`(100f))
    }

    @Test
    fun getAddedAndEditedStats_both() {
        // Given 3 edited images and 2 added images
        val images = listOf(
            Image(file = "file", source = "source", isEdited = true),
            Image(file = "file", source = "source", isEdited = true),
            Image(file = "file", source = "source", isEdited = true),
            Image(file = "file", source = "source", isEdited = false),
            Image(file = "file", source = "source", isEdited = false)
        )
        // When the list of images is computed
        val result = getImageAddedAndEditedStats(images)

        // Then the result is 40-60
        assertThat(result.addedImagesPercent, `is`(40f))
        assertThat(result.editedImagesPercent, `is`(60f))
    }

    @Test
    fun getAddedAndEditedStats_error() {
        // When there's an error loading stats
        val result = getImageAddedAndEditedStats(null)

        // Both added and edited images are 0
        assertThat(result.addedImagesPercent, `is`(0f))
        assertThat(result.editedImagesPercent, `is`(0f))
    }

    @Test
    fun getAddedAndEditedStats_empty() {
        // When there are no images
        val result = getImageAddedAndEditedStats(emptyList())

        // Both added and edited images are 0
        assertThat(result.addedImagesPercent, `is`(0f))
        assertThat(result.editedImagesPercent, `is`(0f))
    }
}
