package blog.photo.buildalbum.db.model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import blog.photo.buildalbum.db.AppRoomDatabase
import blog.photo.buildalbum.db.entity.PaneEntity
import blog.photo.buildalbum.db.repository.PaneRepository
import kotlinx.coroutines.launch

/**
 * Class extends AndroidViewModel and requires application as a parameter.
 */
class PaneViewModel(application: Application) : AndroidViewModel(application) {

    // The ViewModel maintains a reference to the repository to get data.
    private val repository: PaneRepository
    // LiveData gives us updated panes when they change.
    val all: LiveData<List<PaneEntity>>

    init {
        // Gets reference to PaneDao from AppRoomDatabase to construct
        // the correct PaneRepository.
        val paneDao = AppRoomDatabase.getDatabase(application, viewModelScope).paneDao()
        repository = PaneRepository(paneDao)
        all = repository.all
    }

    // The implementation of insert() is completely hidden from the UI.
    // We don't want insert to block the main thread, so we're launching a new
    // coroutine. ViewModels have a coroutine scope based on their lifecycle called
    // viewModelScope which we can use here.
    fun insert(paneEntity: PaneEntity) = viewModelScope.launch {
        repository.insert(paneEntity)
    }
}