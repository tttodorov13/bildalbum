/*
 * Copyright (c) 2018-2020 SAP and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 * Contributors:
 * SAP - initial API and implementation
 */

package blog.photo.buildalbum.data.source.remote

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import blog.photo.buildalbum.data.Image
import blog.photo.buildalbum.data.Result
import blog.photo.buildalbum.data.Result.Error
import blog.photo.buildalbum.data.Result.Success
import blog.photo.buildalbum.data.source.ImagesDataSource
import kotlinx.coroutines.delay

/**
 * Implementation of the data source that adds a latency simulating network.
 */
object ImagesRemoteDataSource : ImagesDataSource {

    private const val SERVICE_LATENCY_IN_MILLIS = 2000L

    private var IMAGES_SERVICE_DATA = LinkedHashMap<Int, Image>(2)

    init {
        // TODO add default images on app install
//        addImage("file1", "source1")
//        addImage("file2", "source2")
    }

    private val observableImages = MutableLiveData<Result<List<Image>>>()

    private val observableCanvases = MutableLiveData<Result<List<Image>>>()

    override suspend fun refreshImages() {
        observableImages.value = getImages()
    }

    override suspend fun refreshImage(imageId: Int) {
        refreshImages()
    }

    override fun observeCanvases(): LiveData<Result<List<Image>>> {
        return observableCanvases
    }

    override fun observeImages(): LiveData<Result<List<Image>>> {
        return observableImages
    }

    override fun observeImage(imageId: Int): LiveData<Result<Image>> {
        return observableImages.map { images ->
            when (images) {
                is Result.Loading -> Result.Loading
                is Error -> Error(images.exception)
                is Success -> {
                    val image = images.data.firstOrNull() { it.id == imageId }
                        ?: return@map Error(Exception("Not found"))
                    Success(image)
                }
            }
        }
    }

    override suspend fun getCanvases(): Result<List<Image>> {
        // Simulate network by delaying the execution.
        val canvases = IMAGES_SERVICE_DATA.values.toList()
        delay(SERVICE_LATENCY_IN_MILLIS)
        return Success(canvases)
    }

    override suspend fun getImages(): Result<List<Image>> {
        // Simulate network by delaying the execution.
        val images = IMAGES_SERVICE_DATA.values.toList()
        delay(SERVICE_LATENCY_IN_MILLIS)
        return Success(images)
    }

    override suspend fun getImage(imageId: Int): Result<Image> {
        // Simulate network by delaying the execution.
        delay(SERVICE_LATENCY_IN_MILLIS)
        IMAGES_SERVICE_DATA[imageId]?.let {
            return Success(it)
        }
        return Error(Exception("Image not found"))
    }

    override fun getLatestId(): Int? {
        TODO("Not yet implemented")
    }

    private fun addImage(file: String, source: String) {
        val newImage = Image(file = file, source = source)
        IMAGES_SERVICE_DATA[newImage.id] = newImage
    }

    override suspend fun saveImage(image: Image) {
        IMAGES_SERVICE_DATA[image.id] = image
    }

    override suspend fun addImage(image: Image) {
        val addedImage = Image(
            id = image.id,
            file = image.file,
            source = image.source,
            isEdited = false,
            isCanvas = false
        )
        IMAGES_SERVICE_DATA[image.id] = addedImage
    }

    override suspend fun addImage(imageId: Int) {
        // Not required for the remote data source
    }

    override suspend fun editImage(image: Image) {
        val editedItem =
            Image(id = image.id, file = image.file, source = image.source, isEdited = true)
        IMAGES_SERVICE_DATA[image.id] = editedItem
    }

    override suspend fun editImage(imageId: Int) {
        // Not required for the remote data source
    }

    override suspend fun deleteEditedImages() {
        IMAGES_SERVICE_DATA = IMAGES_SERVICE_DATA.filterValues {
            !it.isEdited
        } as LinkedHashMap<Int, Image>
    }

    override suspend fun deleteAllImages() {
        IMAGES_SERVICE_DATA.clear()
    }

    override suspend fun deleteImage(imageId: Int) {
        IMAGES_SERVICE_DATA.remove(imageId)
    }

    override suspend fun favoriteImage(image: Image) {
        val favoriteItem =
            Image(id = image.id, file = image.file, source = image.source, isFavorite = true)
        IMAGES_SERVICE_DATA[image.id] = favoriteItem
    }

    override suspend fun favoriteImage(imageId: Int) {
        // Not required for the remote data source
    }

    override suspend fun clearFavoriteCanvases() {
        IMAGES_SERVICE_DATA = IMAGES_SERVICE_DATA.filterValues {
            !it.isFavorite && it.isCanvas
        } as LinkedHashMap<Int, Image>
    }

    override suspend fun clearFavoriteImages() {
        IMAGES_SERVICE_DATA = IMAGES_SERVICE_DATA.filterValues {
            !it.isFavorite && !it.isCanvas
        } as LinkedHashMap<Int, Image>
    }
}
