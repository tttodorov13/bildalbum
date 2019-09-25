package blog.photo.buildalbum

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import blog.photo.buildalbum.adapters.ImagesAdapter
import blog.photo.buildalbum.model.Image
import blog.photo.buildalbum.utils.DatabaseHelper

/**
 * Interface for async responses
 */
interface AsyncResponse {
    fun taskCompleted(stringId: Int)
}

/**
 * Class base for all activities of the application.
 */
open class BaseActivity : AppCompatActivity(), AsyncResponse {

    /**
     * A companion object for class variables.
     */
    companion object {
        internal var grantedPermissions = ArrayList<String>()
        internal const val PERMISSIONS_REQUEST_CODE = 8888
        internal val REQUIRED_PERMISSIONS =
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        internal var frames = ArrayList<Image>()
        internal var images = ArrayList<Image>()
        internal var hasInternet: Boolean = false
        internal lateinit var imagesAdapter: ImagesAdapter
    }

    /**
     * OnCreate BaseActivity
     *
     * @param savedInstanceState
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get granted permission
        getPermissions()

        // Get images to display
        if (images.size == 0)
            getImages()

        // Get frames to add
        if (frames.size == 0)
            getFrames()
    }

    override fun taskCompleted(stringId: Int) {
        toast(getString(stringId))
    }

    /**
     * Class to manage Image Save
     */
    inner class ImageSave(
        private val isEdited: Boolean,
        private val image: Image
    ) :
        AsyncTask<String, Void, Bitmap>() {

        override fun doInBackground(vararg args: String): Bitmap? {
            return when {
                // Image is taken from Gallery
                Manifest.permission.WRITE_EXTERNAL_STORAGE == image.origin && args.size >= 0 -> BitmapFactory.decodeFile(
                    args[0]
                )

                // Image is taken with Camera
                Manifest.permission.CAMERA == image.origin -> MainActivity.getBitmapFromImageView()

                // Image has been edited
                isEdited -> ImageActivity.getBitmapFromImageView()

                // Image is downloaded
                else -> try {
                    BitmapFactory.decodeStream(java.net.URL(image.origin).openStream())
                } catch (e: Exception) {
                    null
                }
            }
        }

        override fun onPostExecute(result: Bitmap?) {
            image.write(result)
            image.save()
            taskCompleted(R.string.image_saved)
        }
    }

    /**
     * Method to show toast message
     */
    protected fun Context.toast(message: String) {
        val toastMessage =
            Toast.makeText(
                this,
                message, Toast.LENGTH_SHORT
            )
        val toastView = toastMessage.view
        toastMessage.view.setBackgroundResource(R.drawable.buildalbum_toast)
        val textView = toastView.findViewById(android.R.id.message) as TextView
        textView.setPadding(20, 0, 20, 0)
        textView.setTextColor(
            ContextCompat.getColor(
                this,
                R.color.colorWhite
            )
        )
        toastMessage.show()
    }

    /**
     * Method to check for the required permissions
     */
    private fun getPermissions() {
        var requestPermissions = ArrayList<String>()
        REQUIRED_PERMISSIONS.forEach {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    it
                ) == PackageManager.PERMISSION_GRANTED
            )
                grantedPermissions.add(it)
            else
                requestPermissions.add(it)
        }
        if (requestPermissions.isNotEmpty()) {
            val requestedPermissionsArray = arrayOfNulls<String>(requestPermissions.size)
            requestPermissions.toArray(requestedPermissionsArray)
            ActivityCompat.requestPermissions(
                this,
                requestedPermissionsArray,
                PERMISSIONS_REQUEST_CODE
            )
        }
    }

    /**
     * Method to get all images
     *
     * @return paths of stored imagesNames
     */
    private fun getImages(): Int {
        images.addAll(DatabaseHelper(this).getAllImagesReverse())
        return images.size
    }

    /**
     * Method to get all frames
     */
    private fun getFrames(): Int {
        frames.addAll(DatabaseHelper(this).getAllFrames())
        return frames.size
    }
}