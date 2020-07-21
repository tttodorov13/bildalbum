/*
 * Copyright (c) 2018-2020 SAP and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 * Contributors:
 * SAP - initial API and implementation
 */
package blog.photo.buildalbum.images

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.lifecycle.*
import blog.photo.buildalbum.Event
import blog.photo.buildalbum.R
import blog.photo.buildalbum.data.Image
import blog.photo.buildalbum.data.Result
import blog.photo.buildalbum.data.Result.Success
import blog.photo.buildalbum.data.source.ImagesDataSource
import blog.photo.buildalbum.data.source.ImagesRepository
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * ViewModel for the image list screen.
 */
class ImagesViewModel(
    private val imagesRepository: ImagesRepository
) : ViewModel() {

    private val _forceUpdate = MutableLiveData<Boolean>(false)

    private val _images: LiveData<List<Image>> = _forceUpdate.switchMap { forceUpdate ->
        if (forceUpdate) {
            _dataLoading.value = true
            viewModelScope.launch {
                imagesRepository.refreshImages()
                _dataLoading.value = false
            }
        }

        imagesRepository.observeImages().switchMap { filterImages(it) }
    }

    val images: LiveData<List<Image>> = _images

    private val _canvases: LiveData<List<Image>> =
        imagesRepository.observeCanvases().switchMap { getCanvases(it) }

    val canvases: LiveData<List<Image>> = _canvases

    private val _editedImages: LiveData<List<Image>> =
        imagesRepository.observeImages().switchMap { getEditedImages(it) }

    val editedImages: LiveData<List<Image>> = _editedImages

    private val _dataLoading = MutableLiveData<Boolean>()
    val dataLoading: LiveData<Boolean> = _dataLoading

    private val _currentFilteringLabel = MutableLiveData<Int>()
    val currentFilteringLabel: LiveData<Int> = _currentFilteringLabel

    private val _noImagesLabel = MutableLiveData<Int>()
    val noImagesLabel: LiveData<Int> = _noImagesLabel

    private val _noImagesIconRes = MutableLiveData<Int>()
    val noImagesIconRes: LiveData<Int> = _noImagesIconRes

    private val _imagesAddViewVisible = MutableLiveData<Boolean>()
    val imagesAddViewVisible: LiveData<Boolean> = _imagesAddViewVisible

    private val _snackbarText = MutableLiveData<Event<Int>>()
    val snackbarText: LiveData<Event<Int>> = _snackbarText

    private var currentFiltering = ImagesFilterType.ALL_IMAGES

    private val _openImageEvent = MutableLiveData<Event<Int>>()
    val openImageEvent: LiveData<Event<Int>> = _openImageEvent

    private val _newImageEvent = MutableLiveData<Event<Unit>>()
    val newImageEvent: LiveData<Event<Unit>> = _newImageEvent

    private var resultMessageShown: Boolean = false

    // This LiveData depends on another so we can use a transformation.
    val empty: LiveData<Boolean> = Transformations.map(_images) {
        it.isEmpty()
    }

    init {
        // Set initial state
        setFiltering(ImagesFilterType.ALL_IMAGES)
        loadImages(true)
    }

    // Check for image/canvas in db
    fun exist(canvas: Image): Boolean {
        canvases.value?.forEach {
            if (canvas.source == it.source)
                return true
        }

        return false
    }

    /**
     * Sets the current image filtering type.
     *
     * @param requestType Can be [ImagesFilterType.ALL_IMAGES],
     * [ImagesFilterType.EDITED_IMAGES],
     * [ImagesFilterType.ADDED_IMAGES] or
     * [ImagesFilterType.FAVORITE_IMAGES]
     */
    fun setFiltering(requestType: ImagesFilterType) {
        currentFiltering = requestType

        // Depending on the filter type, set the filtering label, icon drawables, etc.
        when (requestType) {
            ImagesFilterType.ALL_IMAGES -> {
                setFilter(
                    R.string.all_images, R.string.no_images_all,
                    R.drawable.ic_camera_alt_96dp, true
                )
            }
            ImagesFilterType.ADDED_IMAGES -> {
                setFilter(
                    R.string.added_images, R.string.no_images_added,
                    R.drawable.ic_add_96dp, false
                )
            }
            ImagesFilterType.EDITED_IMAGES -> {
                setFilter(
                    R.string.edited_images, R.string.no_images_edited,
                    R.drawable.ic_edit_96dp, false
                )
            }
            ImagesFilterType.FAVORITE_IMAGES -> {
                setFilter(
                    R.string.favorite_images, R.string.no_images_favorite,
                    R.drawable.ic_favorite_96dp, false
                )
            }
        }

        // Refresh list
        loadImages(false)
    }

    private fun setFilter(
        @StringRes filteringLabelString: Int,
        @StringRes noImageLabelString: Int,
        @DrawableRes noImageIconDrawable: Int,
        imagesAddVisible: Boolean
    ) {
        _currentFilteringLabel.value = filteringLabelString
        _noImagesLabel.value = noImageLabelString
        _noImagesIconRes.value = noImageIconDrawable
        _imagesAddViewVisible.value = imagesAddVisible
    }

    fun clearFavoriteImages() {
        viewModelScope.launch {
            imagesRepository.clearFavorites()
            showSnackbarMessage(R.string.favorite_images_cleared)
        }
    }

    fun deleteEditedImages() {
        viewModelScope.launch {
            imagesRepository.deleteEditedImages()
            showSnackbarMessage(R.string.edited_images_deleted)
        }
    }

    /**
     * Called by the Data Binding library and the FAB's click listener.
     */
    fun addNewImage() {
        _newImageEvent.value = Event(Unit)
    }

    fun isFavorite(imageId: Int): Boolean {
        images.value?.forEach {
            if (imageId == it.id && it.isFavorite)
                return true
        }
        return false
    }

    /**
     * Called by Data Binding.
     */
    fun openImage(imageId: Int) {
        _openImageEvent.value = Event(imageId)
    }

    fun showResultMessage(result: Int) {
        if (resultMessageShown) return
        when (result) {
            IMAGE_ADD_RESULT_OK -> showSnackbarMessage(R.string.successfully_image_added_message)
            IMAGE_EDIT_RESULT_OK -> showSnackbarMessage(R.string.successfully_image_edited_message)
            IMAGE_DELETE_RESULT_OK -> showSnackbarMessage(R.string.successfully_image_deleted_message)
            IMAGE_ADD_RESULT_CANCEL -> showSnackbarMessage(R.string.image_add_cancelled_message)
            IMAGE_EDIT_RESULT_CANCEL -> showSnackbarMessage(R.string.image_edit_cancelled_message)
            IMAGE_DOWNLOAD_RESULT_OK -> showSnackbarMessage(R.string.successfully_images_downloaded_message)
            IMAGE_DOWNLOAD_RESULT_CANCEL -> showSnackbarMessage(R.string.there_are_no_new_images)
        }

        resultMessageShown = true
    }

    private fun showSnackbarMessage(message: Int) {
        _snackbarText.value = Event(message)
    }

    private fun filterImages(imagesResult: Result<List<Image>>): LiveData<List<Image>> {
        // TODO: This is a good case for liveData builder. Replace when stable.
        val result = MutableLiveData<List<Image>>()

        if (imagesResult is Success) {
            viewModelScope.launch {
                result.value = filterImages(imagesResult.data, currentFiltering)
            }
        } else {
            result.value = emptyList()
            showSnackbarMessage(R.string.loading_images_error)
        }

        return result
    }

    private fun filterImages(images: List<Image>, filteringType: ImagesFilterType): List<Image> {
        val imagesToShow = ArrayList<Image>()
        // Filter the images based on the requestType
        images.forEach { image ->
            when (filteringType) {
                ImagesFilterType.ALL_IMAGES -> imagesToShow.add(image)
                ImagesFilterType.ADDED_IMAGES -> if (image.isAdded) {
                    imagesToShow.add(image)
                }
                ImagesFilterType.EDITED_IMAGES -> if (image.isEdited) {
                    imagesToShow.add(image)
                }
                ImagesFilterType.FAVORITE_IMAGES -> if (image.isFavorite) {
                    imagesToShow.add(image)
                }
            }
        }
        return imagesToShow
    }

    private fun getCanvases(canvasesResult: Result<List<Image>>): LiveData<List<Image>> {
        // TODO: This is a good case for liveData builder. Replace when stable.
        val result = MutableLiveData<List<Image>>()

        if (canvasesResult is Success) {
            viewModelScope.launch {
                result.value = canvasesResult.data
            }
        } else {
            result.value = emptyList()
            Timber.d("Error while loading canvases")
        }

        return result
    }

    private fun getEditedImages(imagesResult: Result<List<Image>>): LiveData<List<Image>> {
        // TODO: This is a good case for liveData builder. Replace when stable.
        val result = MutableLiveData<List<Image>>()

        if (imagesResult is Success) {
            viewModelScope.launch {
                result.value = getEditedImages(imagesResult.data)
            }
        } else {
            result.value = emptyList()
            showSnackbarMessage(R.string.loading_images_error)
        }

        return result
    }

    private fun getEditedImages(images: List<Image>): List<Image> {
        val editedImages = ArrayList<Image>()

        // Filter the images based on the isEdited property
        images.forEach { image ->
            if (image.isEdited)
                editedImages.add(image)
        }

        return editedImages
    }

    /**
     * @param forceUpdate Pass in true to refresh the data in the [ImagesDataSource]
     */
    fun loadImages(forceUpdate: Boolean) {
        _forceUpdate.value = forceUpdate
    }

    fun refresh() {
        _forceUpdate.value = true
    }
}
