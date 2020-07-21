package blog.photo.buildalbum

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.room.Room
import blog.photo.buildalbum.data.FakeImagesRemoteDataSource
import blog.photo.buildalbum.data.source.DefaultImagesRepository
import blog.photo.buildalbum.data.source.ImagesDataSource
import blog.photo.buildalbum.data.source.ImagesRepository
import blog.photo.buildalbum.data.source.local.AlbumBuildDatabase
import blog.photo.buildalbum.data.source.local.ImagesLocalDataSource
import kotlinx.coroutines.runBlocking

/**
 * A Service Locator for the [ImagesRepository].
 * This is the mock version, with [FakeImagesRemoteDataSource].
 */
object ServiceLocator {

    private val lock = Any()

    private var database: AlbumBuildDatabase? = null

    @Volatile
    var imagesRepository: ImagesRepository? = null
        @VisibleForTesting set

    fun provideImagesRepository(context: Context): ImagesRepository {
        synchronized(this) {
            return imagesRepository ?: imagesRepository ?: createImagesRepository(context)
        }
    }

    private fun createImagesRepository(context: Context): ImagesRepository {
        val newRepo = DefaultImagesRepository(FakeImagesRemoteDataSource, createImageLocalDataSource(context))
        imagesRepository = newRepo
        return newRepo
    }

    private fun createImageLocalDataSource(context: Context): ImagesDataSource {
        val database = database ?: createDataBase(context)
        return ImagesLocalDataSource(database.imageDao())
    }

    private fun createDataBase(context: Context): AlbumBuildDatabase {
        val result = Room.databaseBuilder(
            context.applicationContext,
            AlbumBuildDatabase::class.java, "AlbumBuild.db"
        ).build()
        database = result
        return result
    }

    @VisibleForTesting
    fun resetRepository() {
        synchronized(lock) {
            runBlocking {
                FakeImagesRemoteDataSource.deleteAllImages();
            }
            // Clear all data to avoid test pollution.
            database?.apply {
                clearAllTables()
                close()
            }
            database = null
            imagesRepository = null
        }
    }
}
