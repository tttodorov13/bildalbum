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
import android.widget.AdapterView
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isGone
import blog.photo.buildalbum.R.string.*
import blog.photo.buildalbum.adapters.IconsAdapter
import blog.photo.buildalbum.adapters.ImagesAdapter
import blog.photo.buildalbum.models.Image
import blog.photo.buildalbum.receivers.ConnectivityReceiver
import blog.photo.buildalbum.tasks.DownloadData
import blog.photo.buildalbum.tasks.DownloadSource
import blog.photo.buildalbum.tasks.DownloadStatus
import blog.photo.buildalbum.tasks.JsonData
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*

/**
 * Class to manage the main screen.
 */
class MainActivity() : AppBase(), ConnectivityReceiver.ConnectivityReceiverListener {

    /**
     * A companion object for class variables.
     */
    companion object {
        private val connectivityReceiver = ConnectivityReceiver()
        private lateinit var imageViewNew: ImageView

        /**
         * Method to get a bitmap from the new image ImageView
         */
        internal fun getBitmapFromImageView(): Bitmap {
            return (imageViewNew.drawable as BitmapDrawable).bitmap
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

        imageViewNew = imageViewCamera

        // Get images to display
        if (images.size == 0)
            getImages()

        // Display images
        imagesAdapter = ImagesAdapter(this, images)
        girdViewImages.adapter = imagesAdapter

        // Click listener for ImageActivity
        girdViewImages.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ ->
                startActivity(
                    Intent(this, ImageActivity::class.java).putExtra(
                        "originalName",
                        images[position].name
                    ).putExtra("originalOrigin", images[position].origin)
                )
            }

        // Click listener for Add Image Dialog
        fab.setOnClickListener {
            val itemTexts = ArrayList<String>()
            val itemIcons = ArrayList<Int>()

            if (hasInternet) {
                itemTexts.add(getString(download_from_pixabay))
                itemIcons.add(android.R.drawable.stat_sys_download_done)
                itemTexts.add(getString(download_from_flickr))
                itemIcons.add(android.R.drawable.stat_sys_download_done)
            }

            if (WRITE_EXTERNAL_STORAGE in grantedPermissions) {
                itemTexts.add(getString(choice_from_gallery))
                itemIcons.add(android.R.drawable.ic_menu_gallery)
            }

            if (CAMERA in grantedPermissions) {
                itemTexts.add(getString(take_photo))
                itemIcons.add(android.R.drawable.ic_menu_camera)
            }

            itemTexts.add(getString(close))
            itemIcons.add(android.R.drawable.ic_menu_close_clear_cancel)

            val adapter = IconsAdapter(this, itemTexts, itemIcons)

            val alertDialog = AlertDialog.Builder(this, R.style.BuildAlbumAlertDialog)
                .setTitle(getString(image_add))
                .setIcon(android.R.drawable.ic_input_add)
            alertDialog.setAdapter(adapter) { dialog, item ->
                when (adapter.getItem(item)) {
                    getString(take_photo) -> startIntentCamera()
                    getString(choice_from_gallery) -> startIntentGallery()
                    getString(download_from_flickr) -> downloadFromFlickr()
                    getString(download_from_pixabay) -> downloadFromPixabay()
                    else -> dialog.dismiss()
                }
            }.show()
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
        if (requestCode == PERMISSIONS_REQUEST_CODE && resultCode == RESULT_OK && data != null)
            when {
                // Image is taken with Camera
                data.extras?.get("data") != null -> {
                    val bmp = data.extras?.get("data") as Bitmap?
                    imageViewCamera.setImageBitmap(bmp)
                    ImageSave(false, Image(this, false, CAMERA)).execute()
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
                    ImageSave(
                        false,
                        Image(this, false, WRITE_EXTERNAL_STORAGE)
                    ).execute(
                        filePath
                    )
                }
            }
        // Image capturing is cancelled
        else
            toast(getString(no_image_is_captured))
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
        // If permissions were granted add them all.
        if (requestCode == PERMISSIONS_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            grantedPermissions.addAll(permissions)
        // If permissions were not granted remove them.
        else
            grantedPermissions.removeAll(permissions)
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
     * onNetworkConnectionChanged MainActivity
     *
     * Callback will be called when there is network change.
     */
    override fun onNetworkConnectionChanged(isConnected: Boolean) {
        hasInternet = if (isConnected) {
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
     * Method to Take a Photo with Camera App
     */
    private fun startIntentCamera() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { intent ->
            // Ensure there is a Camera Activity to handle the intent
            intent.resolveActivity(packageManager)?.also {
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
     * On image save task begin
     * disable FAB and show progress bar spinner
     */
    override fun onTaskBegin() {
        super.onTaskBegin()
        fab.isEnabled = false
        progressBarImagesScreen.isGone = false
    }

    /**
     * On all image save task complete
     * enable FAB and hide progress bar spinner
     */
    override fun onTaskComplete(stringId: Int) {
        super.onTaskComplete(stringId)
        if (taskCountDown <= 0) {
            progressBarImagesScreen.isGone = true
            fab.isEnabled = true
        }
    }
}
