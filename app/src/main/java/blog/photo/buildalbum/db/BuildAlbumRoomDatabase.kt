package blog.photo.buildalbum.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import blog.photo.buildalbum.db.entity.Image
import blog.photo.buildalbum.db.dao.ImageDao
import blog.photo.buildalbum.db.dao.PaneDao
import blog.photo.buildalbum.db.entity.Pane
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Annotates class to be a Room Database with tables (entities) of the Image and Pane classes
 */
@Database(entities = [Image::class, Pane::class], version = 1)
abstract class BuildAlbumRoomDatabase : RoomDatabase() {

    abstract fun imageDao(): ImageDao
    abstract fun paneDao(): PaneDao

    companion object {
        // Singleton prevents multiple instances of database openings at the
        // same time.
        @Volatile
        private var INSTANCE: BuildAlbumRoomDatabase? = null

        fun getDatabase(
            context: Context,
            scope: CoroutineScope
        ): BuildAlbumRoomDatabase {
            val tempInstance =
                INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }
            synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BuildAlbumRoomDatabase::class.java,
                    "buildalbum_database"
                ).addCallback(
                    ImageDatabaseCallback(
                        scope
                    )
                ).build()
                INSTANCE = instance
                return instance
            }
        }
    }

    private class ImageDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {

        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            INSTANCE?.let { _ ->
                scope.launch {
                }
            }
        }
    }
}