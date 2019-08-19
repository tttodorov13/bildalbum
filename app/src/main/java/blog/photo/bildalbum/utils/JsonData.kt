package blog.photo.bildalbum.utils

import android.os.AsyncTask
import android.util.Log
import blog.photo.bildalbum.model.Image
import org.json.JSONException
import org.json.JSONObject

class JsonData(private val listener: OnDataAvailable, private val source: DownloadSource) : AsyncTask<String, Void, ArrayList<String>>() {
    private val TAG = "JsonData"

    interface OnDataAvailable {
        fun onDataAvailable(data: ArrayList<String>)
        fun onError(exception: Exception)
    }

    override fun doInBackground(vararg params: String?): ArrayList<String> {
        Log.d(TAG, "doInBackground starts")
        val imagesUris = ArrayList<String>()

        try {
            val jsonData = JSONObject(params[0].toString())
            when (source) {
                DownloadSource.FLICKR -> {
                    val itemsArray = jsonData.getJSONArray("items")
                    for (i in 0 until itemsArray.length()) {
                        imagesUris.add(itemsArray.getJSONObject(i).getJSONObject("media").getString("m"))
                        Log.d(TAG, "doInBackground: " + imagesUris[i])
                    }
                }
                DownloadSource.PIXABAY -> {
                    val itemsArray = jsonData.getJSONArray("hits")
                    for (i in 0 until itemsArray.length()) {
                        imagesUris.add(itemsArray.getJSONObject(i).getString("previewURL"))
                        Log.d(TAG, "doInBackground: " + imagesUris[i])
                    }
                }
            }
        } catch (e: JSONException) {
            Log.e(TAG, "doInBackground: JSON processing exception", e)
            cancel(true)
            listener.onError(e)
        }
        Log.d(TAG, "doInBackground ends")
        return imagesUris
    }

    override fun onPostExecute(result: ArrayList<String>) {
        Log.d(TAG, "onPostExecute starts")
        super.onPostExecute(result)
        listener.onDataAvailable(result)
        Log.d(TAG, "onPostExecute ends")
    }
}