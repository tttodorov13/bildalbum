package blog.photo.buildalbum.db.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import blog.photo.buildalbum.db.entity.Image

@Dao
interface ImageDao {

    @Query("SELECT * from image_table ORDER BY file DESC")
    fun getAlphabetizedImages(): LiveData<List<Image>>

    @Delete
    suspend fun delete(image: Image)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(image: Image)
}