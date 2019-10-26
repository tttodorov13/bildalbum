package blog.photo.buildalbum.db.repository

import androidx.lifecycle.LiveData
import blog.photo.buildalbum.db.dao.ImageDao
import blog.photo.buildalbum.db.entity.ImageEntity

/**
 * Declares the DAO as a private property in the constructor. Pass in the DAO
 * instead of the whole database, because you only need access to the DAO
 */
class ImageRepository(private val imageDao: ImageDao) {

    // Room executes all queries on a separate thread.
    // Observed LiveData will notify the observer when the data has changed.
    val all: LiveData<List<ImageEntity>> = imageDao.getAll()

    /**
     * The suspend modifier tells the compiler that this must be called from a
     * coroutine or another suspend function.
     */
    suspend fun insert(imageEntity: ImageEntity) {
        imageDao.insert(imageEntity)
    }
}