package blog.photo.bildalbum.utils

import android.os.AsyncTask
import android.util.Log
import java.io.IOException
import java.net.MalformedURLException
import java.net.URL

enum class FlickrDownloadStatus {
    OK, IDLE, NOT_INITIALIZED, FAILED_OR_EMPTY, PERMISSIONS_ERROR, ERROR
}

class FlickrDownloadData(private val listener: OnDownloadComplete) : AsyncTask<String, Void, String>() {
    private val TAG = "FlickrDownloadData"
    private var status = FlickrDownloadStatus.IDLE

    interface OnDownloadComplete {
        fun onDownloadComplete(data: String, statusFlickr: FlickrDownloadStatus)
    }

    override fun onPostExecute(result: String) {
        Log.d(TAG, "onPostExecute called")
        listener.onDownloadComplete(result, status)
    }

    override fun doInBackground(vararg params: String?): String {
        if (params[0] == null) {
            status = FlickrDownloadStatus.NOT_INITIALIZED
            return "No URL specified"
        }

        try {
            status = FlickrDownloadStatus.OK
            return URL(params[0]).readText()
        } catch (e: Exception) {
            val errorMessage = when (e) {
                is MalformedURLException -> {
                    status = FlickrDownloadStatus.NOT_INITIALIZED
                    "doInBackground: Invalid URL: ${e.message}"
                }
                is IOException -> {
                    status = FlickrDownloadStatus.FAILED_OR_EMPTY
                    "doInBackground: IO Exception reading data: ${e.message}"
                }
                is SecurityException -> {
                    status = FlickrDownloadStatus.PERMISSIONS_ERROR
                    "doInBackground: Security exception: ${e.message}"
                }
                else -> {
                    status = FlickrDownloadStatus.ERROR
                    "doInBackground: Unknown exception: $e"
                }
            }
            Log.e(TAG, errorMessage)
            return errorMessage
        }
    }
}