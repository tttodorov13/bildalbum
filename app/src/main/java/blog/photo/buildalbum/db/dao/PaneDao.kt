package blog.photo.buildalbum.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import blog.photo.buildalbum.db.entity.Pane

@Dao
interface PaneDao {

    @Query("SELECT * from pane_table ORDER BY file DESC")
    fun getAlphabetizedPanes(): LiveData<List<Pane>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(pane: Pane)
}