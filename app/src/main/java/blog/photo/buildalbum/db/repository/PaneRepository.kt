package blog.photo.buildalbum.db.repository

import androidx.lifecycle.LiveData
import blog.photo.buildalbum.db.dao.PaneDao
import blog.photo.buildalbum.db.entity.PaneEntity

/**
 * Declares the DAO as a private property in the constructor. Pass in the DAO
 * instead of the whole database, because you only need access to the DAO
 */
class PaneRepository(private val paneDao: PaneDao) {

    // Room executes all queries on a separate thread.
    // Observed LiveData will notify the observer when the data has changed.
    val all: LiveData<List<PaneEntity>> = paneDao.getAll()

    /**
     * The suspend modifier tells the compiler that this must be called from a
     * coroutine or another suspend function.
     */
    suspend fun insert(paneEntity: PaneEntity) {
        paneDao.insert(paneEntity)
    }
}