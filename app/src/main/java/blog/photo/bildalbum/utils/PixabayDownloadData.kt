package blog.photo.bildalbum.utils

import android.os.AsyncTask
import android.util.Log
import java.io.IOException
import java.net.MalformedURLException
import java.net.URL

enum class PixabayDownloadStatus {
    OK, IDLE, NOT_INITIALIZED, FAILED_OR_EMPTY, PERMISSIONS_ERROR, ERROR
}

class PixabayDownloadData(private val listener: OnPixabayDownloadComplete) : AsyncTask<String, Void, String>() {
    private val TAG = "PixabayDownloadData"
    private var status = PixabayDownloadStatus.IDLE

    interface OnPixabayDownloadComplete {
        fun onPixabayDownloadComplete(data: String, statusFlickr: PixabayDownloadStatus)
    }

    override fun onPostExecute(result: String) {
        Log.d(TAG, "onPostExecute called")
        listener.onPixabayDownloadComplete(result, status)
    }

    override fun doInBackground(vararg params: String?): String {
        if (params[0] == null) {
            status = PixabayDownloadStatus.NOT_INITIALIZED
            return "No URL specified"
        }

        try {
            status = PixabayDownloadStatus.OK
            return URL(params[0]).readText()
        } catch (e: Exception) {
            val errorMessage = when (e) {
                is MalformedURLException -> {
                    status = PixabayDownloadStatus.NOT_INITIALIZED
                    "doInBackground: Invalid URL: ${e.message}"
                }
                is IOException -> {
                    status = PixabayDownloadStatus.FAILED_OR_EMPTY
                    "doInBackground: IO Exception reading data: ${e.message}"
                }
                is SecurityException -> {
                    status = PixabayDownloadStatus.PERMISSIONS_ERROR
                    "doInBackground: Security exception: ${e.message}"
                }
                else -> {
                    status = PixabayDownloadStatus.ERROR
                    "doInBackground: Unknown exception: $e"
                }
            }
            Log.e(TAG, errorMessage)
            return errorMessage
        }
    }
}