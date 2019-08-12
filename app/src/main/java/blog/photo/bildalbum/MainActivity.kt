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
import android.widget.ImageView
import android.widget.Toast
import blog.photo.bildalbum.model.FlickrImage
import blog.photo.bildalbum.model.Image
import blog.photo.bildalbum.receiver.ConnectivityReceiver
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

class MainActivity : BaseActivity(), FlickrDownloadData.OnDownloadComplete, FlickrJsonData.OnDataAvailable {
    private val TAG = "MainActivityFlickr"
    private var shareDialog: ShareDialog? = null
    private val limitDownloadPictures: Int? = 5

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        for(i in getStoredImagesPaths())
            displayImage(i)

        Log.d(TAG, "onCreate called")

        Toast.makeText(applicationContext, R.string.logging_in, Toast.LENGTH_SHORT).show()

        shareDialog = ShareDialog(this)

        fab.setOnClickListener {
            shareDialog?.show(ShareLinkContent.Builder().build())
        }

        val inBundle = intent.extras
        val name = inBundle!!.get("name")!!.toString()
        val surname = inBundle.get("surname")!!.toString()
        val imageUrl = inBundle.get("imageUrl")!!.toString()

        nameAndSurname.text = "$name $surname"

        if (ConnectivityReceiver.isConnectedOrConnecting(this)) {
            CreateImage(this, profileImage, false).execute(imageUrl)
        }

        buttonLogoutFacebook?.setOnClickListener {
            facebookLogout()
        }

        buttonFacebookPicturesDownload?.setOnClickListener {
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

    override fun onNetworkConnectionChanged(isConnected: Boolean) {
        if (!ConnectivityReceiver.isConnectedOrConnecting(this)) {
            facebookLogout()
        }
    }

    fun facebookLogout() {
        LoginManager.getInstance().logOut()
        val login = Intent(this@MainActivity, LoginActivity::class.java)
        startActivity(login)
        finish()
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
                val path = storeImage(result)
                displayImage(path)
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

    private fun getFacebookPictures() {
        val callback: GraphRequest.Callback = GraphRequest.Callback { response ->
            var facebookImagesPaths = mutableListOf<String>()
            val data = response.jsonObject.getJSONArray("data")

            for (i in 0 until data.length()) {
                facebookImagesPaths.add(
                    JSONObject(
                        data.get(
                            i
                        ).toString()
                    ).get("picture").toString()
                )
                CreateImage(this, LayoutInflater.from(this).inflate(R.layout.picture_layout, null).findViewById(R.id.picture)).execute(facebookImagesPaths.get(i))
            }
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
        FlickrDownloadData(this).execute(uri)
        Log.d(TAG, "onCreate ended")
    }

    private fun createFlickrUri(baseUri: String, tags: String, lang: String, matchAll: Boolean): String {
        Log.d(TAG, "createFlickrUri starts")

        return Uri.parse(baseUri).buildUpon().appendQueryParameter("tags", tags).appendQueryParameter("lang", lang)
            .appendQueryParameter("tagmode", if (matchAll) "ALL" else "ANY").appendQueryParameter("format", "json")
            .appendQueryParameter("nojsoncallback", "1")
            .build().toString()
    }

    override fun onDownloadComplete(data: String, statusFlickr: FlickrDownloadStatus) {
        Log.d(TAG, "Flickr Integration onDownloadComplete, statusFlickr: $statusFlickr")
        if (statusFlickr == FlickrDownloadStatus.OK)
            FlickrJsonData(this).execute(data)
    }

    override fun onDataAvailable(data: ArrayList<FlickrImage>) {
        Log.d(TAG, "Flickr Integration onDataAvailable starts")
        data.forEach {
            CreateImage(
                this,
                LayoutInflater.from(this).inflate(R.layout.picture_layout, null).findViewById(R.id.picture)
            ).execute(it.image)
        }
        Log.d(TAG, "Flickr Integration onDataAvailable ends")
    }

    override fun onError(exception: Exception) {
        Log.d(TAG, "Flickr Integration onError starts with exception $exception")
    }
}
