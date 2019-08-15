package blog.photo.bildalbum

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.Toast
import androidx.core.view.isGone
import blog.photo.bildalbum.model.FlickrImage
import blog.photo.bildalbum.model.Image
import blog.photo.bildalbum.model.PixabayImage
import blog.photo.bildalbum.receiver.ConnectivityReceiver
import blog.photo.bildalbum.utils.*
import kotlinx.android.synthetic.main.content_main.*
import java.io.File
import java.io.FileOutputStream
import java.lang.System.currentTimeMillis
import java.util.*
import android.widget.AdapterView
import blog.photo.bildalbum.utils.ImagesAdapter
import android.view.View
import android.widget.GridView

class MainActivity() : BaseActivity(), FlickrDownloadData.OnFlickrDownloadComplete,
    FlickrJsonData.OnFlickrDataAvailable,
    PixabayDownloadData.OnPixabayDownloadComplete, PixabayJsonData.OnPixabayDataAvailable {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val gridView = findViewById<View>(R.id.gridview) as GridView
        var storedImagesPaths = getStoredImagesPaths()
        var imagesAdapter = ImagesAdapter(this, storedImagesPaths)
        gridView.adapter = imagesAdapter

        gridView.onItemClickListener = object : AdapterView.OnItemClickListener {
            override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
//                val image = getStoredImagesPaths().get(position)
                imagesAdapter.notifyDataSetChanged()
            }
        }

        buttonFlickrImagesDownload?.setOnClickListener {
            getFlickrImages()
            storedImagesPaths = getStoredImagesPaths()
            if(storedImagesPaths.size > 0)
                storedImagesPaths.add(storedImagesPaths.get(0));

            imagesAdapter = ImagesAdapter(this, storedImagesPaths)

            // Update the GridView
            imagesAdapter.notifyDataSetChanged();
            gridView.adapter = imagesAdapter
        }

        buttonPixabayImagesDownload?.setOnClickListener {
            getPixabayImages()
            storedImagesPaths = getStoredImagesPaths()
            if(storedImagesPaths.size > 0)
                storedImagesPaths.add(storedImagesPaths.get(0));

            imagesAdapter = ImagesAdapter(this, storedImagesPaths)

            // Update the GridView
            imagesAdapter.notifyDataSetChanged();
            gridView.adapter = imagesAdapter
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

    override fun onNetworkConnectionChanged(isConnected: Boolean) {
        buttonFlickrImagesDownload.isGone = !ConnectivityReceiver.isConnectedOrConnecting(this)
        buttonPixabayImagesDownload.isGone = !ConnectivityReceiver.isConnectedOrConnecting(this)
        noInternetConnection.isGone = ConnectivityReceiver.isConnectedOrConnecting(this)
    }

    inner class CreateImage(context: Context, var bmImage: ImageView, save: Boolean) :
        AsyncTask<String, Void, Bitmap>() {
        val context = context
        val save = save

        constructor(context: Context, bmImage: ImageView) : this(context, bmImage, true)

        override fun doInBackground(vararg urls: String): Bitmap? {
            val urlDisplay = urls[0]
            var bm: Bitmap? = null
            try {
                val `in` = java.net.URL(urlDisplay).openStream()
                bm = BitmapFactory.decodeStream(`in`)
            } catch (e: Exception) {
                Log.e("Error", e.message.toString())
                e.printStackTrace()
            }

            return bm
        }

        override fun onPostExecute(result: Bitmap) {
            if (save) {
                val path = storeImage(result)
                ImagesDBOpenHelper(context, null).addPhoto(Image(path))
            }
            bmImage.setImageBitmap(result)
        }

        private fun storeImage(finalBitmap: Bitmap): String {
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

            return file.absolutePath
        }
    }

    private fun getFlickrImages() {
        val uri = createFlickrUri(
            getString(R.string.FLICKR_API_URI),
            getString(R.string.FLICKR_API_TAGS),
            getString(R.string.FLICKR_API_LANG),
            true
        )
        FlickrDownloadData(this).execute(uri)
    }

    private fun getPixabayImages() {
        val uri = createPixabayUri(
            getString(R.string.PIXABAY_API_URI),
            getString(R.string.PIXABAY_API_KEY)
        )
        PixabayDownloadData(this).execute(uri)
    }

    private fun createFlickrUri(baseUri: String, tags: String, lang: String, matchAll: Boolean): String {
        return Uri.parse(baseUri).buildUpon().appendQueryParameter("tags", tags).appendQueryParameter("lang", lang)
            .appendQueryParameter("tagmode", if (matchAll) "ALL" else "ANY").appendQueryParameter("format", "json")
            .appendQueryParameter("nojsoncallback", "1")
            .build().toString()
    }

    private fun createPixabayUri(baseUri: String, key: String): String {
        return Uri.parse(baseUri).buildUpon().appendQueryParameter("key", key)
            .build().toString()
    }

    override fun onFlickrDownloadComplete(data: String, statusFlickr: FlickrDownloadStatus) {
        if (statusFlickr == FlickrDownloadStatus.OK)
            FlickrJsonData(this).execute(data)
    }

    override fun onPixabayDownloadComplete(data: String, statusPixabay: PixabayDownloadStatus) {
        if (statusPixabay == PixabayDownloadStatus.OK)
            PixabayJsonData(this).execute(data)
    }

    override fun onFlickrDataAvailable(data: ArrayList<FlickrImage>) {
        data.forEach {
            CreateImage(
                this,
                LayoutInflater.from(this).inflate(R.layout.image_layout, null).findViewById(R.id.picture)
            ).execute(it.image)
        }
    }

    override fun onPixabayDataAvailable(data: ArrayList<PixabayImage>) {
        data.forEach {
            CreateImage(
                this,
                LayoutInflater.from(this).inflate(R.layout.image_layout, null).findViewById(R.id.picture)
            ).execute(it.image)
        }
    }

    override fun onFlickrError(exception: Exception) {
        Toast.makeText(applicationContext, "Flickr Exception: $exception", Toast.LENGTH_SHORT).show()
    }

    override fun onPixabayError(exception: Exception) {
        Toast.makeText(applicationContext, "Pixabay Exception: $exception", Toast.LENGTH_SHORT).show()
    }

    fun getStoredImagesPaths(): ArrayList<String> {
        var listStoredImagesPaths = ArrayList<String>()
        val cursor = ImagesDBOpenHelper(this, null).getAllPhotosReverse()

        if (cursor!!.moveToFirst()) {
            listStoredImagesPaths.add(
                cursor.getString(
                    cursor.getColumnIndex(
                        ImagesDBOpenHelper.COLUMN_NAME
                    )
                )
            )
            while (cursor.moveToNext()) {
                listStoredImagesPaths.add(
                    cursor.getString(
                        cursor.getColumnIndex(
                            ImagesDBOpenHelper.COLUMN_NAME
                        )
                    )
                )
            }
        }
        cursor.close()

        return listStoredImagesPaths
    }
}
