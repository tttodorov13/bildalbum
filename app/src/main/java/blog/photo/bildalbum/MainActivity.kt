package blog.photo.bildalbum

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.facebook.AccessToken
import com.facebook.GraphRequest
import com.facebook.login.LoginManager
import com.facebook.share.model.ShareLinkContent
import com.facebook.share.widget.ShareDialog
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private var shareDialog: ShareDialog? = null
    private val countDownloadPictures: kotlin.Int? = 5

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(toolbar)

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
        logout?.setOnClickListener {
            println(logout)
            LoginManager.getInstance().logOut()
            val login = Intent(this@MainActivity, LoginActivity::class.java)
            startActivity(login)
            finish()
        }
        MainActivity().DownloadImage(profileImage).execute(imageUrl)

        getFacebookPictures()
    }

    inner class DownloadImage(internal var bmImage: ImageView) : AsyncTask<String, Void, Bitmap>() {

        override fun doInBackground(vararg urls: String): Bitmap? {
            val urldisplay = urls[0]
            var mIcon11: Bitmap? = null
            try {
                val `in` = java.net.URL(urldisplay).openStream()
                mIcon11 = BitmapFactory.decodeStream(`in`)
            } catch (e: Exception) {
                Log.e("Error", e.message)
                e.printStackTrace()
            }

            return mIcon11
        }

        override fun onPostExecute(result: Bitmap) {
            bmImage.setImageBitmap(result)
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

    private fun downloadPictures(listPictures: MutableList<String>) {
        for (i in 0 until listPictures.size) {
            var view = LayoutInflater.from(this).inflate(R.layout.picture_layout, null)
            var imageView = view.findViewById<ImageView>(R.id.picture)
            imageView.id = imageView.id + i
            MainActivity().DownloadImage(imageView).execute(listPictures.get(i))
            pictures.addView(view)
        }
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
            }

            downloadPictures(listPictures)
        }

        val request = GraphRequest.newGraphPathRequest(
            AccessToken.getCurrentAccessToken(),
            "/me/photos",
            callback
        );

        val parameters = Bundle()
        parameters.putString("fields", "picture")
        parameters.putString("limit", countDownloadPictures.toString())
        request.version = "v4.0"
        request.parameters = parameters
        request.executeAsync()
    }
}
