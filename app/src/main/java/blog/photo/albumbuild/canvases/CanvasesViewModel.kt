/*
 * Copyright (c) 2018-2020 SAP and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 * Contributors:
 * SAP - initial API and implementation
 */
package blog.photo.albumbuild.canvases

import androidx.annotation.DrawableRes
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
 * ViewModel for the canvases list screen.
 */
class CanvasesViewModel(
    private val imagesRepository: ImagesRepository
) : ViewModel() {

    private val _allowDownload = MutableLiveData<Boolean>()
    val allowDownload: LiveData<Boolean> = _allowDownload

    private val _forceUpdate = MutableLiveData(false)

    private val _canvases: LiveData<List<Image>> = _forceUpdate.switchMap { forceUpdate ->
        if (forceUpdate) {
            _dataLoading.value = true
            viewModelScope.launch {
                imagesRepository.refreshImages()
                _dataLoading.value = false
            }
        }
        imagesRepository.observeCanvases().switchMap { filterCanvases(it) }
    }

    val canvases: LiveData<List<Image>> = _canvases

    // Two-way databinding, exposing MutableLiveData
    var hasPermissionInternet = MutableLiveData<Boolean>()

    private val _dataLoading = MutableLiveData<Boolean>()
    val dataLoading: LiveData<Boolean> = _dataLoading

    private var currentFiltering = CanvasesFilterType.ALL_CANVASES

    private val _currentFilteringLabel = MutableLiveData<Int>()
    val currentFilteringLabel: LiveData<Int> = _currentFilteringLabel

    private val _noCanvasesIconRes = MutableLiveData<Int>()
    val noCanvasesIconRes: LiveData<Int> = _noCanvasesIconRes

    private val _noCanvasesLabel = MutableLiveData<Int>()
    val noCanvasesLabel: LiveData<Int> = _noCanvasesLabel

    private val _canvasesAddViewVisible = MutableLiveData<Boolean>()

    // TODO Write unit tests
    val canvasesAddViewVisible: LiveData<Boolean> = _canvasesAddViewVisible

    private val _canvasOpenEvent = MutableLiveData<Event<Int>>()
    val canvasOpenEvent: LiveData<Event<Int>> = _canvasOpenEvent

    private var resultMessageShown: Boolean = false

    private val _snackBarText = MutableLiveData<Event<Int>>()
    val snackBarText: LiveData<Event<Int>> = _snackBarText

    // This LiveData depends on another so we can use a transformation.
    val empty: LiveData<Boolean> = Transformations.map(_canvases) {
        it.isEmpty()
    }

    init {
        // Set initial state
        _allowDownload.value = false
        setFiltering(CanvasesFilterType.ALL_CANVASES)
        this.hasPermissionInternet.value = false
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

    fun getLatestImageId(): Int? {
        return imagesRepository.getLatestImageId()
    }

    // Called on save canvas from Internet.
    fun saveCanvas(image: Image) {

        if (image.isEmpty) {
            _snackBarText.value = Event(R.string.empty_canvas_message)
            return
        }

        createNew(image)
    }

    /**
     * Sets the current canvas filtering type.
     *
     * @param requestType Can be [CanvasesFilterType.ALL_CANVASES] or
     * [CanvasesFilterType.FAVORITE_CANVASES]
     */
    fun setFiltering(requestType: CanvasesFilterType) {
        currentFiltering = requestType

        // Depending on the filter type, set the filtering label, icon drawables, etc.
        when (requestType) {
            CanvasesFilterType.ALL_CANVASES -> {
                setFilter(
                    R.string.all_canvases, R.string.no_canvases_all,
                    R.drawable.ic_wallpaper_96dp, true
                )
            }
            CanvasesFilterType.FAVORITE_CANVASES -> {
                setFilter(
                    R.string.favorite_canvases, R.string.no_canvases_favorite,
                    R.drawable.ic_favorite_96dp, false
                )
            }
        }

        // Refresh list
        loadCanvases(false)
    }

    fun clearFavoriteCanvases() {
        viewModelScope.launch {
            imagesRepository.clearFavorites()
            showSnackbarMessage(R.string.favorite_canvases_cleared)
        }
    }

    private fun setFilter(
        @StringRes filteringLabelString: Int,
        @StringRes noCanvasLabelString: Int,
        @DrawableRes noCanvasIconDrawable: Int,
        canvasesAddVisible: Boolean
    ) {
        _currentFilteringLabel.value = filteringLabelString
        _noCanvasesLabel.value = noCanvasLabelString
        _noCanvasesIconRes.value = noCanvasIconDrawable
        _canvasesAddViewVisible.value = canvasesAddVisible
    }

    fun isFavorite(canvasId: Int): Boolean {
        canvases.value?.forEach {
            if (canvasId == it.id && it.isFavorite)
                return true
        }
        return false
    }

    /**
     * Called by Data Binding.
     */
    fun openCanvas(canvasId: Int) {
        _canvasOpenEvent.value = Event(canvasId)
    }

    fun showResultMessage(result: Int) {
        if (resultMessageShown) return
        when (result) {
            CANVAS_ADD_RESULT_OK -> showSnackbarMessage(R.string.successfully_canvases_downloaded_message)
            CANVAS_SAVE_RESULT_OK -> showSnackbarMessage(R.string.successfully_canvas_saved_message)
            CANVAS_DELETE_RESULT_OK -> showSnackbarMessage(R.string.successfully_canvas_deleted_message)
            CANVAS_DOWNLOAD_RESULT_CANCEL -> showSnackbarMessage(R.string.there_are_no_new_canvases)
        }
        resultMessageShown = true
    }

    private fun createNew(newImage: Image) = viewModelScope.launch {
        imagesRepository.saveImage(newImage)
    }

    fun setAllowDownload(allowDownload: Boolean) {
        _allowDownload.value = this.hasPermissionInternet.value!! && allowDownload
    }

    private fun showSnackbarMessage(message: Int) {
        _snackBarText.value = Event(message)
    }

    private fun filterCanvases(canvasesResult: Result<List<Image>>): LiveData<List<Image>> {
        // TODO: This is a good case for liveData builder. Replace when stable.
        val result = MutableLiveData<List<Image>>()

        if (canvasesResult is Success) {
            viewModelScope.launch {
                result.value = filterCanvases(canvasesResult.data, currentFiltering)
            }
        } else {
            result.value = emptyList()
            showSnackbarMessage(R.string.loading_canvases_error)
        }

        return result
    }

    private fun filterCanvases(
        canvases: List<Image>,
        filteringType: CanvasesFilterType
    ): List<Image> {
        val canvasesToShow = ArrayList<Image>()
        // We filter the images based on the requestType
        canvases.forEach { canvas ->
            when (filteringType) {
                CanvasesFilterType.ALL_CANVASES -> canvasesToShow.add(canvas)
                CanvasesFilterType.FAVORITE_CANVASES -> if (canvas.isFavorite) {
                    canvasesToShow.add(canvas)
                }
            }
        }
        return canvasesToShow
    }

    /**
     * @param forceUpdate Pass in true to refresh the data in the [ImagesDataSource]
     */
    fun loadCanvases(forceUpdate: Boolean) {
        _forceUpdate.value = forceUpdate
    }

    fun refresh() {
        _forceUpdate.value = true
    }

    fun setHasPermissionInternet(value: Boolean) {
        this.hasPermissionInternet.value = value
        refresh()
    }
}
