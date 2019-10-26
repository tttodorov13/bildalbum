package blog.photo.buildalbum.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import blog.photo.buildalbum.db.entity.ImageEntity

@Dao
interface ImageDao {

    @Query("SELECT * from images ORDER BY name DESC")
    fun getAll(): LiveData<List<ImageEntity>>

    @Query("DELETE FROM images WHERE name = :name")
    suspend fun delete(name: String)

    @Query("DELETE FROM images")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(image: ImageEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(imageEntities: List<ImageEntity>)

    @Query("SELECT * FROM images WHERE name = :name")
    abstract fun load(name: String): LiveData<ImageEntity>

    @Query("SELECT * FROM images WHERE name = :name")
    abstract fun loadSync(name: String): ImageEntity
}