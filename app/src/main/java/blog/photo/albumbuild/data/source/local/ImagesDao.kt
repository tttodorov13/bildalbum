/*
 * Copyright (c) 2018-2020 SAP and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 * Contributors:
 * SAP - initial API and implementation
 */
package blog.photo.albumbuild.data.source.local

import androidx.lifecycle.LiveData
import androidx.room.*
import blog.photo.albumbuild.data.Image

/**
 * Data Access Object for the images table.
 */
@Dao
interface ImagesDao {

    /**
     * Observes list of canvases.
     *
     * @return all canvases.
     */
    @Query("SELECT * FROM Images WHERE canvas = 1 ORDER BY entryId DESC")
    fun observeCanvases(): LiveData<List<Image>>

    /**
     * Observes list of images.
     *
     * @return all images.
     */
    @Query("SELECT * FROM Images WHERE canvas = 0 ORDER BY entryId DESC")
    fun observeImages(): LiveData<List<Image>>

    /**
     * Observes a single image.
     *
     * @param imageId the image id.
     * @return the image with imageId.
     */
    @Query("SELECT * FROM Images WHERE entryId = :imageId")
    fun observeImageById(imageId: Int): LiveData<Image>

    /**
     * Select all canvases from the images table.
     *
     * @return all canvases.
     */
    @Query("SELECT * FROM Images WHERE canvas = 1")
    suspend fun getCanvases(): List<Image>

    /**
     * Select all images from the images table.
     *
     * @return all images.
     */
    @Query("SELECT * FROM Images WHERE canvas = 0")
    suspend fun getImages(): List<Image>

    /**
     * Select an image by id.
     *
     * @param imageId the image id.
     * @return the image with imageId.
     */
    @Query("SELECT * FROM Images WHERE entryId = :imageId")
    suspend fun getImageById(imageId: Int): Image?

    /**
     * Select the latest id.
     *
     * @return the latest id.
     */
    @Query("SELECT entryId FROM Images ORDER BY entryId DESC LIMIT 1")
    fun getLatestImageId(): Int?

    /**
     * Insert an image in the database. If the image already exists, replace it.
     *
     * @param image the image to be inserted.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImage(image: Image)

    /**
     * Update an image.
     *
     * @param image image to be updated
     * @return the number of images updated. This should always be 1.
     */
    @Update
    suspend fun updateImage(image: Image): Int

    /**
     * Update the edit status of an image
     *
     * @param imageId id of the image
     * @param edited status to be updated
     */
    @Query("UPDATE Images SET edited = :edited WHERE entryId = :imageId")
    suspend fun updateEdited(imageId: Int, edited: Boolean)

    /**
     * Update the favorite status of an image
     *
     * @param imageId id of the image
     * @param favorite status to be updated
     */
    @Query("UPDATE Images SET favorite = :favorite WHERE entryId = :imageId")
    suspend fun updateFavorite(imageId: Int, favorite: Boolean)

    /**
     * Delete an image by id.
     *
     * @return the number of the images deleted. This should always be 1.
     */
    @Query("DELETE FROM Images WHERE entryId = :imageId")
    suspend fun deleteImageById(imageId: Int): Int

    /**
     * Delete all images.
     */
    @Query("DELETE FROM Images")
    suspend fun deleteAllImages()

    /**
     * Delete all edited images from the table.
     *
     * @return the number of images deleted.
     */
    @Query("DELETE FROM Images WHERE edited = 1 AND canvas = 0")
    suspend fun deleteEditedImages(): Int

    /**
     * Clear all favorite canvases from the images table.
     *
     * @return the number of images updated.
     */
    @Query("UPDATE Images SET favorite = 0 WHERE favorite = 1 AND canvas = 1")
    suspend fun clearFavoriteCanvases(): Int

    /**
     * Clear all favorite images from the images table.
     *
     * @return the number of images updated.
     */
    @Query("UPDATE Images SET favorite = 0 WHERE favorite = 1 AND canvas = 0")
    suspend fun clearFavoriteImages(): Int
}
