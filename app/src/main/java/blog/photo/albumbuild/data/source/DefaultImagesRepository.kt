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
import blog.photo.albumbuild.data.Result.Success
import blog.photo.albumbuild.util.wrapEspressoIdlingResource
import kotlinx.coroutines.*

/**
 * Default implementation of [ImagesRepository]. Single entry point for managing images' data.
 */
class DefaultImagesRepository(
    private val imagesRemoteDataSource: ImagesDataSource,
    private val imagesLocalDataSource: ImagesDataSource,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ImagesRepository {

    override suspend fun getCanvases(forceUpdate: Boolean): Result<List<Image>> {
        // Set app as busy while this function executes.
        wrapEspressoIdlingResource {

            if (forceUpdate) {
                try {
                    updateImagesFromRemoteDataSource()
                } catch (ex: Exception) {
                    return Result.Error(ex)
                }
            }
            return imagesLocalDataSource.getCanvases()
        }
    }

    override suspend fun getImages(forceUpdate: Boolean): Result<List<Image>> {
        // Set app as busy while this function executes.
        wrapEspressoIdlingResource {

            if (forceUpdate) {
                try {
                    updateImagesFromRemoteDataSource()
                } catch (ex: Exception) {
                    return Result.Error(ex)
                }
            }
            return imagesLocalDataSource.getImages()
        }
    }

    override suspend fun refreshImages() {
        updateImagesFromRemoteDataSource()
    }

    override fun observeCanvases(): LiveData<Result<List<Image>>> {
        return imagesLocalDataSource.observeCanvases()
    }

    override fun observeImages(): LiveData<Result<List<Image>>> {
        return imagesLocalDataSource.observeImages()
    }

    override suspend fun refreshImage(imageId: Int) {
        updateImageFromRemoteDataSource(imageId)
    }

    private suspend fun updateCanvasesFromRemoteDataSource() {
        val remoteImages = imagesRemoteDataSource.getCanvases()

        if (remoteImages is Success) {
            // Real apps might want to do a proper sync, deleting, modifying or adding each image.
            // TODO execute only while testing: imagesLocalDataSource.deleteAllImages()
            remoteImages.data.forEach { image ->
                imagesLocalDataSource.saveImage(image)
            }
        } else if (remoteImages is Result.Error) {
            throw remoteImages.exception
        }
    }

    private suspend fun updateImagesFromRemoteDataSource() {
        val remoteImages = imagesRemoteDataSource.getImages()

        if (remoteImages is Success) {
            // Real apps might want to do a proper sync, deleting, modifying or adding each image.
            // TODO execute only while testing: imagesLocalDataSource.deleteAllImages()
            remoteImages.data.forEach { image ->
                imagesLocalDataSource.saveImage(image)
            }
        } else if (remoteImages is Result.Error) {
            throw remoteImages.exception
        }
    }

    override fun observeImage(imageId: Int): LiveData<Result<Image>> {
        return imagesLocalDataSource.observeImage(imageId)
    }

    private suspend fun updateImageFromRemoteDataSource(imageId: Int) {
        val remoteImage = imagesRemoteDataSource.getImage(imageId)

        if (remoteImage is Success) {
            imagesLocalDataSource.saveImage(remoteImage.data)
        }
    }

    /**
     * Relies on [getImages] to fetch data and picks the image with the same ID.
     */
    override suspend fun getImage(imageId: Int, forceUpdate: Boolean): Result<Image> {
        // Set app as busy while this function executes.
        wrapEspressoIdlingResource {
            if (forceUpdate) {
                updateImageFromRemoteDataSource(imageId)
            }
            return imagesLocalDataSource.getImage(imageId)
        }
    }

    override fun getLatestImageId(): Int? {
        return imagesLocalDataSource.getLatestId()
    }

    override suspend fun saveImage(image: Image) {
        coroutineScope {
            launch { imagesRemoteDataSource.saveImage(image) }
            launch { imagesLocalDataSource.saveImage(image) }
        }
    }

    override suspend fun addImage(image: Image) = withContext<Unit>(ioDispatcher) {
        coroutineScope {
            launch { imagesRemoteDataSource.addImage(image) }
            launch { imagesLocalDataSource.addImage(image) }
        }
    }

    override suspend fun addImage(imageId: Int) {
        withContext(ioDispatcher) {
            (getImageWithId(imageId) as? Success)?.let { it ->
                addImage(it.data)
            }
        }
    }

    override suspend fun editImage(image: Image) {
        coroutineScope {
            launch { imagesRemoteDataSource.editImage(image) }
            launch { imagesLocalDataSource.editImage(image) }
        }
    }

    override suspend fun editImage(imageId: Int) {
        withContext(ioDispatcher) {
            (getImageWithId(imageId) as? Success)?.let { it ->
                editImage(it.data)
            }
        }
    }

    override suspend fun deleteEditedImages() {
        coroutineScope {
            launch { imagesRemoteDataSource.deleteEditedImages() }
            launch { imagesLocalDataSource.deleteEditedImages() }
        }
    }

    override suspend fun deleteAllImages() {
        withContext(ioDispatcher) {
            coroutineScope {
                launch { imagesRemoteDataSource.deleteAllImages() }
                launch { imagesLocalDataSource.deleteAllImages() }
            }
        }
    }

    override suspend fun deleteImage(imageId: Int) {
        coroutineScope {
            launch { imagesRemoteDataSource.deleteImage(imageId) }
            launch { imagesLocalDataSource.deleteImage(imageId) }
        }
    }

    override suspend fun favoriteImage(image: Image) {
        coroutineScope {
            launch { imagesRemoteDataSource.favoriteImage(image) }
            launch { imagesLocalDataSource.favoriteImage(image) }
        }
    }

    override suspend fun favoriteImage(imageId: Int) {
        withContext(ioDispatcher) {
            (getImageWithId(imageId) as? Success)?.let { it ->
                favoriteImage(it.data)
            }
        }
    }

    override suspend fun clearFavorites() {
        coroutineScope {
            launch { imagesRemoteDataSource.clearFavoriteImages() }
            launch { imagesLocalDataSource.clearFavoriteImages() }
        }
    }

    private suspend fun getImageWithId(id: Int): Result<Image> {
        return imagesLocalDataSource.getImage(id)
    }
}
