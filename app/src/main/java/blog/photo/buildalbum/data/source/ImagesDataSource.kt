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

import androidx.lifecycle.LiveData
import blog.photo.buildalbum.data.Image
import blog.photo.buildalbum.data.Result

/**
 * Main entry point for accessing data.
 */
interface ImagesDataSource {

    fun observeCanvases(): LiveData<Result<List<Image>>>

    fun observeImages(): LiveData<Result<List<Image>>>

    suspend fun getCanvases(): Result<List<Image>>

    suspend fun getImages(): Result<List<Image>>

    suspend fun refreshImages()

    fun observeImage(imageId: Int): LiveData<Result<Image>>

    suspend fun getImage(imageId: Int): Result<Image>

    fun getLatestId(): Int?

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

    suspend fun clearFavoriteCanvases()

    suspend fun clearFavoriteImages()
}
