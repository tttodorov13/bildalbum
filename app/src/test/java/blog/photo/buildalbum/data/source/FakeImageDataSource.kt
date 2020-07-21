/*
 * Copyright (c) 2018-2020 SAP and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 * Contributors:
 * SAP - initial API and implementation
 */

package blog.photo.buildalbum.data.source

import androidx.lifecycle.LiveData
import blog.photo.buildalbum.data.Image
import blog.photo.buildalbum.data.Result
import blog.photo.buildalbum.data.Result.Error
import blog.photo.buildalbum.data.Result.Success

class FakeImageDataSource(var images: MutableList<Image>? = mutableListOf()) : ImagesDataSource {

    override suspend fun getImages(): Result<List<Image>> {
        images?.let { return Success(ArrayList(it)) }
        return Error(
            Exception("Images not found")
        )
    }

    override suspend fun getImage(imageId: Int): Result<Image> {
        images?.firstOrNull { it.id == imageId }?.let { return Success(it) }
        return Error(
            Exception("Image not found")
        )
    }

    override fun getLatestId(): Int? {
        return images?.firstOrNull()?.id
    }

    override suspend fun saveImage(image: Image) {
        images?.add(image)
    }

    override suspend fun addImage(image: Image) {
        images?.firstOrNull { it.id == image.id }?.let { it.isEdited = false }
    }

    override suspend fun addImage(imageId: Int) {
        images?.firstOrNull { it.id == imageId }?.let { it.isEdited = false }
    }

    override suspend fun editImage(image: Image) {
        images?.firstOrNull { it.id == image.id }?.let { it.isEdited = true }
    }

    override suspend fun editImage(imageId: Int) {
        images?.firstOrNull { it.id == imageId }?.let { it.isEdited = true }
    }

    override suspend fun deleteEditedImages() {
        images?.removeIf { it.isEdited }
    }

    override suspend fun deleteAllImages() {
        TODO("Not yet implemented")
    }

    override suspend fun deleteImage(imageId: Int) {
        images?.removeIf { it.id == imageId }
    }

    override suspend fun favoriteImage(image: Image) {
        images?.firstOrNull { it.id == image.id }?.let { it.isFavorite = true }
    }

    override suspend fun favoriteImage(imageId: Int) {
        images?.firstOrNull { it.id == imageId }?.let { it.isFavorite = true }
    }

    override suspend fun clearFavoriteCanvases() {
        TODO("Not yet implemented")
    }

    override suspend fun clearFavoriteImages() {
        images?.removeIf { it.isFavorite }
    }

    override fun observeCanvases(): LiveData<Result<List<Image>>> {
        TODO("Not yet implemented")
    }

    override fun observeImages(): LiveData<Result<List<Image>>> {
        TODO("not yet implemented")
    }

    override suspend fun getCanvases(): Result<List<Image>> {
        TODO("Not yet implemented")
    }

    override suspend fun refreshImages() {
        TODO("not implemented")
    }

    override fun observeImage(imageId: Int): LiveData<Result<Image>> {
        TODO("not implemented")
    }

    override suspend fun refreshImage(imageId: Int) {
        TODO("not implemented")
    }
}
