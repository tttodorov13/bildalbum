package blog.photo.buildalbum.db.repository

import androidx.lifecycle.LiveData
import blog.photo.buildalbum.db.dao.ImageDao
import blog.photo.buildalbum.db.entity.Image

// Declares the DAO as a private property in the constructor. Pass in the DAO
// instead of the whole database, because you only need access to the DAO
class ImageRepository(private val imageDao: ImageDao) {

    // Room executes all queries on a separate thread.
    // Observed LiveData will notify the observer when the data has changed.
    val allImages: LiveData<List<Image>> = imageDao.getAlphabetizedImages()

    suspend fun delete(image: Image) {
        imageDao.delete(image)
    }

    suspend fun insert(image: Image) {
        imageDao.insert(image)
    }
}