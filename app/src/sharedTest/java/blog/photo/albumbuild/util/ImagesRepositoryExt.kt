/*
 * Copyright (c) 2018-2020 SAP and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 * Contributors:
 * SAP - initial API and implementation
 */

package blog.photo.albumbuild.util

import blog.photo.albumbuild.data.Image
import blog.photo.albumbuild.data.source.ImagesRepository
import kotlinx.coroutines.runBlocking

/**
 * A blocking version of ImagesRepository.saveImage to minimize the number of times we have to
 * explicitly add <code>runBlocking { ... }</code> in our tests
 */
fun ImagesRepository.saveImageBlocking(image: Image) = runBlocking {
    this@saveImageBlocking.saveImage(image)
}

fun ImagesRepository.getCanvasesBlocking(forceUpdate: Boolean) = runBlocking {
    this@getCanvasesBlocking.getCanvases(forceUpdate)
}

fun ImagesRepository.getImagesBlocking(forceUpdate: Boolean) = runBlocking {
    this@getImagesBlocking.getImages(forceUpdate)
}

fun ImagesRepository.deleteAllImagesBlocking() = runBlocking {
    this@deleteAllImagesBlocking.deleteAllImages()
}