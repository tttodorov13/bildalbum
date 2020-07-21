/*
 * Copyright (c) 2018-2020 SAP and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 * Contributors:
 * SAP - initial API and implementation
 */
package blog.photo.buildalbum.imagedetail

import androidx.annotation.StringRes
import androidx.lifecycle.*
import blog.photo.buildalbum.Event
import blog.photo.buildalbum.R
import blog.photo.buildalbum.data.Image
import blog.photo.buildalbum.data.Result
import blog.photo.buildalbum.data.Result.Success
import blog.photo.buildalbum.data.source.ImagesRepository
import kotlinx.coroutines.launch

/**
 * ViewModel for the image details screen with canvas list.
 */
class ImageDetailViewModel(
    private val imagesRepository: ImagesRepository
) : ViewModel() {

    private val _imageId = MutableLiveData<Int>()

    private val _image = _imageId.switchMap { imageId ->
        imagesRepository.observeImage(imageId).map { computeImageResult(it) }
    }

    val image: LiveData<Image?> = _image

    var isNewImage = MutableLiveData<Boolean>(false)

    private val _canvasId = MutableLiveData<Int>()

    val isDataAvailable: LiveData<Boolean> = _image.map { it != null }

    private val _forceUpdate = MutableLiveData<Boolean>(false)

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

    private val _dataLoading = MutableLiveData<Boolean>()
    val dataLoading: LiveData<Boolean> = _dataLoading

    // Two-way databinding, exposing MutableLiveData
    var hasPermissionInternet = MutableLiveData<Boolean>()

    private val _imageDeleteEvent = MutableLiveData<Event<Unit>>()
    val imageDeleteEvent: LiveData<Event<Unit>> = _imageDeleteEvent

    private val _imageFavoriteEvent = MutableLiveData<Event<Boolean>>()
    val imageFavoriteEvent: LiveData<Event<Boolean>> = _imageFavoriteEvent

    private val _imageEditEvent = MutableLiveData<Event<Unit>>()
    val imageEditEvent: LiveData<Event<Unit>> = _imageEditEvent

    private val _imageRotateEvent = MutableLiveData<Event<Unit>>()
    val imageRotateEvent: LiveData<Event<Unit>> = _imageRotateEvent

    private val _imageSaveEvent = MutableLiveData<Event<Unit>>()
    val imageSaveEvent: LiveData<Event<Unit>> = _imageSaveEvent

    private val _imagePrintEvent = MutableLiveData<Event<Unit>>()
    val imagePrintEvent: LiveData<Event<Unit>> = _imagePrintEvent

    private val _imageShareEvent = MutableLiveData<Event<Unit>>()
    val imageShareEvent: LiveData<Event<Unit>> = _imageShareEvent

    private val _applyCanvasEvent = MutableLiveData<Event<Int>>()
    val applyCanvasEvent: LiveData<Event<Int>> = _applyCanvasEvent

    private val _noCanvesLabel = MutableLiveData<Int>()
    val noCanvasesLabel: LiveData<Int> = _noCanvesLabel

    private val _snackBarText = MutableLiveData<Event<Int>>()
    val snackBarText: LiveData<Event<Int>> = _snackBarText

    // Not used at the moment
    private val isDataLoadingError = MutableLiveData<Boolean>()

    // This LiveData depends on another so we can use a transformation.
    val edited: LiveData<Boolean> = _image.map { input: Image? ->
        input?.isEdited ?: false
    }

    // This LiveData depends on another so we can use a transformation.
    val empty: LiveData<Boolean> = Transformations.map(_canvases) {
        it.isEmpty()
    }

    init {
        // Set canvases
        _noCanvesLabel.value = R.string.no_canvases_all
        hasPermissionInternet.value = false
        loadCanvases(true)
    }

    // Check for canvas in db
    fun exist(canvas: Image): Boolean {
        canvases.value?.forEach {
            if (canvas.source == it.source)
                return true
        }

        return false
    }

    private fun createNew(newImage: Image) = viewModelScope.launch {
        imagesRepository.saveImage(newImage)
    }

    fun deleteImage() = viewModelScope.launch {
        _imageId.value?.let {
            imagesRepository.deleteImage(it)
            _imageDeleteEvent.value = Event(Unit)
        }
    }

    fun getLatestImageId(): Int? {
        return imagesRepository.getLatestImageId()
    }

    fun favoriteImage() = viewModelScope.launch {
        _image.value?.let {
            if (image.value?.isFavorite!!) {
                imagesRepository.favoriteImage(it)
                _snackBarText.value = Event(R.string.successfully_image_unfavorited_message)
                _imageFavoriteEvent.value = Event(false)
                return@let
            }
            imagesRepository.favoriteImage(it)
            _snackBarText.value = Event(R.string.successfully_image_favorited_message)
            _imageFavoriteEvent.value = Event(true)
        }
    }

    fun editImage(image: Image) {
        if (image.isEmpty) {
            _snackBarText.value = Event(R.string.empty_image_message)
            return
        }

        if (isNewImage.value!!)
            viewModelScope.launch {
                imagesRepository.saveImage(image)
                _imageEditEvent.value = Event(Unit)
            }
    }

    fun rotateImage() {
        _imageId.value?.let {
            _imageRotateEvent.value = Event(Unit)
        }
    }

    fun saveImage() = viewModelScope.launch {
        _imageId.value?.let {
            _imageSaveEvent.value = Event(Unit)
        }
    }

    // Called on save image from Internet.
    fun saveImage(image: Image) {

        if (image.isEmpty) {
            _snackBarText.value = Event(R.string.empty_image_message)
            return
        }

        createNew(image)
    }

    fun printImage() {
        _imageId.value?.let {
            _snackBarText.value = Event(R.string.successfully_image_sent_for_printing_message)
            _imagePrintEvent.value = Event(Unit)
        }
    }

    fun shareImage() {
        _imageId.value?.let {
            _snackBarText.value = Event(R.string.successfully_image_sent_for_sharing_message)
            _imageShareEvent.value = Event(Unit)
        }
    }

    fun setCanvas(canvasId: Int) = viewModelScope.launch {
        _canvasId.value = canvasId
        _canvasId.value?.let {
            _applyCanvasEvent.value = Event(canvasId)
        }
    }

    fun start(imageId: Int?) {
        // If we're already loading or already loaded, return (might be a config change)
        if (_dataLoading.value == true || imageId == _imageId.value) {
            return
        }

        // Trigger the load
        _imageId.value = imageId

        if (imageId == null) {
            // No need to populate, it's a new image
            isNewImage.value = true
            return
        }
    }

    fun refresh() {
        // Refresh the repository and the image will be updated automatically.
        _image.value?.let {
            _dataLoading.value = true
            viewModelScope.launch {
                imagesRepository.refreshImage(it.id)
                _dataLoading.value = false
            }
        }

        _forceUpdate.value = true
    }

    private fun computeImageResult(imageResult: Result<Image>): Image? {
        return if (imageResult is Success) {
            imageResult.data
        } else {
            showSnackbarMessage(R.string.loading_image_error)
            null
        }
    }

    private fun computeCanvasResult(canvasResult: Result<Image>): Image? {
        return if (canvasResult is Success) {
            canvasResult.data
        } else {
            showSnackbarMessage(R.string.loading_canvases_error)
            null
        }
    }

    private fun showSnackbarMessage(@StringRes message: Int) {
        _snackBarText.value = Event(message)
    }

    private fun getCanvases(canvasesResult: Result<List<Image>>): LiveData<List<Image>> {
        // TODO: This is a good case for liveData builder. Replace when stable.
        val result = MutableLiveData<List<Image>>()

        if (canvasesResult is Success) {
            isDataLoadingError.value = false
            viewModelScope.launch {
                result.value = canvasesResult.data
            }
        } else {
            result.value = emptyList()
            showSnackbarMessage(R.string.loading_canvases_error)
            isDataLoadingError.value = true
        }

        return result
    }

    /**
     * @param forceUpdate Pass in true to refresh the data in the [ImagesDataSource]
     */
    private fun loadCanvases(forceUpdate: Boolean) {
        _forceUpdate.value = forceUpdate
    }

    fun setHasPermissionInternet(hasPermissionInternet: Boolean) {
        this.hasPermissionInternet.value = hasPermissionInternet
        refresh()
    }
}
