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
import android.widget.AdapterView
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import blog.photo.bildalbum.model.Frame
import blog.photo.bildalbum.model.Image
import blog.photo.bildalbum.model.Picture
import blog.photo.bildalbum.network.DownloadData
import blog.photo.bildalbum.network.DownloadSource
import blog.photo.bildalbum.network.DownloadStatus
import blog.photo.bildalbum.network.DownloadStatus.NETWORK_ERROR
import blog.photo.bildalbum.network.DownloadStatus.OK
import blog.photo.bildalbum.network.JsonData
import blog.photo.bildalbum.utils.BuildAlbumDBOpenHelper
import blog.photo.bildalbum.utils.PicturesAdapter
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
     * A companion object to declare variables for displaying imagesNames
     */
    companion object {
        private const val tag = "MainActivity"
        var frames = ArrayList<Picture>()
        var images = ArrayList<Picture>()
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

        // Get imagesNames to display
        getImages()
        imagesAdapter = PicturesAdapter(this, images)
        girdViewImages.adapter = imagesAdapter

        girdViewImages.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ ->
                val intent = Intent(applicationContext, ImageActivity::class.java)
                intent.putExtra("imageOriginal", images[position].name)
                startActivity(intent)
            }

        // Get framesNames for adding
        getFrames()
        if (frames.size == 0)
            downloadFrames()

        buttonDownloadFromFlickr?.setOnClickListener {
            downloadImagesFromFlickr()
        }

        buttonDownloadFromPixabay?.setOnClickListener {
            downloadImagesFromPixabay()
        }
    }

    /**
     * Helper class for creating new image
     *
     * @param context
     * @param imageView
     */
    inner class SavePicture(context: Context, var imageView: ImageView) :
        AsyncTask<String, Void, Bitmap>() {
        val context = context
        private var picture = Picture("", "")

        override fun doInBackground(vararg params: String): Bitmap? {
            var bm: Bitmap? = null
            try {
                picture.uri = params[0]
                val `in` = java.net.URL(picture.uri).openStream()
                bm = BitmapFactory.decodeStream(`in`)
            } catch (e: Exception) {
                e(tag, e.message.toString())
                e.printStackTrace()
            }
            return bm
        }

        override fun onPostExecute(result: Bitmap) {
            if (picture.uri.contains(getString(R.string.FRAMES_URI))) {
                if (picture !in frames) {
                    picture.name = "frame" + currentTimeMillis() + ".png"
                    writeImage(result)
                    frames.add(0, picture)
                    BuildAlbumDBOpenHelper(context, null).addFrame(
                        Frame(picture)
                    )
                }
                return
            }

            if (picture !in images) {
                picture.name = "img" + currentTimeMillis() + ".png"
                writeImage(result)
                images.add(0, picture)
                imagesAdapter.notifyDataSetChanged();
                BuildAlbumDBOpenHelper(context, null).addImage(
                    Image(picture)
                )
            }
        }

        private fun writeImage(finalBitmap: Bitmap) {
            val file = getPicture(picture.name)
            if (file.exists())
                file.delete()

            try {
                val out = FileOutputStream(file)
                finalBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                out.flush()
                out.close()
            } catch (e: Exception) {
                toast(getString(R.string.picture_not_saved))
                e(tag, e.message.toString())
                e.printStackTrace()
            }
        }
    }

    /**
     * Method to download framesNames
     */
    private fun downloadFrames() {
        for (i in 1..getString(R.string.FRAMES_COUNT).toInt()) {
            val frame = "frame$i.png"
            DownloadData(
                this,
                DownloadSource.FRAMES
            ).execute(getString(R.string.FRAMES_URI) + frame)
        }
    }

    /**
     * Method to download imagesNames from Flickr
     */
    private fun downloadImagesFromFlickr() {
        val uri = createUriFlickr(
            getString(R.string.FLICKR_API_URI),
            getString(R.string.FLICKR_API_TAGS),
            getString(R.string.FLICKR_API_LANG),
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
     * Method to download imagesNames from Pixabay
     */
    private fun downloadImagesFromPixabay() {
        val uri = createUriPixabay(
            getString(R.string.PIXABAY_API_URI),
            getString(R.string.PIXABAY_API_KEY)
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
     * Method to get imagesNames paths from database
     *
     * @return paths of stored imagesNames
     */
    private fun getFrames() {
        val cursor = BuildAlbumDBOpenHelper(this, null).getAllFrames()
        var frame: Frame

        if (cursor!!.moveToFirst()) {
            frame = Frame(
                cursor.getString(
                    cursor.getColumnIndex(
                        BuildAlbumDBOpenHelper.COLUMN_NAME
                    )
                ), cursor.getString(
                    cursor.getColumnIndex(
                        BuildAlbumDBOpenHelper.COLUMN_URI
                    )
                )
            )
            frames.add(frame)
            while (cursor.moveToNext()) {
                frame = Frame(
                    cursor.getString(
                        cursor.getColumnIndex(
                            BuildAlbumDBOpenHelper.COLUMN_NAME
                        )
                    ),
                    cursor.getString(
                        cursor.getColumnIndex(
                            BuildAlbumDBOpenHelper.COLUMN_URI
                        )
                    )
                )
                frames.add(frame)
            }
        }
        cursor.close()
    }

    /**
     * Method to get imagesNames paths from database
     *
     * @return paths of stored imagesNames
     */
    private fun getImages() {
        val cursor = BuildAlbumDBOpenHelper(this, null).getAllImagesReverse()
        var image: Image

        if (cursor!!.moveToFirst()) {
            image = Image(
                cursor.getString(
                    cursor.getColumnIndex(
                        BuildAlbumDBOpenHelper.COLUMN_NAME
                    )
                ), cursor.getString(
                    cursor.getColumnIndex(
                        BuildAlbumDBOpenHelper.COLUMN_URI
                    )
                )
            )
            images.add(image)
            while (cursor.moveToNext()) {
                image = Image(
                    cursor.getString(
                        cursor.getColumnIndex(
                            BuildAlbumDBOpenHelper.COLUMN_NAME
                        )
                    ), cursor.getString(
                        cursor.getColumnIndex(
                            BuildAlbumDBOpenHelper.COLUMN_URI
                        )
                    )
                )
                images.add(image)
            }
        }
        cursor.close()
    }

    /**
     * Extension method to show toast message
     */
    private fun Context.toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    /**
     * Method to get picture from file system
     */
    fun getPicture(name: String): File {
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        if (!storageDir!!.exists()) {
            storageDir.mkdirs()
        }

        return File(storageDir, name)
    }
}
