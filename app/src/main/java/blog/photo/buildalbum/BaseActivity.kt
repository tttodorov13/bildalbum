package blog.photo.buildalbum

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import blog.photo.buildalbum.model.Image
import blog.photo.buildalbum.utils.BuildAlbumDBOpenHelper
import blog.photo.buildalbum.utils.PicturesAdapter

/**
 * Class to be base for all activities of the application.
 */
@SuppressLint("Registered")
open class BaseActivity : AppCompatActivity() {

    /**
     * A companion object for static variables
     */
    companion object {
        private const val tag = "BaseActivity"
        internal var grantedPermissions = ArrayList<String>()
        internal const val PERMISSIONS_REQUEST_CODE = 8888
        internal val REQUIRED_PERMISSIONS =
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        internal var frames = ArrayList<Image>()
        internal var images = ArrayList<Image>()
        internal lateinit var imagesAdapter: PicturesAdapter
    }

    /**
     * OnCreate BaseActivity
     *
     * @param savedInstanceState
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
     * Method to get images from database
     *
     * @return paths of stored imagesNames
     */
    private fun getImages(): Int {
        images.addAll(BuildAlbumDBOpenHelper(this, null).getAllImagesReverse())
        return images.size
    }

    /**
     * Method to get frames from database
     */
    private fun getFrames(): Int {
        frames.addAll(BuildAlbumDBOpenHelper(this, null).getAllFrames())
        return frames.size
    }
}