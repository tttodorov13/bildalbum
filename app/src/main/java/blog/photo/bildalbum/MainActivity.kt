package blog.photo.bildalbum

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.os.Environment
import android.util.Log.e
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.GridView
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

class MainActivity() : AppCompatActivity(), DownloadData.OnDownloadComplete,
    JsonData.OnDataAvailable {

    companion object {
        lateinit var gridView: GridView
        lateinit var storedImagesPaths: ArrayList<String>
        lateinit var imagesAdapter: ImagesAdapter
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        gridView = findViewById<View>(R.id.gridview) as GridView
        storedImagesPaths = getStoredImagesPaths()
        imagesAdapter = ImagesAdapter(this, storedImagesPaths)
        gridView.adapter = imagesAdapter

        gridView.onItemClickListener = object : AdapterView.OnItemClickListener {
            override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                // TODO: open ImageDetailsActivity for edit and share
                // val photoUri = getStoredImagesPaths().get(position)
                imagesAdapter.notifyDataSetChanged()
            }
        }

        buttonFlickrImagesDownload?.setOnClickListener {
            getImagesFlickr()
        }

        buttonPixabayImagesDownload?.setOnClickListener {
            getImagesPixabay()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_scrolling, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    inner class CreateImage(context: Context, var bmImage: ImageView) :
        AsyncTask<String, Void, Bitmap>() {
        val context = context

        override fun doInBackground(vararg urls: String): Bitmap? {
            var bm: Bitmap? = null
            try {
                val `in` = java.net.URL(urls[0]).openStream()
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

            ImagesDBOpenHelper(context, null).addImage(Image(path))
            bmImage.setImageBitmap(result)
        }

        private fun writeImage(finalBitmap: Bitmap): String {
            val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)

            if (!storageDir!!.exists()) {
                storageDir.mkdirs()
            }

            val file = File(storageDir, "pic" + currentTimeMillis() + ".jpg")
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

    private fun getImagesFlickr() {
        val uri = createUriFlickr(
            getString(R.string.FLICKR_API_URI),
            getString(R.string.FLICKR_API_TAGS),
            getString(R.string.FLICKR_API_LANG),
            true
        )
        DownloadData(this, DownloadSource.FLICKR).execute(uri)
    }

    private fun getImagesPixabay() {
        val uri = createUriPixabay(
            getString(R.string.PIXABAY_API_URI),
            getString(R.string.PIXABAY_API_KEY)
        )
        DownloadData(this, DownloadSource.PIXABAY).execute(uri)
    }

    private fun createUriFlickr(baseUri: String, tags: String, lang: String, matchAll: Boolean): String {
        return Uri.parse(baseUri).buildUpon().appendQueryParameter("tags", tags).appendQueryParameter("lang", lang)
            .appendQueryParameter("tagmode", if (matchAll) "ALL" else "ANY").appendQueryParameter("format", "json")
            .appendQueryParameter("nojsoncallback", "1")
            .build().toString()
    }

    private fun createUriPixabay(baseUri: String, key: String): String {
        return Uri.parse(baseUri).buildUpon().appendQueryParameter("key", key)
            .build().toString()
    }

    override fun onDownloadComplete(data: String, status: DownloadStatus, source: DownloadSource) {
        if (status == OK)
            JsonData(this, source).execute(data)
        if (status == NETWORK_ERROR)
            Toast.makeText(applicationContext, R.string.check_internet_connection, Toast.LENGTH_SHORT).show()
    }

    override fun onDataAvailable(data: ArrayList<String>) {
        data.forEach {
            CreateImage(
                this,
                LayoutInflater.from(this).inflate(R.layout.image_layout, null).findViewById(R.id.picture)
            ).execute(it)
        }
    }

    override fun onError(exception: Exception) {
        Toast.makeText(applicationContext, "DownloadData Exception: $exception", Toast.LENGTH_SHORT).show()
    }

    fun getStoredImagesPaths(): ArrayList<String> {
        var listStoredImagesPaths = ArrayList<String>()
        val cursor = ImagesDBOpenHelper(this, null).getAllPhotosReverse()

        if (cursor!!.moveToFirst()) {
            listStoredImagesPaths.add(
                cursor.getString(
                    cursor.getColumnIndex(
                        ImagesDBOpenHelper.COLUMN_PATH
                    )
                )
            )
            while (cursor.moveToNext()) {
                listStoredImagesPaths.add(
                    cursor.getString(
                        cursor.getColumnIndex(
                            ImagesDBOpenHelper.COLUMN_PATH
                        )
                    )
                )
            }
        }
        cursor.close()

        return listStoredImagesPaths
    }
}
