package blog.photo.buildalbum.db.model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import blog.photo.buildalbum.db.BuildAlbumRoomDatabase
import blog.photo.buildalbum.db.entity.Image
import blog.photo.buildalbum.db.repository.ImageRepository
import kotlinx.coroutines.launch

// Class extends AndroidViewModel and requires application as a parameter.
class ImageViewModel(application: Application) : AndroidViewModel(application) {

    // The ViewModel maintains a reference to the repository to get data.
    private val repository: ImageRepository
    // LiveData gives us updated images when they change.
    val allImages: LiveData<List<Image>>

    init {
        // Gets reference to ImageDao from ImageRoomDatabase to construct
        // the correct ImageRepository.
        val imageDao = BuildAlbumRoomDatabase.getDatabase(application, viewModelScope).imageDao()
        repository =
            ImageRepository(imageDao)
        allImages = repository.allImages
    }

    /**
     * The implementation of delete() in the database is completely hidden from the UI.
     * Room ensures that you're not doing any long running operations on
     * the main thread, blocking the UI, so we don't need to handle changing Dispatchers.
     * ViewModels have a coroutine scope based on their lifecycle called
     * viewModelScope which we can use here.
     */
    fun delete(image: Image) = viewModelScope.launch {
        repository.delete(image)
    }

    /**
     * The implementation of insert() in the database is completely hidden from the UI.
     * Room ensures that you're not doing any long running operations on
     * the main thread, blocking the UI, so we don't need to handle changing Dispatchers.
     * ViewModels have a coroutine scope based on their lifecycle called
     * viewModelScope which we can use here.
     */
    fun insert(image: Image) = viewModelScope.launch {
        repository.insert(image)
    }
}