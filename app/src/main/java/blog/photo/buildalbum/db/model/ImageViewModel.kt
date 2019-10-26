package blog.photo.buildalbum.db.model

import android.app.Application
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import blog.photo.buildalbum.db.AppRoomDatabase
import blog.photo.buildalbum.db.entity.ImageEntity
import blog.photo.buildalbum.db.repository.ImageRepository
import kotlinx.coroutines.launch
import java.io.File

/**
 * Class extends AndroidViewModel and requires application as a parameter.
 */
class ImageViewModel(application: Application) : AndroidViewModel(application) {

    // The ViewModel maintains a reference to the repository to get data.
    private val repository: ImageRepository
    // LiveData gives us updated images when they change.
    val all: LiveData<List<ImageEntity>>

    val allPaths = arrayListOf<String>()

    init {
        // Gets reference to ImageDao from AppRoomDatabase to construct
        // the correct ImageRepository.
        val imageDao = AppRoomDatabase.getDatabase(application, viewModelScope).imageDao()
        repository = ImageRepository(imageDao)
        all = repository.all
        all.observeForever() { images ->
            images.forEach {
                allPaths.add(getFilePath(application, it))
            }
        }
    }

    // The implementation of insert() is completely hidden from the UI.
    // We don't want insert to block the main thread, so we're launching a new
    // coroutine. ViewModels have a coroutine scope based on their lifecycle called
    // viewModelScope which we can use here.
    fun insert(imageEntity: ImageEntity) = viewModelScope.launch {
        repository.insert(imageEntity)
    }

    private fun getFilePath(application: Application, imageEntity: ImageEntity): String {
        return File(
            application.applicationContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            imageEntity.name
        ).canonicalPath
    }
}