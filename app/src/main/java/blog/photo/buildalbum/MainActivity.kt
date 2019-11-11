package blog.photo.buildalbum

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.AdapterView
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isGone
import blog.photo.buildalbum.adapter.IconAdapter
import blog.photo.buildalbum.db.entity.Image
import blog.photo.buildalbum.task.DownloadData
import blog.photo.buildalbum.task.DownloadSource
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.android.synthetic.main.spinner_layout.*
import java.io.FileNotFoundException

/**
 * Class to manage the main screen.
 */
class MainActivity : BaseActivity() {

    /**
     * OnCreate MainActivity
     *
     * @param savedInstanceState    - to be used on app resume
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        newImageView = mainImageView

        // Display images in an interesting layout
        imagesGridView.adapter = imageAdapter

        // Click listener for ImageActivity
        imagesGridView.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ ->
                startActivity(
                    Intent(this, ImageActivity::class.java)
                        .putExtra(
                            "file",
                            imageAdapter.getImages()[position].file
                        )
                        .putExtra(
                            "source",
                            imageAdapter.getImages()[position].source
                        )
                )
            }

        // Click listener for add button
        fabAdd.setOnClickListener {
            val itemTexts = ArrayList<String>()
            val itemIcons = ArrayList<Int>()

            if (hasInternet) {
                itemTexts.add(getString(R.string.latest_from_pixabay))
                itemIcons.add(R.drawable.ic_download_24dp)
                itemTexts.add(getString(R.string.latest_from_flickr))
                itemIcons.add(R.drawable.ic_download_24dp)
            } else
                toast(R.string.enable_internet)

            if (Manifest.permission.WRITE_EXTERNAL_STORAGE in permissionsGranted) {
                itemTexts.add(getString(R.string.pick_from_gallery))
                itemIcons.add(R.drawable.ic_gallery_24dp)
            }

            if (Manifest.permission.CAMERA in permissionsGranted) {
                itemTexts.add(getString(R.string.take_photo))
                itemIcons.add(R.drawable.ic_camera_24dp)
            }

            itemTexts.add(getString(R.string.close))
            itemIcons.add(R.drawable.ic_close_24dp)

            val adapter = IconAdapter(this, itemTexts, itemIcons)

            val alertDialog = AlertDialog.Builder(this)
                .setTitle(getString(R.string.image_add))
                .setIcon(R.drawable.ic_add_24dp)
            alertDialog.setAdapter(adapter) { dialog, item ->
                when (adapter.getItem(item)) {
                    getString(R.string.take_photo) -> startIntentCamera()
                    getString(R.string.pick_from_gallery) -> startIntentGallery()
                    getString(R.string.latest_from_flickr) -> downloadFromFlickr()
                    getString(R.string.latest_from_pixabay) -> downloadFromPixabay()
                    else -> dialog.dismiss()
                }
            }.show()
        }
    }

    /**
     * OnActivityResult MainActivity
     *
     * @param requestCode   - sent on adding new image
     * @param resultCode    - comes with request end
     * @param data          - comes as a result
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Check for the right request/result codes and data available
        if (requestCode == permissionsRequestCode && resultCode == RESULT_OK && data != null) {
            spinner_title.text = getString(R.string.progress_saving)
            val image =
                Image(
                    false,
                    "".plus(System.nanoTime()).plus(".png"),
                    packageName
                )

            when {
                // Image is taken with Camera
                data.extras?.get("data") != null -> {
                    mainImageView.setImageBitmap(data.extras?.get("data") as Bitmap?)
                }

                // Image is taken from Gallery
                data.data != null -> {
                    try {
                        mainImageView.setImageBitmap(
                            BitmapFactory.decodeStream(
                                contentResolver.openInputStream(data.data!!)
                            )
                        )
                    } catch (e: FileNotFoundException) {
                        Log.e("MainActivity", e.message.toString())
                        toast(R.string.nothing_new_captured)
                        return
                    }
                }
            }
            CardSave().execute(image)
        }
        // Image capturing is cancelled
        else
            toast(R.string.nothing_new_captured)
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
        if (requestCode == permissionsRequestCode &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        )
            permissionsGranted.addAll(permissions)
        // If permissions were not granted remove them.
        else
            permissionsGranted.removeAll(permissions)
    }

    /**
     * On image save task begin
     * disable FAB and show progress bar spinner
     */
    override fun onTaskBegin() {
        super.onTaskBegin()
        fabAdd.isEnabled = false
        fabAdd.alpha = 0.1F
        progressSpinner.isGone = false
    }

    /**
     * On all image save task complete
     * enable FAB and hide progress bar spinner
     */
    override fun onTaskComplete(stringId: Int) {
        super.onTaskComplete(stringId)
        if (taskCountDown <= 0) {
            progressSpinner.isGone = true
            fabAdd.isEnabled = true
            fabAdd.alpha = 1.0F
        }
        toast(stringId)
    }

    /**
     * Method to Take a Photo with Camera App
     */
    private fun startIntentCamera() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { intent ->
            // Ensure there is a Camera Activity to handle the intent
            intent.resolveActivity(packageManager)?.also {
                startActivityForResult(intent, permissionsRequestCode)
            }
        }
    }

    /**
     * Method to Choice Image from Gallery App
     */
    private fun startIntentGallery() {
        Intent(
            Intent.ACTION_PICK
        ).also { intent ->
            intent.type = "image/*"
            startActivityForResult(intent, permissionsRequestCode)
        }
    }

    /**
     * Method to download images from https://www.flickr.com
     */
    private fun downloadFromFlickr() {
        val uri = Uri.parse(getString(R.string.FLICKR_API_URI)).buildUpon()
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
        val uri = Uri.parse(getString(R.string.PIXABAY_API_URI)).buildUpon()
            .appendQueryParameter("key", getString(R.string.PIXABAY_API_KEY))
            .build().toString()

        DownloadData(
            this,
            DownloadSource.PIXABAY
        ).execute(uri)
    }
}