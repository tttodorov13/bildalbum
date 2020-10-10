/*
 * Copyright (c) 2018-2020 SAP and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 * Contributors:
 * SAP - initial API and implementation
 */
package blog.photo.albumbuild.data.source

import androidx.lifecycle.LiveData
import blog.photo.albumbuild.data.Image
import blog.photo.albumbuild.data.Result

/**
 * Interface to the data layer.
 */
interface ImagesRepository {

    fun observeCanvases(): LiveData<Result<List<Image>>>

    fun observeImages(): LiveData<Result<List<Image>>>

    suspend fun getCanvases(forceUpdate: Boolean = false): Result<List<Image>>

    suspend fun getImages(forceUpdate: Boolean = false): Result<List<Image>>

    suspend fun refreshImages()

    fun observeImage(imageId: Int): LiveData<Result<Image>>

    suspend fun getImage(imageId: Int, forceUpdate: Boolean = false): Result<Image>

    fun getLatestImageId(): Int?

    suspend fun refreshImage(imageId: Int)

    suspend fun saveImage(image: Image)

    suspend fun addImage(image: Image)

    suspend fun addImage(imageId: Int)

    suspend fun editImage(image: Image)

    suspend fun editImage(imageId: Int)

    suspend fun deleteEditedImages()

    suspend fun deleteAllImages()

    suspend fun deleteImage(imageId: Int)

    suspend fun favoriteImage(image: Image)

    suspend fun favoriteImage(imageId: Int)

    suspend fun clearFavorites()
}
