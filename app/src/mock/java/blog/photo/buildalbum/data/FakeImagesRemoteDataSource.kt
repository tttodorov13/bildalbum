package blog.photo.buildalbum.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import blog.photo.buildalbum.data.Result.Error
import blog.photo.buildalbum.data.Result.Success
import blog.photo.buildalbum.data.source.ImagesDataSource
import java.util.LinkedHashMap

/**
 * Implementation of a remote data source with static access to the data for easy testing.
 */
object FakeImagesRemoteDataSource : ImagesDataSource {

    private var imagesServiceData: LinkedHashMap<Int, Image> = LinkedHashMap()

    private val observableImages = MutableLiveData<Result<List<Image>>>()

    override suspend fun refreshImages() {
        observableImages.postValue(getImages())
    }

    override suspend fun refreshImage(imageId: Int) {
        refreshImages()
    }

    override fun observeImages(): LiveData<Result<List<Image>>> {
        return observableImages
    }

    override fun observeImage(imageId: Int): LiveData<Result<Image>> {
        return observableImages.map { images ->
            when (images) {
                is Result.Loading -> Result.Loading
                is Error -> Error(images.exception)
                is Success -> {
                    val image = images.data.firstOrNull() { it.id == imageId }
                        ?: return@map Error(Exception("Not found"))
                    Success(image)
                }
            }
        }
    }

    override suspend fun getImage(imageId: Int): Result<Image> {
        imagesServiceData[imageId]?.let {
            return Success(it)
        }
        return Error(Exception("Could not find image"))
    }

    override suspend fun getImages(): Result<List<Image>> {
        return Success(imagesServiceData.values.toList())
    }

    override suspend fun saveImage(image: Image) {
        imagesServiceData[image.id] = image
    }

    override suspend fun editImage(image: Image) {
        val editedItem = Image(file = image.file, source = image.source, isEdited = true, isCanvas = false, id = image.id)
        imagesServiceData[image.id] = editedItem
    }

    override suspend fun editImage(imageId: Int) {
        // Not required for the remote data source.
    }

    override suspend fun addImage(image: Image) {
        val addedImage = Image(file = image.file, source =  image.source, isEdited = false, isCanvas = false, id = image.id)
        imagesServiceData[image.id] = addedImage
    }

    override suspend fun addImage(imageId: Int) {
        // Not required for the remote data source.
    }

    suspend fun deleteEditedImages() {
        imagesServiceData = imagesServiceData.filterValues {
            !it.isEdited
        } as LinkedHashMap<Int, Image>
    }

    suspend fun clearFavoriteImages() {
        imagesServiceData = imagesServiceData.filterValues {
            !it.isFavorite
        } as LinkedHashMap<Int, Image>
    }

    override suspend fun deleteImage(imageId: Int) {
        imagesServiceData.remove(imageId)
        refreshImages()
    }

    override suspend fun deleteAllImages() {
        imagesServiceData.clear()
        refreshImages()
    }
}
