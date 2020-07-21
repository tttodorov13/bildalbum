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

import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import blog.photo.buildalbum.data.Image
import blog.photo.buildalbum.data.Result
import blog.photo.buildalbum.data.source.ImagesDataSource

object FakeFailingImagesRemoteDataSource : ImagesDataSource {

    override suspend fun getCanvases(): Result<List<Image>> {
        return Result.Error(Exception("Test"))
    }

    override suspend fun getImages(): Result<List<Image>> {
        return Result.Error(Exception("Test"))
    }

    override suspend fun getImage(imageId: Int): Result<Image> {
        return Result.Error(Exception("Test"))
    }

    override fun getLatestId(): Int? {
        TODO("Not yet implemented")
    }

    override fun observeCanvases(): LiveData<Result<List<Image>>> {
        return liveData { emit(getCanvases()) }
    }

    override fun observeImages(): LiveData<Result<List<Image>>> {
        return liveData { emit(getImages()) }
    }

    override suspend fun refreshImages() {
        TODO("not implemented")
    }

    override fun observeImage(imageId: Int): LiveData<Result<Image>> {
        return liveData { emit(getImage(imageId)) }
    }

    override suspend fun refreshImage(imageId: Int) {
        TODO("not implemented")
    }

    override suspend fun saveImage(image: Image) {
        TODO("not implemented")
    }

    override suspend fun editImage(image: Image) {
        TODO("not implemented")
    }

    override suspend fun editImage(imageId: Int) {
        TODO("not implemented")
    }

    override suspend fun favoriteImage(image: Image) {
        TODO("Not yet implemented")
    }

    override suspend fun favoriteImage(imageId: Int) {
        TODO("Not yet implemented")
    }

    override suspend fun addImage(image: Image) {
        TODO("not implemented")
    }

    override suspend fun addImage(imageId: Int) {
        TODO("not implemented")
    }

    override suspend fun deleteEditedImages() {
        TODO("not implemented")
    }

    override suspend fun clearFavoriteCanvases() {
        TODO("Not yet implemented")
    }

    override suspend fun clearFavoriteImages() {
        TODO("Not yet implemented")
    }

    override suspend fun deleteAllImages() {
        TODO("not implemented")
    }

    override suspend fun deleteImage(imageId: Int) {
        TODO("not implemented")
    }
}
