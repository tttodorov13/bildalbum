package blog.photo.buildalbum

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import blog.photo.buildalbum.model.Image
import blog.photo.buildalbum.utils.DatabaseHelper
import blog.photo.buildalbum.adapters.ImagesAdapter

/**
 * Class to be base for all activities of the application.
 */
open class BaseActivity : AppCompatActivity() {

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