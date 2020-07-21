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

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.*
import blog.photo.buildalbum.data.Image
import blog.photo.buildalbum.data.Result
import blog.photo.buildalbum.data.Result.Error
import blog.photo.buildalbum.data.Result.Success
import kotlinx.coroutines.runBlocking
import java.util.*

/**
 * Implementation of a remote data source with static access to the data for easy testing.
 */
class FakeImageRepository : ImagesRepository {

    var imagesServiceData: LinkedHashMap<Int, Image> = LinkedHashMap()

    private var shouldReturnError = false

    private val observableImages = MutableLiveData<Result<List<Image>>>()

    fun setReturnError(value: Boolean) {
        shouldReturnError = value
    }

    override suspend fun refreshImages() {
        observableImages.value = getImages()
    }

    override suspend fun refreshImage(imageId: Int) {
        refreshImages()
    }

    override fun observeCanvases(): LiveData<Result<List<Image>>> {
        runBlocking { refreshImages() }

        return observableImages.switchMap { filterImages(it, true) }
    }

    override fun observeImages(): LiveData<Result<List<Image>>> {
        runBlocking { refreshImages() }

        return observableImages.switchMap { filterImages(it, false) }
    }

    private fun filterImages(
        imagesResult: Result<List<Image>>,
        isCanvas: Boolean
    ): LiveData<Result<List<Image>>> {
        // TODO: This is a good case for liveData builder. Replace when stable.
        val result = MutableLiveData<Result<List<Image>>>()

        if (imagesResult is Success)
            result.value = Success(filterImages(imagesResult.data, isCanvas))
        else
            result.value = Error(Exception("Test exception"))

        return result
    }

    private fun filterImages(images: List<Image>, isCanvas: Boolean): List<Image> {
        val imagesFiltered = ArrayList<Image>()

        // Filter the images based on the isEdited property
        images.forEach { image ->
            if (isCanvas == image.isCanvas)
                imagesFiltered.add(image)
        }

        return imagesFiltered
    }

    override suspend fun getCanvases(forceUpdate: Boolean): Result<List<Image>> {
        if (shouldReturnError)
            return Error(Exception("Test exception"))

        return Success(imagesServiceData.values.toList().filter { it.isCanvas })
    }

    override fun observeImage(imageId: Int): LiveData<Result<Image>> {
        runBlocking { refreshImages() }
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

    override suspend fun getImage(imageId: Int, forceUpdate: Boolean): Result<Image> {
        if (shouldReturnError)
            return Error(Exception("Test exception"))

        imagesServiceData[imageId]?.let {
            return Success(it)
        }

        return Error(Exception("Could not find image"))
    }

    override fun getLatestImageId(): Int? {
        if (imagesServiceData.size > 0) {
            //get all entries of the LinkedHashMap
            val entries: Set<Map.Entry<Int, Image>> = imagesServiceData.entries

            //get the iterator for entries
            val iterator: Iterator<Map.Entry<Int, Image>> = entries.iterator()

            var entry: Map.Entry<Int, Image>? = null
            var firstEntry: Map.Entry<Int?, Image?>? = null
            var lastEntry: Map.Entry<Int, Image>? = null

            //iterate through the entries
            while (iterator.hasNext()) {
                entry = iterator.next()

                //this will be null only first time
                if (firstEntry == null)
                    firstEntry = entry

                // this will have last entry after iteration is over
                lastEntry = entry
            }

            return lastEntry?.value?.id
        }

        return null
    }

    override suspend fun getImages(forceUpdate: Boolean): Result<List<Image>> {
        if (shouldReturnError)
            return Error(Exception("Test exception"))

        return Success(imagesServiceData.values.toList().filter { !it.isCanvas })
    }

    override suspend fun saveImage(image: Image) {
        imagesServiceData[image.id] = image
    }

    override suspend fun editImage(image: Image) {
        val editedItem = Image(
            id = image.id,
            file = image.file,
            source = image.source,
            isEdited = image.isEdited,
            isFavorite = image.isFavorite,
            isCanvas = image.isCanvas
        )

        imagesServiceData[image.id] = editedItem
    }

    override suspend fun editImage(imageId: Int) {
        // Not required for the remote data source.
        throw NotImplementedError()
    }

    override suspend fun addImage(image: Image) {
        val addedImage = Image(
            id = image.id,
            file = image.file,
            source = image.source,
            isEdited = image.isEdited,
            isFavorite = image.isFavorite,
            isCanvas = image.isCanvas
        )

        imagesServiceData[image.id] = addedImage
    }

    override suspend fun addImage(imageId: Int) {
        throw NotImplementedError()
    }

    override suspend fun deleteEditedImages() {
        imagesServiceData = imagesServiceData.filterValues {
            !it.isEdited
        } as LinkedHashMap<Int, Image>
    }

    override suspend fun deleteImage(imageId: Int) {
        imagesServiceData.remove(imageId)
        refreshImages()
    }

    override suspend fun deleteAllImages() {
        imagesServiceData.clear()
        refreshImages()
    }

    override suspend fun favoriteImage(image: Image) {
        val favoriteItem = Image(
            id = image.id,
            file = image.file,
            source = image.source,
            isEdited = image.isEdited,
            isFavorite = image.isFavorite,
            isCanvas = image.isCanvas
        )

        imagesServiceData[image.id] = favoriteItem
    }

    override suspend fun favoriteImage(imageId: Int) {
        // Not required for the remote data source.
    }

    override suspend fun clearFavorites() {
        imagesServiceData = imagesServiceData.filterValues {
            !it.isFavorite
        } as LinkedHashMap<Int, Image>
    }

    @VisibleForTesting
    fun addImages(vararg images: Image) {
        for (image in images)
            imagesServiceData[image.id] = image
        
        runBlocking { refreshImages() }
    }
}
