package blog.photo.buildalbum.tasks

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.util.Log.e
import blog.photo.buildalbum.ImageActivity
import blog.photo.buildalbum.R.string.app_name
import blog.photo.buildalbum.model.Image

class SaveImage(private val context: Context, private val image: Image) :
    AsyncTask<String, Void, Bitmap>() {

    private val tag = "SaveImage"

    override fun doInBackground(vararg args: String): Bitmap {
        // Image is downloaded from Internet
        if (image.origin.isNotBlank())
            try {
                return BitmapFactory.decodeStream(java.net.URL(image.origin).openStream())
            } catch (e: Exception) {
                e(tag, e.message.toString())
            }

        return when {
            // Image is taken with Camera
            BitmapFactory.decodeFile(image.file.canonicalPath) != null && image.origin != context.getString(
                app_name
            ) -> BitmapFactory.decodeFile(
                image.file.canonicalPath
            )

            // Image is taken from Gallery
            args.isNotEmpty() -> BitmapFactory.decodeFile(args[0])

            // Image is created
            else -> ImageActivity.getBitmapFromImageView()
        }
    }

    override fun onPostExecute(result: Bitmap) {
        image.write(result)
        image.save()
    }
}