/*
 * Copyright (c) 2018-2020 SAP and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 * Contributors:
 * SAP - initial API and implementation
 */
package blog.photo.buildalbum

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import blog.photo.buildalbum.addimage.AddImageViewModel
import blog.photo.buildalbum.canvasdetail.CanvasDetailViewModel
import blog.photo.buildalbum.canvases.CanvasesViewModel
import blog.photo.buildalbum.data.source.ImagesRepository
import blog.photo.buildalbum.imagedetail.ImageDetailViewModel
import blog.photo.buildalbum.images.ImagesViewModel
import blog.photo.buildalbum.statistics.StatisticsViewModel

/**
 * Factory for all ViewModels.
 */
@Suppress("UNCHECKED_CAST")
class ImageViewModelFactory constructor(
    private val imagesRepository: ImagesRepository
) : ViewModelProvider.NewInstanceFactory() {

    override fun <T : ViewModel> create(modelClass: Class<T>) =
        with(modelClass) {
            when {
                isAssignableFrom(AddImageViewModel::class.java) ->
                    AddImageViewModel(imagesRepository)
                isAssignableFrom(CanvasDetailViewModel::class.java) ->
                    CanvasDetailViewModel(imagesRepository)
                isAssignableFrom(CanvasesViewModel::class.java) ->
                    CanvasesViewModel(imagesRepository)
                isAssignableFrom(StatisticsViewModel::class.java) ->
                    StatisticsViewModel(imagesRepository)
                isAssignableFrom(ImageDetailViewModel::class.java) ->
                    ImageDetailViewModel(imagesRepository)
                isAssignableFrom(ImagesViewModel::class.java) ->
                    ImagesViewModel(imagesRepository)
                else ->
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        } as T
}
