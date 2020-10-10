/*
 * Copyright (c) 2018-2020 SAP and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 * Contributors:
 * SAP - initial API and implementation
 */
package blog.photo.albumbuild.canvasdetail

import androidx.annotation.StringRes
import androidx.lifecycle.*
import blog.photo.albumbuild.Event
import blog.photo.albumbuild.R
import blog.photo.albumbuild.data.Image
import blog.photo.albumbuild.data.Result
import blog.photo.albumbuild.data.Result.Success
import blog.photo.albumbuild.data.source.ImagesRepository
import kotlinx.coroutines.launch

/**
 * ViewModel for the Details screen.
 */
class CanvasDetailViewModel(
    private val imagesRepository: ImagesRepository
) : ViewModel() {

    private val _canvasId = MutableLiveData<Int>()

    private val _canvas = _canvasId.switchMap { imageId ->
        imagesRepository.observeImage(imageId).map { computeResult(it) }
    }
    val canvas: LiveData<Image?> = _canvas

    val isDataAvailable: LiveData<Boolean> = _canvas.map { it != null }

    private val _dataLoading = MutableLiveData<Boolean>()
    val dataLoading: LiveData<Boolean> = _dataLoading

    private val _canvasDeleteEvent = MutableLiveData<Event<Unit>>()
    val canvasDeleteEvent: LiveData<Event<Unit>> = _canvasDeleteEvent

    private val _canvasFavoriteEvent = MutableLiveData<Event<Boolean>>()
    val canvasFavoriteEvent: LiveData<Event<Boolean>> = _canvasFavoriteEvent

    private val _canvasSaveEvent = MutableLiveData<Event<Unit>>()
    val canvasSaveEvent: LiveData<Event<Unit>> = _canvasSaveEvent

    private val _snackbarText = MutableLiveData<Event<Int>>()
    val snackbarText: LiveData<Event<Int>> = _snackbarText

    // This LiveData depends on another so we can use a transformation.
    val edited: LiveData<Boolean> = _canvas.map { input: Image? ->
        input?.isEdited ?: false
    }

    fun deleteCanvas() = viewModelScope.launch {
        _canvasId.value?.let {
            imagesRepository.deleteImage(it)
            _canvasDeleteEvent.value = Event(Unit)
        }
    }

    fun favoriteCanvas() = viewModelScope.launch {
        _canvas.value?.let {
            if (canvas.value?.isFavorite!!) {
                imagesRepository.favoriteImage(it)
                _snackbarText.value = Event(R.string.successfully_canvas_unfavorited_message)
                _canvasFavoriteEvent.value = Event(false)
                return@let
            }
            imagesRepository.favoriteImage(it)
            _snackbarText.value = Event(R.string.successfully_canvas_favorited_message)
            _canvasFavoriteEvent.value = Event(true)
        }
    }

    fun saveCanvas() {
        _canvasSaveEvent.value = Event(Unit)
    }

    fun start(canvasId: Int?) {
        // If we're already loading or already loaded, return (might be a config change)
        if (_dataLoading.value == true || canvasId == _canvasId.value) {
            return
        }
        // Trigger the load
        _canvasId.value = canvasId
    }

    private fun computeResult(canvasResult: Result<Image>): Image? {
        return if (canvasResult is Success) {
            canvasResult.data
        } else {
            showSnackbarMessage(R.string.loading_canvases_error)
            null
        }
    }

    fun refresh() {
        // Refresh the repository and the canvas will be updated automatically.
        _canvas.value?.let {
            _dataLoading.value = true
            viewModelScope.launch {
                imagesRepository.refreshImage(it.id)
                _dataLoading.value = false
            }
        }
    }

    private fun showSnackbarMessage(@StringRes message: Int) {
        _snackbarText.value = Event(message)
    }
}
