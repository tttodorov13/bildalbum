/*
 * Copyright (c) 2018-2020 SAP and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 * Contributors:
 * SAP - initial API and implementation
 */
package blog.photo.albumbuild.statistics

import androidx.lifecycle.*
import blog.photo.albumbuild.data.Image
import blog.photo.albumbuild.data.Result
import blog.photo.albumbuild.data.Result.Error
import blog.photo.albumbuild.data.Result.Success
import blog.photo.albumbuild.data.source.ImagesRepository
import kotlinx.coroutines.launch

/**
 * ViewModel for the statistics screen.
 */
class StatisticsViewModel(
    private val imagesRepository: ImagesRepository
) : ViewModel() {

    private val images: LiveData<Result<List<Image>>> = imagesRepository.observeImages()
    private val _dataLoading = MutableLiveData<Boolean>(false)
    private val imageStats: LiveData<ImageStatsResult?> = images.map {
        if (it is Success) {
            getImageAddedAndEditedStats(it.data)
        } else {
            null
        }
    }

    val addedImages = imageStats.map {
        it?.addedImages ?: 0
    }
    val addedImagesPercent = imageStats.map {
        it?.addedImagesPercent ?: 0f
    }
    val editedImages = imageStats.map {
        it?.editedImages ?: 0
    }
    val editedImagesPercent: LiveData<Float> = imageStats.map {
        it?.editedImagesPercent ?: 0f
    }
    val editedImagesPercentRound: LiveData<Int> = imageStats.map {
        it?.editedImagesPercent?.toInt() ?: 0
    }
    val dataLoading: LiveData<Boolean> = _dataLoading
    val error: LiveData<Boolean> = images.map { it is Error }
    val empty: LiveData<Boolean> = images.map { (it as? Success)?.data.isNullOrEmpty() }

    fun refresh() {
        _dataLoading.value = true
        viewModelScope.launch {
            imagesRepository.refreshImages()
            _dataLoading.value = false
        }
    }
}
