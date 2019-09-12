package blog.photo.buildalbum

import android.Manifest.permission.CAMERA
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.ConnectivityManager
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log.e
import android.view.LayoutInflater
import android.widget.AdapterView
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.core.view.isGone
import blog.photo.buildalbum.R.string.*
import blog.photo.buildalbum.model.Image
import blog.photo.buildalbum.network.DownloadData
import blog.photo.buildalbum.network.DownloadSource
import blog.photo.buildalbum.network.DownloadStatus
import blog.photo.buildalbum.network.DownloadStatus.OK
import blog.photo.buildalbum.network.JsonData
import blog.photo.buildalbum.receiver.ConnectivityReceiver
import blog.photo.buildalbum.utils.BuildAlbumDBOpenHelper
import blog.photo.buildalbum.utils.PicturesAdapter
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
    private var dialogItems = ArrayList<String>()

    /**
     * A companion object for static variables
     */
    companion object {
        private const val tag = "MainActivity"
        private lateinit var file: File
    }

    /**
     * OnCreate MainActivity
     *
     * @param savedInstanceState
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Register connectivity receiver
        registerReceiver(
            ConnectivityReceiver(),
            IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        )

        // Get the app granted permission
        getPermissions()

        // Add items to the Dialog
        if (WRITE_EXTERNAL_STORAGE in grantedPermissions)
            dialogItems.add(getString(choice_from_gallery))
        if (CAMERA in grantedPermissions)
            dialogItems.add(getString(take_photo))
        dialogItems.add(getString(close))

        // Display images
        imagesAdapter = PicturesAdapter(this, images)
        girdViewImages.adapter = imagesAdapter
        
        // Click listener for ImageActivity
        girdViewImages.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ ->
                val intent = Intent(this, ImageActivity::class.java)
                intent.putExtra("imageOriginalName", images[position].name)
                intent.putExtra("imageOriginalOrigin", images[position].origin)
                startActivity(intent)
            }

        // Click listener for Add Image
        buttonAddImage.setOnClickListener {
            val itemsArray = arrayOfNulls<String>(dialogItems.size)
            dialogItems.toArray(itemsArray)

            val builder = AlertDialog.Builder(this, R.style.BuildAlbumAlertDialog)
            builder.setTitle(getString(add_image)).setItems(
                itemsArray
            ) { dialog, item ->
                when {
                    dialogItems[item] == getString(take_photo) -> startIntentCamera()
                    dialogItems[item] == getString(choice_from_gallery) -> startIntentGallery()
                    dialogItems[item] == getString(download_from_flickr) -> downloadFromFlickr()
                    dialogItems[item] == getString(download_from_pixabay) -> downloadFromPixabay()
                    else -> dialog.dismiss()
                }
            }
            builder.show()
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
        if (requestCode == PERMISSIONS_REQUEST_CODE && data != null) {
            when (resultCode) {
                RESULT_OK -> when {
                    // Image is taken with Camera
                    data.data == null -> {
                        SavePicture(Image(this, image.name)).execute()
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
                        SavePicture(Image(this)).execute(filePath)
                    }
                }
                RESULT_CANCELED
                    // Image capturing is cancelled
                -> toast(getString(no_image_is_captured))
            }
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
     */
    override fun onResume() {
        super.onResume()
        ConnectivityReceiver.connectivityReceiverListener = this
    }

    /**
     * Callback will be called when there is network change
     */
    override fun onNetworkConnectionChanged(isConnected: Boolean) {
        if (isConnected) {
            buttonAddImage.isGone = false
            dialogItems.add(0, getString(download_from_flickr))
            dialogItems.add(0, getString(download_from_pixabay))
            // TODO: Download new frames only when available
            downloadFrames()
        } else {
            dialogItems.remove(getString(download_from_flickr))
            dialogItems.remove(getString(download_from_pixabay))
            if (dialogItems.size <= 1)
                buttonAddImage.isGone = true
            toast(getString(enable_internet_to_download_images))
        }
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
                val imageFile = createImage()

                // Continue only if the file was successfully created
                imageFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        this,
                        "blog.photo.buildalbum.FileProvider",
                        it
                    )
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                }

                startActivityForResult(intent, PERMISSIONS_REQUEST_CODE)
            }
        }
    }

    /**
     * Method to Choice Picture from Gallery App
     */
    private fun startIntentGallery() {
        Intent(
            Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        ).also { intent ->
            startActivityForResult(intent, PERMISSIONS_REQUEST_CODE)
        }
    }

    /**
     * Helper class for creating new image
     */
    inner class SavePicture(private val image: Image) :
        AsyncTask<String, Void, Bitmap>() {

        override fun doInBackground(vararg params: String): Bitmap {
            // Picture is downloaded from Internet
            if (!image.origin.isBlank())
                try {
                    return BitmapFactory.decodeStream(java.net.URL(image.origin).openStream())
                } catch (e: Exception) {
                    e(tag, e.message.toString())
                }

            return when {
                // Picture is taken with Camera
                BitmapFactory.decodeFile(image.file.canonicalPath) != null -> BitmapFactory.decodeFile(
                    image.file.canonicalPath
                )

                // Picture is taken from Gallery
                params.size >= 0 -> BitmapFactory.decodeFile(params[0])

                // Default picture is used

                else -> getDefaultImageBitmap()
            }
        }

        override fun onPostExecute(result: Bitmap) {
            // Check if the image is frame
            if (image.isFrame) {
                if (image !in frames) {
                    writeImage(result)
                    BuildAlbumDBOpenHelper(applicationContext, null).addFrame(
                        image
                    )
                    frames.add(0, image)
                }
                return
            }

            // Check if the image already exists
            if (image !in images) {
                writeImage(result)
                BuildAlbumDBOpenHelper(applicationContext, null).addImage(
                    image
                )
                images.add(0, image)
                imagesAdapter.notifyDataSetChanged()
            }
        }

        // Write the image on the file system
        private fun writeImage(finalBitmap: Bitmap) {
            val file = image.file
            if (file.exists())
                file.delete()

            try {
                val out = FileOutputStream(file)
                finalBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                out.flush()
                out.close()
            } catch (e: IOException) {
                e(tag, e.message.toString())
            }
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
        val uri = createUriFlickr(
            getString(FLICKR_API_URI),
            getString(FLICKR_API_TAGS),
            getString(FLICKR_API_LANG),
            true
        )
        DownloadData(
            this,
            DownloadSource.FLICKR
        ).execute(uri)
    }

    /**
     * Method to create Flickr download URI
     *
     * @param baseUri
     * @param tags
     * @param lang
     * @param matchAll
     */
    private fun createUriFlickr(
        baseUri: String,
        tags: String,
        lang: String,
        matchAll: Boolean
    ): String {
        return Uri.parse(baseUri).buildUpon().appendQueryParameter("tags", tags)
            .appendQueryParameter("lang", lang)
            .appendQueryParameter("tagmode", if (matchAll) "ALL" else "ANY")
            .appendQueryParameter("format", "json")
            .appendQueryParameter("nojsoncallback", "1")
            .build().toString()
    }

    /**
     * Method to download images from https://pixabay.com
     */
    private fun downloadFromPixabay() {
        val uri = createUriPixabay(
            getString(PIXABAY_API_URI),
            getString(PIXABAY_API_KEY)
        )
        DownloadData(
            this,
            DownloadSource.PIXABAY
        ).execute(uri)
    }

    /**
     * Method to create Pixabay download URI
     *
     * @param baseUri
     * @param key
     */
    private fun createUriPixabay(baseUri: String, key: String): String {
        return Uri.parse(baseUri).buildUpon().appendQueryParameter("key", key)
            .build().toString()
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
    // TODO: Return proper message when no image is downloaded
    override fun onDataAvailable(data: ArrayList<String>) {
        data.forEach {
            SavePicture(
                Image(
                    this,
                    it.contains(Uri.parse(getString(FRAMES_URI)).authority.toString()),
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
        toast(getString(download_exception).plus(exception))
    }

    /**
     * Method to get default image bitmap
     */
    fun getDefaultImageBitmap(): Bitmap {
        return ((LayoutInflater.from(this).inflate(
            R.layout.image_layout,
            null
        ).findViewById(R.id.picture) as ImageView).drawable as BitmapDrawable).bitmap
    }
}
