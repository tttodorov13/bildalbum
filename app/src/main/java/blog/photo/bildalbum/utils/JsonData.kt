package blog.photo.bildalbum.utils

import android.os.AsyncTask
import android.util.Log
import blog.photo.bildalbum.model.Image
import org.json.JSONException
import org.json.JSONObject

/**
 * Class that manages the download of images' URIs.
 */
class JsonData(private val listener: OnDataAvailable, private val source: DownloadSource) : AsyncTask<String, Void, ArrayList<String>>() {
    private val TAG = "JsonData"

    /**
     * Interface for image uri download completed.
     */
    interface OnDataAvailable {
        fun onDataAvailable(data: ArrayList<String>)
        fun onError(exception: Exception)
    }

    /**
     * Method to override AsyncTask doInBackground.
     *
     * @param params
     * @return list of image URIs
     */
    override fun doInBackground(vararg params: String?): ArrayList<String> {
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
        return imagesUris
    }

    /**
     * Method to override AsyncTask onPostExecute.
     *
     * @param result
     */
    override fun onPostExecute(result: ArrayList<String>) {
        super.onPostExecute(result)
        listener.onDataAvailable(result)
    }
}