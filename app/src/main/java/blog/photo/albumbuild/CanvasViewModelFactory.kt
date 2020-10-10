/*
 * Copyright (c) 2018-2020 SAP and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 * Contributors:
 * SAP - initial API and implementation
 */
package blog.photo.albumbuild

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import blog.photo.albumbuild.canvasdetail.CanvasDetailViewModel
import blog.photo.albumbuild.canvases.CanvasesViewModel
import blog.photo.albumbuild.data.source.ImagesRepository
import blog.photo.albumbuild.imagedetail.ImageDetailViewModel

/**
 * Factory for all ViewModels.
 */
@Suppress("UNCHECKED_CAST")
class CanvasViewModelFactory constructor(
    private val imagesRepository: ImagesRepository
) : ViewModelProvider.NewInstanceFactory() {

    override fun <T : ViewModel> create(modelClass: Class<T>) =
        with(modelClass) {
            when {
                isAssignableFrom(CanvasDetailViewModel::class.java) ->
                    CanvasDetailViewModel(imagesRepository)
                isAssignableFrom(CanvasesViewModel::class.java) ->
                    CanvasesViewModel(imagesRepository)
                isAssignableFrom(ImageDetailViewModel::class.java) ->
                    ImageDetailViewModel(imagesRepository)
                else ->
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        } as T
}
