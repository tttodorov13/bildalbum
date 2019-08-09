package blog.photo.bildalbum

import android.content.Context
import android.content.Intent
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
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import blog.photo.bildalbum.model.FlickrPhoto
import blog.photo.bildalbum.model.Image
import blog.photo.bildalbum.utils.*
import com.facebook.AccessToken
import com.facebook.GraphRequest
import com.facebook.login.LoginManager
import com.facebook.share.model.ShareLinkContent
import com.facebook.share.widget.ShareDialog
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.lang.System.currentTimeMillis
import java.util.*

class MainActivity : AppCompatActivity(), DownloadData.OnDownloadComplete, GetFlickrJsonData.OnDataAvailable {
    private val TAG = "MainActivityFlickr"
    private val flickrRVAdapter = FlickrRecyclerViewAdapter(ArrayList())
    private var shareDialog: ShareDialog? = null
    private val limitDownloadPictures: Int? = 5

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        displayImages(getStoredImagesPaths())

        Log.d(TAG, "onCreate called")

        Toast.makeText(applicationContext, R.string.logging_in, Toast.LENGTH_SHORT).show()

        shareDialog = ShareDialog(this)

        fab.setOnClickListener {
            val content = ShareLinkContent.Builder().build()
            shareDialog?.show(content)
        }

        val inBundle = intent.extras
        val name = inBundle!!.get("name")!!.toString()
        val surname = inBundle.get("surname")!!.toString()
        val imageUrl = inBundle.get("imageUrl")!!.toString()

        nameAndSurname.text = "$name $surname"

//        CreateImage(this, profileImage, false).execute(imageUrl)

        buttonLogoutFacebook?.setOnClickListener {
            LoginManager.getInstance().logOut()
            val login = Intent(this@MainActivity, LoginActivity::class.java)
            startActivity(login)
            finish()
        }

        buttonFacebookPictureDownload?.setOnClickListener {
            getFacebookPictures()
        }

        buttonFlickrPicturesDownload?.setOnClickListener {
            getFlickrPictures()
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

    inner class CreateImage(context: Context, var bmImage: ImageView, save: Boolean) :
        AsyncTask<String, Void, Bitmap>() {
        val context = context
        val save = save

        constructor(context: Context, bmImage: ImageView) : this(context, bmImage, true)

        override fun doInBackground(vararg urls: String): Bitmap? {
            val urldisplay = urls[0]
            var bm: Bitmap? = null
            try {
                val `in` = java.net.URL(urldisplay).openStream()
                bm = BitmapFactory.decodeStream(`in`)
            } catch (e: Exception) {
                Log.e("Error", e.message.toString())
                e.printStackTrace()
            }

            return bm
        }

        override fun onPostExecute(result: Bitmap) {
            if (save) {
                val path = storeImage(result);
                val photo = Image(path)
                val dbHandler = PhotosDBOpenHelper(context, null)
                dbHandler.addPhoto(photo)
            }
            bmImage.setImageBitmap(result)
        }

        private fun storeImage(finalBitmap: Bitmap): String {
            val storageDir = File(
                Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES
                ), "bildalbum"
            )

            if (!storageDir.exists()) {
                storageDir.mkdirs();
            }

            val n = System.currentTimeMillis()
            val fileName = "pic$n.jpg"
            val file = File(storageDir, fileName)
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

    private fun createFlickrUri(baseUri: String, tags: String, lang: String, matchAll: Boolean): String {
        Log.d(TAG, "createFlickrUri starts")

        return Uri.parse(baseUri).buildUpon().appendQueryParameter("tags", tags).appendQueryParameter("lang", lang)
            .appendQueryParameter("tagmode", if (matchAll) "ALL" else "ANY").appendQueryParameter("format", "json")
            .appendQueryParameter("nojsoncallback", "1")
            .build().toString()
    }

    private fun displayImages(imagesPaths: MutableList<String>) {
        var view = LayoutInflater.from(this).inflate(R.layout.picture_layout, null)
        var imageView = view.findViewById<ImageView>(R.id.picture)

        for (imagePath in imagesPaths) {
            imageView.id = currentTimeMillis().toInt()
            imageView.setImageBitmap(BitmapFactory.decodeFile(imagePath))
            if (imageView.parent != null) {
                (imageView.parent as ViewGroup).removeView(imageView)
            }
            pictures.addView(imageView)
        }
    }

    private fun getStoredImagesPaths(): MutableList<String> {
        var listStoredImagesPaths = mutableListOf<String>()

        val dbHandler = PhotosDBOpenHelper(this, null)
        val cursor = dbHandler.getAllPhotos()
        if (cursor!!.moveToFirst()) {
            listStoredImagesPaths.add(
                cursor.getString(
                    cursor.getColumnIndex(
                        PhotosDBOpenHelper.COLUMN_NAME
                    )
                )
            )
            while (cursor.moveToNext()) {
                listStoredImagesPaths.add(
                    cursor.getString(
                        cursor.getColumnIndex(
                            PhotosDBOpenHelper.COLUMN_NAME
                        )
                    )
                )
            }
        }
        cursor.close()

        return listStoredImagesPaths
    }

    private fun getFacebookPictures() {
        val callback: GraphRequest.Callback = GraphRequest.Callback { response ->
            var listPictures = mutableListOf<String>()
            val data = response.jsonObject.getJSONArray("data")

            for (i in 0 until data.length()) {
                listPictures.add(
                    JSONObject(
                        data.get(
                            i
                        ).toString()
                    ).get("picture").toString()
                )

                val view = LayoutInflater.from(this).inflate(R.layout.picture_layout, null)
                val imageView = view.findViewById<ImageView>(R.id.picture)
                CreateImage(this, imageView).execute(listPictures.get(i))
            }

            displayImages(listPictures)
        }

        val request = GraphRequest.newGraphPathRequest(
            AccessToken.getCurrentAccessToken(),
            "/me/photos",
            callback
        )

        val parameters = Bundle()
        parameters.putString("fields", "picture")
        parameters.putString("limit", limitDownloadPictures.toString())
        request.version = "v4.0"
        request.parameters = parameters
        request.executeAsync()
    }

    private fun getFlickrPictures() {
        val uri = createFlickrUri(
            getString(R.string.FLICKR_API_URI),
            getString(R.string.FLICKR_API_TAGS),
            getString(R.string.FLICKR_API_LANG),
            true
        )
        val downloadData = DownloadData(this)
        downloadData.execute(uri)
        Log.d(TAG, "onCreate ended")
    }

    override fun onDownloadComplete(data: String, status: DownloadStatus) {
        Log.d(TAG, "Flickr Integration onDownloadComplete, status: $status")
        if (status == DownloadStatus.OK) {
            val parser = GetFlickrJsonData(this)
            parser.execute(data)
        }
    }

    override fun onDataAvailable(data: ArrayList<FlickrPhoto>) {
        Log.d(TAG, "Flickr Integration onDataAvailable starts")
        data.forEach {
            var view = LayoutInflater.from(this).inflate(R.layout.picture_layout, null)
            var imageView = view.findViewById<ImageView>(R.id.picture)
            CreateImage(this, imageView).execute(it.image)
            pictures.addView(view)
        }

        flickrRVAdapter.loadNewData(data)
        Log.d(TAG, "Flickr Integration onDataAvailable ends")
    }

    override fun onError(exception: Exception) {
        Log.d(TAG, "Flickr Integration onError starts with exception $exception")
    }
}
