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

/**
 * Function that does some trivial computation. Used to showcase unit tests.
 */
internal fun getImageAddedAndEditedStats(images: List<Image>?): ImageStatsResult {

    return if (images == null || images.isEmpty()) {
        ImageStatsResult(
            addedImages = 0,
            addedImagesPercent = 0f,
            editedImages = 0,
            editedImagesPercent = 0f
        )
    } else {
        val totalImages = images.size
        val numberOfAddedImages = images.count { it.isAdded }
        val numberOfEditedImages = totalImages - numberOfAddedImages
        ImageStatsResult(
            addedImages = numberOfAddedImages,
            addedImagesPercent = 100f * numberOfAddedImages / totalImages,
            editedImages = numberOfEditedImages,
            editedImagesPercent = 100f * (totalImages - numberOfAddedImages) / totalImages
        )
    }
}

data class ImageStatsResult(
    val addedImages: Int,
    val editedImages: Int,
    val addedImagesPercent: Float,
    val editedImagesPercent: Float
)