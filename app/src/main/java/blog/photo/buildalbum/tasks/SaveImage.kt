package blog.photo.buildalbum.tasks

import android.Manifest.permission.CAMERA
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.util.Log.e
import blog.photo.buildalbum.ImageActivity
import blog.photo.buildalbum.MainActivity
import blog.photo.buildalbum.model.Image

class SaveImage(private val isEdited: Boolean, private val image: Image) :
    AsyncTask<String, Void, Bitmap>() {

    private val tag = "SaveImage"

    override fun doInBackground(vararg args: String): Bitmap? {
        return when {
            // Image is taken from Gallery
            WRITE_EXTERNAL_STORAGE == image.origin -> BitmapFactory.decodeFile(args[0])

            // Image is taken with Camera
            CAMERA == image.origin -> MainActivity.getBitmapFromImageView()

            // Image is edited
            isEdited -> ImageActivity.getBitmapFromImageView()

            // Image download
            else -> try {
                BitmapFactory.decodeStream(java.net.URL(image.origin).openStream())
            } catch (e: Exception) {
                e(tag, e.message.toString())
                null
            }
        }
    }

    override fun onPostExecute(result: Bitmap) {
        image.write(result)
        image.save()
    }
}