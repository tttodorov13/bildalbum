/*
 * Copyright (c) 2018-2020 SAP and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 * Contributors:
 * SAP - initial API and implementation
 */
package blog.photo.albumbuild.addimage

import androidx.lifecycle.*
import blog.photo.albumbuild.Event
import blog.photo.albumbuild.R
import blog.photo.albumbuild.data.Image
import blog.photo.albumbuild.data.Result
import blog.photo.albumbuild.data.Result.Success
import blog.photo.albumbuild.data.source.ImagesRepository
import kotlinx.coroutines.launch

/**
 * ViewModel for the Add/Edit screen.
 */
class AddImageViewModel(
    private val imagesRepository: ImagesRepository
) : ViewModel() {

    private val _forceUpdate = MutableLiveData(false)

    fun exist(image: Image): Boolean {
        images.value?.forEach {
            if (image.source == it.source)
                return true
        }

        return false
    }

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

        if (imagesResult is Success) {
            viewModelScope.launch {
                result.value = imagesResult.data
            }
        } else {
            result.value = emptyList()
            showSnackbarMessage(R.string.loading_images_error)
        }

        return result
    }

    private fun showSnackbarMessage(message: Int) {
        _snackBarText.value = Event(message)
    }

    // Two-way databinding, exposing MutableLiveData
    var hasPermissionInternet = MutableLiveData<Boolean>()

    // Two-way databinding, exposing MutableLiveData
    val hasPermissionCamera = MutableLiveData<Boolean>()

    // Two-way databinding, exposing MutableLiveData
    val hasPermissionStorage = MutableLiveData<Boolean>()

    // Two-way databinding, exposing MutableLiveData
    val file = MutableLiveData<String>()

    // Two-way databinding, exposing MutableLiveData
    val source = MutableLiveData<String>()

    private val _dataLoading = MutableLiveData<Boolean>()
    val dataLoading: LiveData<Boolean> = _dataLoading

    private val _imageAddEvent = MutableLiveData<Event<Int>>()
    val imageAddEvent: LiveData<Event<Int>> = _imageAddEvent

    private val _imageDownloadCancelEvent = MutableLiveData<Event<Unit>>()
    val imageDownloadCancelEvent: LiveData<Event<Unit>> = _imageDownloadCancelEvent

    private val _imageDownloadEvent = MutableLiveData<Event<Int>>()
    val imageDownloadEvent: LiveData<Event<Int>> = _imageDownloadEvent

    private val _snackBarText = MutableLiveData<Event<Int>>()
    val snackBarText: LiveData<Event<Int>> = _snackBarText

    private var isDataLoaded = false

    init {
        hasPermissionCamera.value = false
        hasPermissionStorage.value = false
        hasPermissionInternet.value = false
        loadImages(true)
    }

    fun start() {
        if (_dataLoading.value == true) {
            return
        }

        if (isDataLoaded) {
            // No need to populate, already have data.
            return
        }

        _dataLoading.value = true
    }

    fun imageDownload(count: Int) = viewModelScope.launch {
        _imageDownloadEvent.value = Event(count)
    }

    fun imageDownloadCancel() = viewModelScope.launch {
        _imageDownloadCancelEvent.value = Event(Unit)
    }

    fun getLatestImageId(): Int? {
        return imagesRepository.getLatestImageId()
    }

    // Called on save from device.
    fun imageAdd() {
        val currentFile = file.value
        val currentSource = source.value

        if (currentFile == null || currentSource == null) {
            _snackBarText.value = Event(R.string.empty_image_message)
            return
        }

        if (Image(file = currentFile, source = currentSource).isEmpty) {
            _snackBarText.value = Event(R.string.empty_image_message)
            return
        }

        createNewAdd(Image(file = currentFile, source = currentSource))
    }

    // Called on add image from device.
    fun imageAdd(image: Image) {

        if (image.isEmpty) {
            _snackBarText.value = Event(R.string.empty_image_message)
            return
        }

        createNewAdd(image)
    }

    // Called on add image from Internet.
    fun imageDownload(image: Image) {

        if (image.isEmpty) {
            _snackBarText.value = Event(R.string.empty_image_message)
            return
        }

        createNewDownload(image)
    }

    private fun createNewAdd(newImage: Image) = viewModelScope.launch {
        imagesRepository.saveImage(newImage)

        _imageAddEvent.value = Event(1)
    }

    private fun createNewDownload(newImage: Image) = viewModelScope.launch {
        imagesRepository.saveImage(newImage)

        _imageDownloadEvent.value = Event(1)
    }

    /**
     * @param forceUpdate Pass in true to refresh the data in the [Image]'s list.
     */
    private fun loadImages(forceUpdate: Boolean) {
        _forceUpdate.value = forceUpdate
    }

    fun refresh() {
        _forceUpdate.value = true
    }

    fun setHasPermissionCamera(hasPermissionCamera: Boolean) {
        this.hasPermissionCamera.value = hasPermissionCamera
        refresh()
    }

    fun setHasPermissionStorage(hasPermissionStorage: Boolean) {
        this.hasPermissionStorage.value = hasPermissionStorage
        refresh()
    }

    fun setHasPermissionInternet(hasPermissionInternet: Boolean) {
        this.hasPermissionInternet.value = hasPermissionInternet
        refresh()
    }
}