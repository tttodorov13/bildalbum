/*
 * Copyright (c) 2018-2020 SAP and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 * Contributors:
 * SAP - initial API and implementation
 */
package blog.photo.buildalbum.data.source.local

import androidx.room.Database
import androidx.room.RoomDatabase
import blog.photo.buildalbum.data.Image

/**
 * The Room Database that contains the Image table.
 *
 * Note that exportSchema should be true in production databases.
 */
@Database(entities = [Image::class], version = 1, exportSchema = false)
abstract class AlbumBuildDatabase : RoomDatabase() {

    abstract fun imageDao(): ImagesDao
}
