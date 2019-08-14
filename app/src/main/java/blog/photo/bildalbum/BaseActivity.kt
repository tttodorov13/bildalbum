package blog.photo.bildalbum

import android.annotation.SuppressLint
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import blog.photo.bildalbum.receiver.ConnectivityReceiver
import blog.photo.bildalbum.utils.ImagesDBOpenHelper
import kotlinx.android.synthetic.main.content_main.*

// THIS IS THE BASE ACTIVITY OF ALL ACTIVITIES OF THE APPLICATION
@SuppressLint("Registered")
open class BaseActivity : AppCompatActivity(), ConnectivityReceiver.ConnectivityReceiverListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerReceiver(ConnectivityReceiver(), IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
    }

    override fun onResume() {
        super.onResume()
        ConnectivityReceiver.connectivityReceiverListener = this
    }

    /**
     * Callback will be called when there is network change
     */
    override fun onNetworkConnectionChanged(isConnected: Boolean) {
        if (isConnected) {
            showMessage(getString(R.string.internet_connection))
        } else {
            showMessage(getString(R.string.no_internet_connection))
        }
    }

    private fun showMessage(message: String) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
    }

    fun displayImage(imagePath: String) {
        var imageView = LayoutInflater.from(this).inflate(R.layout.image_layout, null).findViewById<ImageView>(R.id.picture)
        imageView.id = System.currentTimeMillis().toInt()
        imageView.setImageBitmap(BitmapFactory.decodeFile(imagePath))
        if (imageView.parent != null) {
            (imageView.parent as ViewGroup).removeView(imageView)
        }
        pictures.addView(imageView)
    }

    fun getStoredImagesPaths(): MutableList<String> {
        var listStoredImagesPaths = mutableListOf<String>()
        val cursor = ImagesDBOpenHelper(this, null).getAllPhotos()

        if (cursor!!.moveToFirst()) {
            listStoredImagesPaths.add(
                cursor.getString(
                    cursor.getColumnIndex(
                        ImagesDBOpenHelper.COLUMN_NAME
                    )
                )
            )
            while (cursor.moveToNext()) {
                listStoredImagesPaths.add(
                    cursor.getString(
                        cursor.getColumnIndex(
                            ImagesDBOpenHelper.COLUMN_NAME
                        )
                    )
                )
            }
        }
        cursor.close()

        return listStoredImagesPaths
    }

}