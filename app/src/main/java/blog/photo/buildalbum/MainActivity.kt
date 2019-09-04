package blog.photo.buildalbum

import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log.e
import android.widget.AdapterView
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
class MainActivity() : AppCompatActivity(), DownloadData.OnDownloadComplete,
    JsonData.OnDataAvailable {

    private lateinit var image: Image

    /**
     * A companion object to declare variables for displaying imagesNames
     */
    companion object {
        private const val tag = "MainActivity"
        private const val REQUEST_TAKE_PHOTO = 100
        var frames = ArrayList<Image>()
        var images = ArrayList<Image>()
        lateinit var file: File
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

        imagesAdapter = PicturesAdapter(this, images)
        girdViewImages.adapter = imagesAdapter

        // TODO Fix app crash on image download when No Internet
        // Get images to display
        if (getImages() == 0) {
            downloadImagesFromFlickr()
            downloadImagesFromPixabay()
        }
        // Get frames to add
        if (getFrames() == 0)
            downloadFrames()

        girdViewImages.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ ->
                val intent = Intent(this, ImageActivity::class.java)
                intent.putExtra("imageOriginalName", images[position].name)
                startActivity(intent)
            }

        buttonTakePhoto.setOnClickListener {
            val camera = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            camera.also { takePictureIntent ->
                // Ensure that there's a camera activity to handle the intent
                takePictureIntent.resolveActivity(packageManager)?.also {
                    // Create the File where the photo should go
                    val photoFile = createImage()

                    // Continue only if the File was successfully created
                    photoFile?.also {
                        val photoURI: Uri = FileProvider.getUriForFile(
                            this,
                            "blog.photo.buildalbum.FileProvider",
                            it
                        )
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)

                        // Temporary grant write URI permissions
                        packageManager
                            .queryIntentActivities(
                                camera,
                                PackageManager.MATCH_DEFAULT_ONLY
                            ).forEach { resolvedIntentInfo ->
                                applicationContext.grantUriPermission(
                                    resolvedIntentInfo.activityInfo.packageName,
                                    photoURI,
                                    FLAG_GRANT_WRITE_URI_PERMISSION
                                )
                            }
                    }

                    startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO)
                }
            }
        }

        // TODO: Add Upload Photo functionality
    }

    /**
     * OnActivityResult Activity
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_TAKE_PHOTO && (resultCode == RESULT_OK)) {
            BuildAlbumDBOpenHelper(this, null).addImage(
                image
            )
            images.add(0, image)
            imagesAdapter.notifyDataSetChanged()
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
            e.printStackTrace()
            null
        }
    }

    /**
     * Helper class for creating new image
     */
    inner class SavePicture(private val image: Image) :
        AsyncTask<String, Void, Bitmap>() {

        override fun doInBackground(vararg params: String): Bitmap? {
            try {
                val `in` = java.net.URL(image.origin).openStream()
                return BitmapFactory.decodeStream(`in`)
            } catch (e: Exception) {
                toast(getString(not_enough_space_on_disk))
                e(tag, e.message.toString())
                e.printStackTrace()
            }
            return convertImageViewToBitmap(imageViewImageNew)
        }

        override fun onPostExecute(result: Bitmap) {
            if (image.isFrame) {
                if (image !in frames) {
                    writeImage(result)
                    frames.add(0, image)
                    BuildAlbumDBOpenHelper(applicationContext, null).addFrame(
                        image
                    )
                }
                return
            }

            if (image !in images) {
                writeImage(result)
                BuildAlbumDBOpenHelper(applicationContext, null).addImage(
                    image
                )
                images.add(0, image)
                imagesAdapter.notifyDataSetChanged();
            }
        }

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
                toast(getString(not_enough_space_on_disk))
                e(tag, e.message.toString())
                e.printStackTrace()
            }
        }
    }

    /**
     * Method to download framesNames
     */
    private fun downloadFrames() {
        for (i in 1..getString(FRAMES_COUNT).toInt()) {
            val frame = "frame$i.png"
            DownloadData(
                this,
                DownloadSource.FRAMES
            ).execute(getString(FRAMES_URI).plus(frame))
        }
    }

    /**
     * Method to download imagesNames from https://www.flickr.com
     */
    private fun downloadImagesFromFlickr() {
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
    private fun downloadImagesFromPixabay() {
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
     * Method to download imagesNames
     *
     * @param data - imagesNames' URIs
     */
    override fun onDataAvailable(data: ArrayList<String>) {
        data.forEach {
            SavePicture(
                Image(
                    this,
                    it.contains(getString(FRAMES_URI)),
                    it
                )
            ).execute()
        }
    }

    /**
     * Method to mark image download completion
     *
     * @param data
     * @param source
     * @param status
     */
    override fun onDownloadComplete(data: String, source: DownloadSource, status: DownloadStatus) {
        if (status == OK)
            JsonData(this, source).execute(data)
        if (status == NETWORK_ERROR)
            toast(getString(enable_internet))
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
     * Method to get imagesNames paths from database
     */
    private fun getFrames(): Int {
        val cursor = BuildAlbumDBOpenHelper(this, null).getAllFrames()
        var frame: Image

        if (cursor!!.moveToFirst()) {
            frame = Image(
                this,
                true,
                cursor.getString(
                    cursor.getColumnIndex(
                        BuildAlbumDBOpenHelper.COLUMN_NAME
                    )
                ), cursor.getString(
                    cursor.getColumnIndex(
                        BuildAlbumDBOpenHelper.COLUMN_ORIGIN
                    )
                )
            )
            if (frame !in frames)
                frames.add(frame)
            while (cursor.moveToNext()) {
                frame = Image(
                    this,
                    true,
                    cursor.getString(
                        cursor.getColumnIndex(
                            BuildAlbumDBOpenHelper.COLUMN_NAME
                        )
                    ),
                    cursor.getString(
                        cursor.getColumnIndex(
                            BuildAlbumDBOpenHelper.COLUMN_ORIGIN
                        )
                    )
                )
                if (frame !in frames)
                    frames.add(frame)
            }
        }
        cursor.close()
        return images.size
    }

    /**
     * Method to get imagesNames paths from database
     *
     * @return paths of stored imagesNames
     */
    private fun getImages(): Int {
        val cursor = BuildAlbumDBOpenHelper(this, null).getAllImagesReverse()
        var image: Image

        if (cursor!!.moveToFirst()) {
            image = Image(
                this,
                cursor.getString(
                    cursor.getColumnIndex(
                        BuildAlbumDBOpenHelper.COLUMN_NAME
                    )
                ), cursor.getString(
                    cursor.getColumnIndex(
                        BuildAlbumDBOpenHelper.COLUMN_ORIGIN
                    )
                )
            )
            if (image !in images)
                images.add(image)
            while (cursor.moveToNext()) {
                image = Image(
                    this,
                    cursor.getString(
                        cursor.getColumnIndex(
                            BuildAlbumDBOpenHelper.COLUMN_NAME
                        )
                    ), cursor.getString(
                        cursor.getColumnIndex(
                            BuildAlbumDBOpenHelper.COLUMN_ORIGIN
                        )
                    )
                )
                if (image !in images)
                    images.add(image)
            }
        }
        cursor.close()
        return images.size
    }

    /**
     * Method to get a bitmap from ImageView
     */
    private fun convertImageViewToBitmap(view: ImageView): Bitmap {
        return (view.drawable as BitmapDrawable).bitmap
    }

    /**
     * Extension method to show toast message
     */
    private fun Context.toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
