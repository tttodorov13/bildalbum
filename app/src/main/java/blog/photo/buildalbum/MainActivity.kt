package blog.photo.buildalbum

import android.Manifest.permission.CAMERA
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log.e
import android.view.LayoutInflater
import android.widget.AdapterView
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import blog.photo.buildalbum.R.string.*
import blog.photo.buildalbum.model.Image
import blog.photo.buildalbum.network.DownloadData
import blog.photo.buildalbum.network.DownloadSource
import blog.photo.buildalbum.network.DownloadStatus
import blog.photo.buildalbum.network.DownloadStatus.NETWORK_ERROR
import blog.photo.buildalbum.network.DownloadStatus.OK
import blog.photo.buildalbum.network.JsonData
import blog.photo.buildalbum.utils.BuildAlbumDBOpenHelper
import blog.photo.buildalbum.utils.PicturesAdapter
import kotlinx.android.synthetic.main.content_main.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Class to manage the main screen.
 */
// TODO: Add check for Internet
class MainActivity() : AppCompatActivity(), DownloadData.OnDownloadComplete,
    JsonData.OnDataAvailable {

    private lateinit var image: Image
    private var grantedPermissions = ArrayList<String>()

    /**
     * A companion object to declare variables for displaying imagesNames
     */
    companion object {
        private const val tag = "MainActivity"
        private const val PERMISSIONS_REQUEST_CODE = 8888
        private val REQUIRED_PERMISSIONS =
            arrayOf(CAMERA, WRITE_EXTERNAL_STORAGE)
        var frames = ArrayList<Image>()
        var images = ArrayList<Image>()
        private lateinit var file: File
        lateinit var imagesAdapter: PicturesAdapter
    }

    /**
     * OnCreate Activity
     *
     * @param savedInstanceState
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Get the app granted permission
        getPermissions()

        // Get images to display
        if (images.size == 0)
            getImages()
        imagesAdapter = PicturesAdapter(this, images)
        girdViewImages.adapter = imagesAdapter

        // TODO: Do not download frames when No Internet
        // Get frames to add
        if (frames.size == 0 && getFrames() == 0)
            downloadFrames()

        girdViewImages.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ ->
                val intent = Intent(this, ImageActivity::class.java)
                intent.putExtra("imageOriginalName", images[position].name)
                intent.putExtra("imageOriginalOrigin", images[position].origin)
                startActivity(intent)
            }

        buttonAddImage.setOnClickListener {
            var items = ArrayList<String>()
            if (CAMERA in grantedPermissions)
                items.add(getString(take_photo))
            if (WRITE_EXTERNAL_STORAGE in grantedPermissions)
                items.add(getString(choice_from_gallery))
            items.add(getString(download_from_flickr))
            items.add(getString(download_from_pixabay))
            items.add(getString(close))

            val itemsArray = arrayOfNulls<String>(items.size)
            items.toArray(itemsArray)

            val builder = AlertDialog.Builder(this, R.style.BuildAlbumAlertDialog)
            builder.setTitle(getString(add_image)).setItems(
                itemsArray
            ) { dialog, item ->
                when {
                    items[item] == getString(take_photo) -> startIntentCamera()
                    items[item] == getString(choice_from_gallery) -> startIntentGallery()
                    items[item] == getString(download_from_flickr) -> downloadFromFlickr()
                    items[item] == getString(download_from_pixabay) -> downloadFromPixabay()
                    else -> dialog.dismiss()
                }
            }
            builder.show()
        }
    }

    /**
     * OnActivityResult Activity
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK && requestCode == PERMISSIONS_REQUEST_CODE && data != null) {
            when {
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

                // Image capturing is cancelled
                else -> toast(getString(no_image_is_captured))
            }
        }
    }

    /**
     * OnRequestPermissionsResult Activity
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
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    grantedPermissions.addAll(permissions)
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    grantedPermissions.removeAll(permissions)
                }
            }
            // Add other 'when' lines to check for other
            // permissions this app might request.
            else -> {
                // Ignore all other requests.
            }
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
        if (!requestPermissions.isEmpty()) {
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
     * Method to Take a Photo with Camera app
     */
    private fun startIntentCamera() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { intent ->
            // Ensure that there's a camera activity to handle the intent
            intent.resolveActivity(packageManager)?.also {
                // Create the File where the photo should go
                val imageFile = createImage()

                // Continue only if the File was successfully created
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
     * Method to Catch Image from Gallery
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
    // TODO: Do not download image with same origin twice
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
                imagesAdapter.notifyDataSetChanged();
            }
        }

        // Write the image into the file system
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
     * Method to download framesNames
     */
    private fun downloadFrames() {
        DownloadData(
            this,
            DownloadSource.FRAMES
        ).execute(getString(FRAMES_URI))
    }

    /**
     * Method to download imagesNames from https://www.flickr.com
     */
    // TODO: Case no Internet
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
     * Method to download imagesNames from https://pixabay.com
     */
    // TODO: Case no Internet
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
     * Method to mark image download completion
     *
     * @param data
     * @param source
     * @param status
     */
    // TODO: Fix app crash on image download when No Internet
    override fun onDownloadComplete(
        data: String,
        source: DownloadSource,
        status: DownloadStatus
    ) {
        if (status == OK)
            JsonData(this, source).execute(data)
        if (status == NETWORK_ERROR)
            toast(getString(enable_internet))
    }

    /**
     * Method to download imagesNames
     *
     * @param data - images' URIs
     */
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
    // TODO: Fix app crash on image download when No Internet
    override fun onError(exception: Exception) {
        toast(getString(download_exception).plus(exception))
    }

    /**
     * Method to get imagesNames paths from database
     */
    private fun getFrames(): Int {
        frames.addAll(BuildAlbumDBOpenHelper(this, null).getAllFrames())
        return frames.size
    }

    /**
     * Method to get imagesNames paths from database
     *
     * @return paths of stored imagesNames
     */
    private fun getImages(): Int {
        images.addAll(BuildAlbumDBOpenHelper(this, null).getAllImagesReverse())
        return images.size
    }

    /**
     * Method to get a bitmap from ImageView
     */
    fun getDefaultImageBitmap(): Bitmap {
        return ((LayoutInflater.from(this).inflate(
            R.layout.image_layout,
            null
        ).findViewById(R.id.picture) as ImageView).drawable as BitmapDrawable).bitmap
    }

    /**
     * Extension method to show toast message
     */
    private fun Context.toast(message: String) {
        val toastMessage =
            Toast.makeText(this, message, Toast.LENGTH_SHORT)
        val toastView = toastMessage.view
        toastMessage.view.setBackgroundResource(R.drawable.buildalbum_toast)
        (toastView.findViewById(android.R.id.message) as TextView).setTextColor(
            ContextCompat.getColor(
                this,
                R.color.colorWhite
            )
        )
        toastMessage.show()
    }
}
