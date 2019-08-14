package blog.photo.bildalbum.utils

import android.os.AsyncTask
import android.util.Log
import blog.photo.bildalbum.model.FlickrImage
import blog.photo.bildalbum.model.PixabayImage
import org.json.JSONException
import org.json.JSONObject

class PixabayJsonData(private val listener: OnPixabayDataAvailable) : AsyncTask<String, Void, ArrayList<PixabayImage>>() {

    private val TAG = "PixabayJsonData"

    interface OnPixabayDataAvailable {
        fun onPixabayDataAvailable(data: ArrayList<PixabayImage>)
        fun onPixabayError(exception: Exception)
    }

    override fun doInBackground(vararg params: String?): ArrayList<PixabayImage> {
        Log.d(TAG, "doInBackground starts")
        val images = ArrayList<PixabayImage>()

        try {
            val jsonData = JSONObject(params[0].toString())
            val itemsArray = jsonData.getJSONArray("hits")
            for (i in 0 until itemsArray.length()) {
                val jsonObject = itemsArray.getJSONObject(i)
                val title = jsonObject.getString("pageURL")
                val author = jsonObject.getString("user")
                val authorId = jsonObject.getString("user_id")
                val tags = jsonObject.getString("tags")
                val link = jsonObject.getString("webformatURL")
                val photoUrl = jsonObject.getString("previewURL")

                val photo = PixabayImage(title, author, authorId, link, tags, photoUrl)
                images.add(photo)
                Log.d(TAG, "doInBackground: $photo")
            }
        } catch (e: JSONException) {
            Log.e(TAG, "doInBackground: JSON processing exception", e)
            cancel(true)
            listener.onPixabayError(e)
        }
        Log.d(TAG, "doInBackground ends")

        return images
    }

    override fun onPostExecute(result: ArrayList<PixabayImage>) {
        Log.d(TAG, "onPostExecute starts")
        super.onPostExecute(result)
        listener.onPixabayDataAvailable(result)
        Log.d(TAG, "onPostExecute ends")
    }

}