package blog.photo.buildalbum.db.repository

import androidx.lifecycle.LiveData
import blog.photo.buildalbum.db.dao.PaneDao
import blog.photo.buildalbum.db.entity.Image
import blog.photo.buildalbum.db.entity.Pane

// Declares the DAO as a private property in the constructor. Pass in the DAO
// instead of the whole database, because you only need access to the DAO
class PaneRepository(private val paneDao: PaneDao) {

    // Room executes all queries on a separate thread.
    // Observed LiveData will notify the observer when the data has changed.
    val allPanes: LiveData<List<Pane>> = paneDao.getAlphabetizedPanes()

    suspend fun insert(pane: Pane) {
        paneDao.insert(pane)
    }
}