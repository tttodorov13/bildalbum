/*
 * Copyright (c) 2018-2020 SAP and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 * Contributors:
 * SAP - initial API and implementation
 */
package blog.photo.buildalbum.launch

import androidx.lifecycle.*
import blog.photo.buildalbum.data.Image
import blog.photo.buildalbum.data.Result
import blog.photo.buildalbum.data.source.ImagesDataSource
import blog.photo.buildalbum.data.source.ImagesRepository
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * ViewModel for the launch screen.
 */
class LaunchViewModel(
    private val imagesRepository: ImagesRepository
) : ViewModel() {

    private val _forceUpdate = MutableLiveData(false)

    private val _dataLoading = MutableLiveData<Boolean>()
    val dataLoading: LiveData<Boolean> = _dataLoading

    private val _images: LiveData<List<Image>> = _forceUpdate.switchMap { forceUpdate ->
        if (forceUpdate) {
            _dataLoading.value = true
            viewModelScope.launch {
                _dataLoading.value = false
            }
        }
        imagesRepository.observeImages().switchMap { getImages(it) }
    }

    val images: LiveData<List<Image>> = _images

    private fun getImages(imagesResult: Result<List<Image>>): LiveData<List<Image>> {
        // TODO: This is a good case for liveData builder. Replace when stable.
        val result = MutableLiveData<List<Image>>()

        if (imagesResult is Result.Success) {
            viewModelScope.launch {
                result.value = imagesResult.data
            }
        } else {
            result.value = emptyList()
            Timber.d("Error while loading images")
        }

        return result
    }

    private val _canvases: LiveData<List<Image>> = _forceUpdate.switchMap { forceUpdate ->
        if (forceUpdate) {
            _dataLoading.value = true
            viewModelScope.launch {
                imagesRepository.refreshImages()
                _dataLoading.value = false
            }
        }
        imagesRepository.observeCanvases().switchMap { getCanvases(it) }
    }

    val canvases: LiveData<List<Image>> = _canvases

    private fun getCanvases(canvasesResult: Result<List<Image>>): LiveData<List<Image>> {
        // TODO: This is a good case for liveData builder. Replace when stable.
        val result = MutableLiveData<List<Image>>()

        if (canvasesResult is Result.Success) {
            viewModelScope.launch {
                result.value = canvasesResult.data
            }
        } else {
            result.value = emptyList()
            Timber.d("Error while loading canvases")
        }

        return result
    }

    init {
        loadSources(true)
    }

    // Check for image/canvas in db
    fun exist(image: Image): Boolean {
        images.value?.forEach {
            if (image.source == it.source)
                return true
        }

        canvases.value?.forEach {
            if (image.source == it.source)
                return true
        }

        return false
    }

    fun getLatestImageId(): Int? {
        return imagesRepository.getLatestImageId()
    }

    // Check size of canvases list
    fun hasCanvases(): Boolean {
        return canvases.value?.size!! > 0
    }

    // Check size of images list
    fun hasImages(): Boolean {
        return images.value?.isNotEmpty()!!
    }

    // Called on save image from Internet.
    fun saveImage(image: Image) {

        if (image.isEmpty) {
            Timber.d("Image cannot be empty")
            return
        }

        viewModelScope.launch {
            imagesRepository.saveImage(image)
        }
    }

    // Called on save canvas from Internet.
    fun saveCanvas(canvas: Image) {

        if (canvas.isEmpty) {
            Timber.d("Canvas cannot be empty")
            return
        }

        viewModelScope.launch {
            imagesRepository.saveImage(canvas)
        }
    }

    /**
     * @param forceUpdate Pass in true to refresh the data in the [ImagesDataSource]
     */
    private fun loadSources(forceUpdate: Boolean) {
        _forceUpdate.value = forceUpdate
    }
}
