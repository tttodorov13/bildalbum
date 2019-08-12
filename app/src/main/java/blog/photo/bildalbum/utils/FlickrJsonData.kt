package blog.photo.bildalbum.utils

import android.os.AsyncTask
import android.util.Log
import blog.photo.bildalbum.model.FlickrImage
import org.json.JSONException
import org.json.JSONObject

class FlickrJsonData(private val listener: OnDataAvailable) : AsyncTask<String, Void, ArrayList<FlickrImage>>() {

    private val TAG = "FlickrJsonData"

    interface OnDataAvailable {
        fun onDataAvailable(data: ArrayList<FlickrImage>)
        fun onError(exception: Exception)
    }

    override fun doInBackground(vararg params: String?): ArrayList<FlickrImage> {
        Log.d(TAG, "doInBackground starts")
        val photos = ArrayList<FlickrImage>()

        try {
            val jsonData = JSONObject(params[0].toString())
            val itemsArray = jsonData.getJSONArray("items")
            for (i in 0 until itemsArray.length()) {
                val jsonObject = itemsArray.getJSONObject(i)
                val title = jsonObject.getString("title")
                val author = jsonObject.getString("author")
                val authorId = jsonObject.getString("author_id")
                val tags = jsonObject.getString("tags")
                val link = jsonObject.getString("link").replaceFirst("_m.jpg", "_b.jpg")

                val jsonMedia = jsonObject.getJSONObject("media")
                val photoUrl = jsonMedia.getString("m")

                val photo = FlickrImage(title, author, authorId, link, tags, photoUrl)
                photos.add(photo)
                Log.d(TAG, "doInBackground: $photo")
            }
        } catch (e: JSONException) {
            Log.e(TAG, "doInBackground: JSON processing exception", e)
            cancel(true)
            listener.onError(e)
        }
        Log.d(TAG, "doInBackground ends")

        return photos
    }

    override fun onPostExecute(result: ArrayList<FlickrImage>) {
        Log.d(TAG, "onPostExecute starts")
        super.onPostExecute(result)
        listener.onDataAvailable(result)
        Log.d(TAG, "onPostExecute ends")
    }

}