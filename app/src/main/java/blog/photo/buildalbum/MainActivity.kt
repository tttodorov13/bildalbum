package blog.photo.buildalbum

import android.Manifest.permission.CAMERA
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log.e
import android.widget.AdapterView
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import blog.photo.buildalbum.R.string.*
import blog.photo.buildalbum.model.Image
import blog.photo.buildalbum.receiver.ConnectivityReceiver
import blog.photo.buildalbum.tasks.*
import blog.photo.buildalbum.tasks.DownloadStatus.OK
import blog.photo.buildalbum.utils.ImagesAdapter
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Class to manage the main screen.
 */
class MainActivity() : BaseActivity(), ConnectivityReceiver.ConnectivityReceiverListener,
    DownloadData.OnDownloadComplete,
    JsonData.OnDataAvailable {

    private lateinit var image: Image

    /**
     * A companion object for class variables.
     */
    companion object {
        private const val tag = "MainActivity"
        private lateinit var file: File
        private val connectivityReceiver = ConnectivityReceiver()
        private lateinit var imageNewView: ImageView

        /**
         * Method to get a bitmap from the new image ImageView
         */
        internal fun getBitmapFromImageView(): Bitmap {
            return (imageNewView.drawable as BitmapDrawable).bitmap
        }
    }

    /**
     * OnCreate MainActivity
     *
     * @param savedInstanceState
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        imageNewView = imageViewCamera

        // Display images
        imagesAdapter = ImagesAdapter(this, images)
        girdViewImages.adapter = imagesAdapter

        // Click listener for ImageActivity
        girdViewImages.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ ->
                startActivity(
                    Intent(this, ImageActivity::class.java).putExtra(
                        "imageOriginalName",
                        images[position].name
                    ).putExtra("imageOriginalOrigin", images[position].origin)
                )
            }

        // Click listener for Add Image Dialog
        fab.setOnClickListener {
            var dialogItems = ArrayList<String>()

            if (hasInternet) {
                dialogItems.add(getString(download_from_pixabay))
                dialogItems.add(getString(download_from_flickr))
            }

            if (WRITE_EXTERNAL_STORAGE in grantedPermissions)
                dialogItems.add(getString(choice_from_gallery))

            if (CAMERA in grantedPermissions)
                dialogItems.add(getString(take_photo))

            if (getString(close) !in dialogItems)
                dialogItems.add(getString(close))

            val dialogItemsArray = arrayOfNulls<String>(dialogItems.size)
            dialogItems.toArray(dialogItemsArray)

            AlertDialog.Builder(this, R.style.BuildAlbumAlertDialog)
                .setTitle(getString(image_add))
                .setIcon(android.R.drawable.ic_input_add)
                .setItems(
                    dialogItemsArray
                ) { dialog, item ->
                    when {
                        dialogItems[item] == getString(take_photo) -> startIntentCamera()
                        dialogItems[item] == getString(choice_from_gallery) -> startIntentGallery()
                        dialogItems[item] == getString(download_from_flickr) -> downloadFromFlickr()
                        dialogItems[item] == getString(download_from_pixabay) -> downloadFromPixabay()
                        else -> dialog.dismiss()
                    }
                }
                .show()
        }
    }

    /**
     * OnActivityResult MainActivity
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == PERMISSIONS_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            when {
                // Image is taken with Camera
                data.extras?.get("data") != null -> {
                    val bmp = data.extras?.get("data") as Bitmap?
                    imageViewCamera.setImageBitmap(bmp)
                    SaveImage(this, false, Image(this, false, CAMERA)).execute()
                }

                // Image is taken from Gallery
                data.data != null -> {
                    val filePathColumn = arrayOf(MediaStore.Images.Media.DATA)
                    val cursor = contentResolver.query(
                        data.data!!,
                        filePathColumn, null, null, null
                    )
                    cursor!!.moveToFirst()
                    val filePath = cursor.getString(cursor.getColumnIndex(filePathColumn[0]))
                    cursor.close()
                    SaveImage(this, false, Image(this, false, WRITE_EXTERNAL_STORAGE)).execute(
                        filePath
                    )
                }
            }
        }
        // Image capturing is cancelled
        else {
            toast(getString(no_image_is_captured))
        }
    }

    /**
     * OnRequestPermissionsResult MainActivity
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        when (requestCode) {
            PERMISSIONS_REQUEST_CODE -> {
                // If permissions were granted add them all.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    grantedPermissions.addAll(permissions)
                } else {
                    // If permissions were not granted remove them.
                    grantedPermissions.removeAll(permissions)
                }
            }
            // Ignore all other requests.
            else -> {
            }
        }
    }

    /**
     * OnResume MainActivity
     *
     * Set connectivity receiver to listen for Internet connection.
     */
    override fun onResume() {
        super.onResume()
        ConnectivityReceiver.connectivityReceiverListener = this
    }

    /**
     * OnStart MainActivity
     *
     * Register connectivity receiver to listen for Internet connection.
     */
    override fun onStart() {
        super.onStart()

        // Register connectivity receiver
        registerReceiver(
            connectivityReceiver,
            IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        )
    }

    /**
     * Callback will be called when there is network change
     */
    override fun onNetworkConnectionChanged(isConnected: Boolean) {
        hasInternet = if (isConnected) {
            downloadFrames()
            true
        } else {
            toast(getString(enable_internet))
            false
        }
    }

    /**
     * OnStop MainActivity
     */
    override fun onStop() {
        unregisterReceiver(connectivityReceiver)
        super.onStop()
    }

    /**
     * Method to create image file on disk
     */
    private fun createImage(): File? {
        image = Image(this)
        file = image.file
        if (file.exists())
            file.delete()

        return try {
            val out = FileOutputStream(file)
            out.flush()
            out.close()
            file
        } catch (e: IOException) {
            toast(getString(not_enough_space_on_disk))
            e(tag, e.message.toString())
            null
        }
    }

    /**
     * Method to Take a Photo with Camera App
     */
    private fun startIntentCamera() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { intent ->
            // Ensure that there's a Camera Activity to handle the intent
            intent.resolveActivity(packageManager)?.also {
                // Create the File where the photo should go
//                val imageFile = createImage()
//
//                // Continue only if the file was successfully created
//                imageFile?.also {
//                    val photoURI: Uri = FileProvider.getUriForFile(
//                        this,
//                        "blog.photo.buildalbum.FileProvider",
//                        it
//                    )
//                    intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
//                }

                startActivityForResult(intent, PERMISSIONS_REQUEST_CODE)
            }
        }
    }

    /**
     * Method to Choice Image from Gallery App
     */
    private fun startIntentGallery() {
        Intent(
            Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        ).also { intent ->
            startActivityForResult(intent, PERMISSIONS_REQUEST_CODE)
        }
    }

    /**
     * Method to download frames
     */
    private fun downloadFrames() {
        DownloadData(
            this,
            DownloadSource.FRAMES
        ).execute(getString(FRAMES_URI))
    }

    /**
     * Method to download images from https://www.flickr.com
     */
    private fun downloadFromFlickr() {
        val uri = Uri.parse(getString(FLICKR_API_URI)).buildUpon()
            .appendQueryParameter("format", "json")
            .appendQueryParameter("nojsoncallback", "1")
            .build().toString()

        DownloadData(
            this,
            DownloadSource.FLICKR
        ).execute(uri)
    }

    /**
     * Method to download images from https://pixabay.com
     */
    private fun downloadFromPixabay() {
        val uri = Uri.parse(getString(PIXABAY_API_URI)).buildUpon()
            .appendQueryParameter("key", getString(PIXABAY_API_KEY))
            .build().toString()

        DownloadData(
            this,
            DownloadSource.PIXABAY
        ).execute(uri)
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
        if (status == OK)
            JsonData(this, source).execute(data)
    }

    /**
     * Method to download images
     *
     * @param data - images' URIs
     */
    override fun onDataAvailable(data: ArrayList<String>) {
        data.forEach {
            val image = Image(
                this,
                it.contains(Uri.parse(getString(FRAMES_URI)).authority.toString()),
                it
            )
            if (image !in images && image !in frames)
                SaveImage(
                    this,
                    false,
                    image
                ).execute()
        }
    }

    /**
     * Method to display error message on image download unsuccessful
     *
     * @param exception
     */
    override fun onError(exception: Exception) {
        toast(getString(download_exception).plus(exception))
    }
}
