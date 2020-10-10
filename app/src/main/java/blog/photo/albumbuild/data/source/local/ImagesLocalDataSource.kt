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

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import blog.photo.albumbuild.data.Image
import blog.photo.albumbuild.data.Result
import blog.photo.albumbuild.data.Result.Error
import blog.photo.albumbuild.data.Result.Success
import blog.photo.albumbuild.data.source.ImagesDataSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Concrete implementation of a data source as a db.
 */
class ImagesLocalDataSource internal constructor(
    private val imagesDao: ImagesDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ImagesDataSource {

    override fun observeCanvases(): LiveData<Result<List<Image>>> {
        return imagesDao.observeCanvases().map {
            Success(it)
        }
    }

    override fun observeImages(): LiveData<Result<List<Image>>> {
        return imagesDao.observeImages().map {
            Success(it)
        }
    }

    override fun observeImage(imageId: Int): LiveData<Result<Image>> {
        return imagesDao.observeImageById(imageId).map {
            Success(it)
        }
    }

    override suspend fun refreshImage(imageId: Int) {
        // NO-OP
    }

    override suspend fun refreshImages() {
        // NO-OP
    }

    override suspend fun getCanvases(): Result<List<Image>> = withContext(ioDispatcher) {
        return@withContext try {
            Success(imagesDao.getCanvases())
        } catch (e: Exception) {
            Error(e)
        }
    }

    override suspend fun getImages(): Result<List<Image>> = withContext(ioDispatcher) {
        return@withContext try {
            Success(imagesDao.getImages())
        } catch (e: Exception) {
            Error(e)
        }
    }

    override suspend fun getImage(imageId: Int): Result<Image> = withContext(ioDispatcher) {
        try {
            val image = imagesDao.getImageById(imageId)
            if (image != null) {
                return@withContext Success(image)
            } else {
                return@withContext Error(Exception("Image not found!"))
            }
        } catch (e: Exception) {
            return@withContext Error(e)
        }
    }

    override fun getLatestId(): Int? {
        return imagesDao.getLatestImageId()
    }

    override suspend fun saveImage(image: Image) = withContext(ioDispatcher) {
        imagesDao.insertImage(image)
    }

    override suspend fun addImage(image: Image) = withContext(ioDispatcher) {
        imagesDao.updateEdited(image.id, false)
    }

    override suspend fun addImage(imageId: Int) {
        imagesDao.updateEdited(imageId, false)
    }

    override suspend fun editImage(image: Image) = withContext(ioDispatcher) {
        imagesDao.updateEdited(image.id, true)
    }

    override suspend fun editImage(imageId: Int) {
        imagesDao.updateEdited(imageId, true)
    }

    override suspend fun deleteEditedImages() = withContext<Unit>(ioDispatcher) {
        imagesDao.deleteEditedImages()
    }

    override suspend fun deleteAllImages() = withContext(ioDispatcher) {
        imagesDao.deleteAllImages()
    }

    override suspend fun deleteImage(imageId: Int) = withContext<Unit>(ioDispatcher) {
        imagesDao.deleteImageById(imageId)
    }

    override suspend fun favoriteImage(image: Image) = withContext(ioDispatcher) {
        imagesDao.updateFavorite(image.id, !image.isFavorite)
    }

    override suspend fun favoriteImage(imageId: Int) {
        imagesDao.updateFavorite(imageId, true)
    }

    override suspend fun clearFavoriteCanvases() = withContext<Unit>(ioDispatcher) {
        imagesDao.clearFavoriteCanvases()
    }

    override suspend fun clearFavoriteImages() = withContext<Unit>(ioDispatcher) {
        imagesDao.clearFavoriteImages()
    }
}
