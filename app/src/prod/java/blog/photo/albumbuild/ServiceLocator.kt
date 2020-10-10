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

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.room.Room
import blog.photo.albumbuild.data.source.DefaultImagesRepository
import blog.photo.albumbuild.data.source.ImagesDataSource
import blog.photo.albumbuild.data.source.ImagesRepository
import blog.photo.albumbuild.data.source.local.AlbumBuildDatabase
import blog.photo.albumbuild.data.source.local.ImagesLocalDataSource
import blog.photo.albumbuild.data.source.remote.ImagesRemoteDataSource
import kotlinx.coroutines.runBlocking

/**
 * A Service Locator for the [ImagesRepository].
 * This is the prod version, with the "real" [ImagesRemoteDataSource].
 */
object ServiceLocator {

    private val lock = Any()

    private var database: AlbumBuildDatabase? = null

    @Volatile
    var imagesRepository: ImagesRepository? = null
        @VisibleForTesting set

    fun provideImagesRepository(context: Context): ImagesRepository {
        synchronized(this) {
            return imagesRepository ?: imagesRepository ?: createImagesRepository(context)
        }
    }

    private fun createImagesRepository(context: Context): ImagesRepository {
        val newRepo =
            DefaultImagesRepository(ImagesRemoteDataSource, createImageLocalDataSource(context))
        imagesRepository = newRepo
        return newRepo
    }

    private fun createImageLocalDataSource(context: Context): ImagesDataSource {
        val database = database ?: createDataBase(context)
        return ImagesLocalDataSource(database.imageDao())
    }

    private fun createDataBase(context: Context): AlbumBuildDatabase {
        val result = Room.databaseBuilder(
            context.applicationContext,
            AlbumBuildDatabase::class.java, "AlbumBuild.db"
        ).build()
        database = result
        return result
    }

    @VisibleForTesting
    fun resetRepository() {
        synchronized(lock) {
            runBlocking {
                ImagesRemoteDataSource.deleteAllImages()
            }
            // Clear all data to avoid test pollution.
            database?.apply {
                clearAllTables()
                close()
            }
            database = null
            imagesRepository = null
        }
    }
}
