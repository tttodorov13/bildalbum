package blog.photo.buildalbum

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import blog.photo.buildalbum.adapter.ImageAdapter
import blog.photo.buildalbum.adapter.PaneAdapter
import blog.photo.buildalbum.db.entity.Card
import blog.photo.buildalbum.db.entity.Image
import blog.photo.buildalbum.db.entity.Pane
import blog.photo.buildalbum.db.model.ImageViewModel
import blog.photo.buildalbum.db.model.PaneViewModel
import blog.photo.buildalbum.task.*
import kotlinx.android.synthetic.main.spinner_layout.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Class base for all application activities.
 */
open class BaseActivity : AppCompatActivity(), AsyncResponse, DownloadData.OnDownloadComplete,
    DownloadJson.OnDataAvailable {

    private val tag = "BaseActivity"
    private val permissionsRequired =
        arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    internal var permissionsGranted = ArrayList<String>()
    internal val permissionsRequestCode = 8888
    internal var taskCountDown = 0
    internal var hasInternet: Boolean = false
    internal lateinit var imageAdapter: ImageAdapter
    internal lateinit var imageViewModel: ImageViewModel
    internal lateinit var newImageView: ImageView
    internal lateinit var paneAdapter: PaneAdapter
    internal lateinit var paneViewModel: PaneViewModel

    /**
     * OnCreate BaseActivity
     *
     * @param savedInstanceState
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkInternet()

        // Get granted permission
        getPermissions()

        // Initialize images
        imageAdapter = ImageAdapter(this)

        imageViewModel = ViewModelProvider(this).get(ImageViewModel::class.java)

        imageViewModel.allImages.observe(this, Observer { images ->
            // Update the cached copy of the images in the adapter.
            images?.let { imageAdapter.setImages(it) }
        })

        // Initialize panes
        paneAdapter = PaneAdapter(this)

        paneViewModel = ViewModelProvider(this).get(PaneViewModel::class.java)

        paneViewModel.allPanes.observe(this, Observer { panes ->
            // Update the cached copy of the images in the adapter.
            panes?.let { paneAdapter.setPanes(it) }
        })
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
            DownloadJson(this, source).execute(data)
    }

    /**
     * Method to download images
     *
     * @param data - cards' URIs
     */
    override fun onDataAvailable(data: ArrayList<String>) {
        spinner_title.text = getString(R.string.progress_downloading)
        val list = ArrayList<Card>()
        data.forEach {
            val card = Card(
                false,
                "".plus(System.nanoTime()).plus(".png"),
                it
            )
            if (card !in imageAdapter.getImages() && card !in paneAdapter.getPanes())
                list.add(card)
        }
        val array = arrayOfNulls<Card>(list.size)
        list.toArray(array)
        spinner_percentage.text = "0 %"
        CardSave().execute(*array)
    }

    /**
     * Method to display error message on card download failed
     *
     * @param exception - exception thrown
     */
    override fun onError(exception: Exception) {
        toast(R.string.download_failed)
        Log.e(tag, exception.message.toString())
    }

    /**
     * OnResume BaseActivity
     *
     * Check for Internet connection.
     */
    override fun onResume() {
        super.onResume()
        checkInternet()
    }

    /**
     * OnStart BaseActivity
     *
     * Check for Internet connection.
     */
    override fun onStart() {
        super.onStart()
        checkInternet()
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
     * Method to show toast message
     *
     * @param stringId - ID of translatable string
     */
    internal fun Context.toast(stringId: Int) {
        val toastMessage =
            Toast.makeText(
                this,
                getString(stringId), Toast.LENGTH_SHORT
            )
        val toastView = toastMessage.view
        toastMessage.view.setBackgroundResource(R.drawable.buildalbum_toast)
        val textView = toastView.findViewById(android.R.id.message) as TextView
        textView.setPadding(20, 0, 20, 0)
        textView.setTextColor(
            ContextCompat.getColor(
                this,
                R.color.colorYellow
            )
        )
        toastMessage.show()
    }

    /**
     * Method to get a bitmap from the new image ImageView
     */
    internal fun getBitmapFromImageView(): Bitmap {
        return (newImageView.drawable as BitmapDrawable).bitmap
    }

    /**
     * Method to check for the required permissions
     */
    private fun getPermissions() {
        val requestPermissions = ArrayList<String>()
        permissionsRequired.forEach {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    it
                ) == PackageManager.PERMISSION_GRANTED
            )
                permissionsGranted.add(it)
            else
                requestPermissions.add(it)
        }
        if (requestPermissions.isNotEmpty()) {
            val requestedPermissionsArray = arrayOfNulls<String>(requestPermissions.size)
            requestPermissions.toArray(requestedPermissionsArray)
            ActivityCompat.requestPermissions(
                this,
                requestedPermissionsArray,
                permissionsRequestCode
            )
        }
    }

    /**
     * Method to write on filesystem
     *
     * @param card     -   image to be written to
     * @param bitmap    -   bitmap to be applied on image
     *
     * @return true if completed successfully
     */
    private fun writeOnFilesystem(card: Card, bitmap: Bitmap?): Boolean {
        val file = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), card.file)
        if (file.exists())
            file.delete()

        return try {
            val out = FileOutputStream(file)
            bitmap?.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.flush()
            out.close()
            true
        } catch (e: IOException) {
            Log.e(tag, e.message.toString())
            false
        }
    }

    /**
     * Method to check Internet connection.
     */
    private fun checkInternet() {
        CheckInternet(object : CheckInternet.Consumer {
            override fun accept(internet: Boolean) {
                hasInternet = internet
            }
        })
    }

    /**
     * Class to manage card saving
     *
     * @param isEdited  -   shows whether the card is edited on the filesystem
     * @param card      -   the card to be saved
     */
    inner class CardSave :
        AsyncTask<Card, String, Int>() {

        // Mark task beginning
        override fun onPreExecute() {
            onTaskBegin()
        }

        // Save the card in parallel thread
        override fun doInBackground(vararg args: Card): Int? {
            var bitmap: Bitmap?
            var count = 0

            args.forEach {
                // Multiple cards are downloaded
                if (args.size > 1) {
                    // Get bitmap from Internet
                    bitmap = try {
                        BitmapFactory.decodeStream(java.net.URL(it.source).openStream())
                    } catch (e: Exception) {
                        null
                    }

                    if (writeOnFilesystem(it, bitmap)) {
                        if (it.source.contains(Uri.parse(getString(R.string.PANES_URI)).authority.toString())) {
                            val pane = Pane(false, it.file, it.source)
                            paneViewModel.insert(pane)

                            // try to touch View of UI thread
                            runOnUiThread(java.lang.Runnable {
                                paneAdapter.notifyDataSetChanged()
                            })
                        } else {
                            val image = Image(false, it.file, it.source)
                            imageViewModel.insert(image)

                            // try to touch View of UI thread
                            runOnUiThread(java.lang.Runnable {
                                imageAdapter.notifyDataSetChanged()
                            })
                        }
                        count++
                        publishProgress(
                            "".plus((count.toDouble() / args.size * 100).toInt()).plus(
                                " %"
                            )
                        )
                    }
                }
                // Single card is added/edited
                else if (args.size == 1) {
                    // Get bitmap from imageView
                    bitmap = getBitmapFromImageView()

                    publishProgress(getString(R.string.progress_sketch))

                    if (writeOnFilesystem(it, bitmap)) {
                        // Image is edited
                        return if (it.isEdited)
                            R.string.image_edited
                        // Image is added
                        else {
                            val image = Image(false, it.file, it.source)
                            imageViewModel.insert(image)

                            // try to touch View of UI thread
                            runOnUiThread(java.lang.Runnable {
                                imageAdapter.notifyDataSetChanged()
                            })

                            R.string.image_added
                        }
                    }
                }
            }
            return if (count > 0)
                R.string.download_completed
            else
                R.string.nothing_new_captured
        }

        // Show proper message on progress
        override fun onProgressUpdate(vararg values: String?) {
            if (values.isNotEmpty())
                spinner_percentage.text = values[0]
        }

        // Show proper message on end
        override fun onPostExecute(result: Int) {
            onTaskComplete(result)
        }
    }
}