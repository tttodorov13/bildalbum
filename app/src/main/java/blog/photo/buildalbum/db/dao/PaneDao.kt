package blog.photo.buildalbum.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import blog.photo.buildalbum.db.entity.PaneEntity

@Dao
interface PaneDao {

    @Query("SELECT * from panes ORDER BY name DESC")
    fun getAll(): LiveData<List<PaneEntity>>

    @Query("DELETE FROM panes WHERE name = :name")
    suspend fun delete(name: String)

    @Query("DELETE FROM panes")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pane: PaneEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(paneEntities: List<PaneEntity>)

    @Query("SELECT * FROM panes WHERE name = :name")
    abstract fun load(name: String): LiveData<PaneEntity>

    @Query("SELECT * FROM panes WHERE name = :name")
    abstract fun loadSync(name: String): PaneEntity
}