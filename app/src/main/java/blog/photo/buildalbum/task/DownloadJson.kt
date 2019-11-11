package blog.photo.buildalbum.task

import android.os.AsyncTask
import android.util.Log
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Class to manage cards' URIs download.
 */
internal class DownloadJson(private val listener: OnDataAvailable, private val source: DownloadSource) :
    AsyncTask<String, Void, ArrayList<String>>() {
    private val tag = "JsonData"

    /**
     * Interface for card source download completed.
     */
    interface OnDataAvailable {
        fun onDataAvailable(data: ArrayList<String>)
        fun onError(exception: Exception)
    }

    /**
     * Method to override AsyncTask doInBackground.
     *
     * @param params
     * @return list of cards' URIs
     */
    override fun doInBackground(vararg params: String): ArrayList<String> {
        val cardsURIs = ArrayList<String>()
        val jsonData: JSONObject
        val itemsArray: JSONArray

        try {
            jsonData = JSONObject(params[0])
            when (source) {
                DownloadSource.FRAMES -> {
                    itemsArray = jsonData.getJSONArray("items")
                    for (i in 0 until itemsArray.length()) {
                        cardsURIs.add(
                            itemsArray.getJSONObject(i).getJSONObject("media").getString(
                                "m"
                            )
                        )
                    }
                }
                DownloadSource.FLICKR -> {
                    itemsArray = jsonData.getJSONArray("items")
                    for (i in 0 until itemsArray.length()) {
                        cardsURIs.add(
                            itemsArray.getJSONObject(i).getJSONObject("media").getString(
                                "m"
                            )
                        )
                    }
                }
                DownloadSource.PIXABAY -> {
                    itemsArray = jsonData.getJSONArray("hits")
                    for (i in 0 until itemsArray.length()) {
                        cardsURIs.add(itemsArray.getJSONObject(i).getString("previewURL"))
                    }
                }
            }
        } catch (e: JSONException) {
            Log.e(tag, "doInBackground: JSON processing exception", e)
            cancel(true)
            listener.onError(e)
        }
        return cardsURIs
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