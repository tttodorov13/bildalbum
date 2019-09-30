package blog.photo.buildalbum

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import blog.photo.buildalbum.adapters.ImagesAdapter
import blog.photo.buildalbum.models.Image
import blog.photo.buildalbum.tasks.*
import blog.photo.buildalbum.utils.DatabaseHelper

/**
 * Class base for all activities of the application.
 */
// TODO: Add text to progress bar spinner
// TODO: Automate download new frames
// TODO: Find and fix Unable to decode stream: java.io.FileNotFoundException
// TODO: Translate in all Amazon sale's languages
open class AppBase : AppCompatActivity(), AsyncResponse, DownloadData.OnDownloadComplete,
    JsonData.OnDataAvailable {

    /**
     * OnCreate AppBase
     *
     * @param savedInstanceState
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get granted permission
        getPermissions()
    }

    /**
     * Method to mark image download complete
     *
     * @param data
     * @param source
     * @param status
     */
    override fun onDownloadComplete(
        data: String,
        source: DownloadSource,
        status: DownloadStatus
    ) {
        if (status == DownloadStatus.OK && data.isNotBlank())
            JsonData(this, source).execute(data)
    }

    /**
     * Method to download images
     *
     * @param data - images' URIs
     */
    override fun onDataAvailable(data: ArrayList<String>) {
        data.forEach {
            ImageSave(
                false, Image(
                    this,
                    it.contains(Uri.parse(getString(R.string.FRAMES_URI)).authority.toString()),
                    it
                )
            ).execute()
        }
    }

    /**
     * Method to display error message on image download unsuccessful
     *
     * @param exception
     */
    override fun onError(exception: Exception) {
        toast(getString(R.string.download_exception).plus(exception))
    }

    /**
     * Method to customize async task begin
     */
    override fun onTaskBegin() {
        taskCountDown++
    }

    /**
     * Method to customize async task end
     */
    override fun onTaskComplete(stringId: Int) {
        taskCountDown--
    }

    /**
     * Class to manage Image Save
     */
    inner class ImageSave(
        private val isEdited: Boolean,
        private val image: Image
    ) :
        AsyncTask<String, Void, Bitmap>() {

        override fun onPreExecute() {
            onTaskBegin()
        }

        override fun doInBackground(vararg args: String): Bitmap? {
            return when {
                // Image has been edited
                isEdited -> ImageActivity.getBitmapFromImageView()

                // Image from Gallery
                Manifest.permission.WRITE_EXTERNAL_STORAGE == image.origin && args.size >= 0 -> BitmapFactory.decodeFile(
                    args[0]
                )

                // Image from Camera
                Manifest.permission.CAMERA == image.origin -> MainActivity.getBitmapFromImageView()

                // Image download
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
            if (image.isFrame)
                onTaskComplete(R.string.pane_added)
            else
                onTaskComplete(R.string.image_saved)
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
     * Method to download frames
     */
    protected fun downloadFrames() {
        DownloadData(
            this,
            DownloadSource.FRAMES
        ).execute(getString(R.string.FRAMES_URI))
    }

    /**
     * Method to get all frames
     */
    protected fun getFrames(): Int {
        frames.addAll(DatabaseHelper(this).getAllFramesReverse())
        return frames.size
    }

    /**
     * Method to get all images
     *
     * @return paths of stored imagesNames
     */
    protected fun getImages(): Int {
        images.addAll(DatabaseHelper(this).getAllImagesReverse())
        return images.size
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
        internal var taskCountDown = 0

        internal lateinit var adapterFrames: ImagesAdapter
        internal lateinit var adapterImages: ImagesAdapter
    }
}