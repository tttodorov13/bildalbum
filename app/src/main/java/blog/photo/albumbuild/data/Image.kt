/*
 * Copyright (c) 2018-2020 SAP and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 * Contributors:
 * SAP - initial API and implementation
 */
package blog.photo.albumbuild.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Immutable model class for an Image. In order to compile with Room, we can't use @JvmOverloads to
 * generate multiple constructors.
 *
 * @param file of image
 * @param source of image
 * @param isEdited image
 * @param isFavorite image
 * @param isCanvas image
 * @param id of image
 */
@Entity(tableName = "images")
data class Image @JvmOverloads constructor(
    @PrimaryKey @ColumnInfo(name = "entryId") var id: Int = 0,
    @ColumnInfo(name = "file") var file: String = "",
    @ColumnInfo(name = "source") var source: String = "",
    @ColumnInfo(name = "edited") var isEdited: Boolean = false,
    @ColumnInfo(name = "favorite") var isFavorite: Boolean = false,
    @ColumnInfo(name = "canvas") var isCanvas: Boolean = false
) {

    val isAdded
        get() = !isEdited

    val isEmpty
        get() = file.isEmpty() || source.isEmpty()

    val publicId
        get() = id.toString()
}
