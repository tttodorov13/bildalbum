package blog.photo.buildalbum.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import blog.photo.buildalbum.db.dao.ImageDao
import blog.photo.buildalbum.db.dao.PaneDao
import blog.photo.buildalbum.db.entity.ImageEntity
import blog.photo.buildalbum.db.entity.PaneEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Class annotated to be a Room Database with a table (entity) of the
 * ImageEntity and PaneEntity class
 */
@Database(entities = [ImageEntity::class, PaneEntity::class], version = 1)
public abstract class AppRoomDatabase : RoomDatabase() {

    abstract fun imageDao(): ImageDao
    abstract fun paneDao(): PaneDao

    private class AppDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {

        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            INSTANCE?.let { database ->
                scope.launch {
                    populateImages(database.imageDao())
                    populatePanes(database.paneDao())
                }
            }
        }

        suspend fun populateImages(imageDao: ImageDao) {
            // Add sample words.
            var image = ImageEntity("image1", "origin1")
            imageDao.insert(image)
            image = ImageEntity("image2", "origin2")
            imageDao.insert(image)
        }

        suspend fun populatePanes(paneDao: PaneDao) {
            // Add sample words.
            var pane = PaneEntity("pane1", "origin1")
            paneDao.insert(pane)
            pane = PaneEntity("pane2", "origin2")
            paneDao.insert(pane)
        }
    }

    // Singleton prevents multiple instances of database
    // opening at the same time.
    companion object {
        @Volatile
        private var INSTANCE: AppRoomDatabase? = null

        fun getDatabase(
            context: Context,
            scope: CoroutineScope
        ): AppRoomDatabase {
            // if the INSTANCE is not null, then return it,
            // if it is, then create the database
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppRoomDatabase::class.java,
                    "build_album.db"
                )
                    .addCallback(AppDatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                // return instance
                instance
            }
        }
    }
}