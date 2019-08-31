package blog.photo.bildalbum

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.os.Environment
import android.util.Log.e
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import blog.photo.bildalbum.model.Image
import blog.photo.bildalbum.utils.*
import blog.photo.bildalbum.utils.DownloadStatus.NETWORK_ERROR
import blog.photo.bildalbum.utils.DownloadStatus.OK
import kotlinx.android.synthetic.main.content_main.*
import java.io.File
import java.io.FileOutputStream
import java.lang.System.currentTimeMillis
import java.util.*

/**
 * Class that manages the main screen.
 */
class MainActivity() : AppCompatActivity(), DownloadData.OnDownloadComplete,
    JsonData.OnDataAvailable {

    /**
     * A companion object to declare variables for displaying images
     */
    companion object {
        lateinit var storedImagesPaths: ArrayList<String>
        lateinit var imagesAdapter: ImagesAdapter
    }

    /**
     * OnCreate Activity
     *
     * @param savedInstanceState
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        storedImagesPaths = getStoredImagesPaths()
        imagesAdapter = ImagesAdapter(this, storedImagesPaths)
        gridView.adapter = imagesAdapter

        gridView.onItemClickListener = object : AdapterView.OnItemClickListener {
            override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                val intent = Intent(applicationContext, ImageActivity::class.java)
                intent.putExtra("imageFilePath", storedImagesPaths[position])
                startActivity(intent)
            }
        }

        buttonFlickrImagesDownload?.setOnClickListener {
            getImagesFlickr()
        }

        buttonPixabayImagesDownload?.setOnClickListener {
            getImagesPixabay()
        }
    }

    /**
     * Helper class for creating new image
     *
     * @param context
     * @param imageView
     */
    inner class SaveImage(context: Context, var imageView: ImageView) :
        AsyncTask<String, Void, Bitmap>() {
        val context = context

        override fun doInBackground(vararg args: String): Bitmap? {
            var bm: Bitmap? = null
            try {
                val `in` = java.net.URL(args[0]).openStream()
                bm = BitmapFactory.decodeStream(`in`)
            } catch (e: Exception) {
                e("Error", e.message.toString())
                e.printStackTrace()
            }
            return bm
        }

        override fun onPostExecute(result: Bitmap) {
            val path = writeImage(result)

            // Update the images GridView
            storedImagesPaths.add(0, path)
            imagesAdapter.notifyDataSetChanged();

            BuildAlbumDBOpenHelper(context, null).addImage(Image(path))
            imageView.setImageBitmap(result)
        }

        private fun writeImage(finalBitmap: Bitmap): String {
            val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)

            if (!storageDir!!.exists()) {
                storageDir.mkdirs()
            }

            val file = File(storageDir, "img" + currentTimeMillis() + ".jpg")
            if (file.exists())
                file.delete()

            try {
                val out = FileOutputStream(file)
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                out.flush()
                out.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return file.canonicalPath
        }
    }

    /**
     * Method to download images from Flickr
     */
    private fun getImagesFlickr() {
        val uri = createUriFlickr(
            getString(R.string.FLICKR_API_URI),
            getString(R.string.FLICKR_API_TAGS),
            getString(R.string.FLICKR_API_LANG),
            true
        )
        DownloadData(this, DownloadSource.FLICKR).execute(uri)
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
     * Method to download images from Pixabay
     */
    private fun getImagesPixabay() {
        val uri = createUriPixabay(
            getString(R.string.PIXABAY_API_URI),
            getString(R.string.PIXABAY_API_KEY)
        )
        DownloadData(this, DownloadSource.PIXABAY).execute(uri)
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
     * Method to download images
     *
     * @param data - images' URIs
     */
    override fun onDataAvailable(data: ArrayList<String>) {
        data.forEach {
            SaveImage(
                this,
                LayoutInflater.from(this).inflate(
                    R.layout.image_layout,
                    null
                ).findViewById(R.id.picture)
            ).execute(it)
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
            toast(getString(R.string.check_internet_connection))
    }

    /**
     * Method to display error message on image download unsuccessful
     *
     * @param exception
     */
    override fun onError(exception: Exception) {
        toast("DownloadData Exception: $exception")
    }

    /**
     * Method to get images paths from database
     *
     * @return paths of stored images
     */
    fun getStoredImagesPaths(): ArrayList<String> {
        var listStoredImagesPaths = ArrayList<String>()
        val cursor = BuildAlbumDBOpenHelper(this, null).getAllImagesReverse()

        if (cursor!!.moveToFirst()) {
            listStoredImagesPaths.add(
                cursor.getString(
                    cursor.getColumnIndex(
                        BuildAlbumDBOpenHelper.COLUMN_PATH
                    )
                )
            )
            while (cursor.moveToNext()) {
                listStoredImagesPaths.add(
                    cursor.getString(
                        cursor.getColumnIndex(
                            BuildAlbumDBOpenHelper.COLUMN_PATH
                        )
                    )
                )
            }
        }
        cursor.close()

        return listStoredImagesPaths
    }

    /**
     * Extension function to show toast message
     */
    fun Context.toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
