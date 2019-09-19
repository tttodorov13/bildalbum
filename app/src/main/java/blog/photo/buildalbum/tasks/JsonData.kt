package blog.photo.buildalbum.tasks

import android.os.AsyncTask
import android.util.Log
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Class that manages the download of imagesNames' URIs.
 */
class JsonData(private val listener: OnDataAvailable, private val source: DownloadSource) :
    AsyncTask<String, Void, ArrayList<String>>() {
    private val tag = "JsonData"

    /**
     * Interface for picture origin download completed.
     */
    interface OnDataAvailable {
        fun onDataAvailable(data: ArrayList<String>)
        fun onError(exception: Exception)
    }

    /**
     * Method to override AsyncTask doInBackground.
     *
     * @param params
     * @return list of picture URIs
     */
    override fun doInBackground(vararg params: String?): ArrayList<String> {
        val imagesUris = ArrayList<String>()
        val jsonData: JSONObject
        val itemsArray: JSONArray

        try {
            jsonData = JSONObject(params[0].toString())
            when (source) {
                DownloadSource.FRAMES -> {
                    itemsArray = jsonData.getJSONArray("items")
                    for (i in 0 until itemsArray.length()) {
                        imagesUris.add(
                            itemsArray.getJSONObject(i).getJSONObject("media").getString(
                                "m"
                            )
                        )
                    }
                }
                DownloadSource.FLICKR -> {
                    itemsArray = jsonData.getJSONArray("items")
                    for (i in 0 until itemsArray.length()) {
                        imagesUris.add(
                            itemsArray.getJSONObject(i).getJSONObject("media").getString(
                                "m"
                            )
                        )
                    }
                }
                DownloadSource.PIXABAY -> {
                    itemsArray = jsonData.getJSONArray("hits")
                    for (i in 0 until itemsArray.length()) {
                        imagesUris.add(itemsArray.getJSONObject(i).getString("previewURL"))
                    }
                }
            }
        } catch (e: JSONException) {
            Log.e(tag, "doInBackground: JSON processing exception", e)
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