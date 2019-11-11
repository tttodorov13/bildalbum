package blog.photo.buildalbum.task

import android.os.AsyncTask
import android.util.Log
import java.io.IOException
import java.net.MalformedURLException
import java.net.URL

/**
 * Enum for card's download sources.
 */
enum class DownloadSource {
    FLICKR, PIXABAY, FRAMES
}

/**
 * Enum for card's download statuses.
 */
enum class DownloadStatus {
    OK, IDLE, NOT_INITIALIZED, FAILED_OR_EMPTY, PERMISSIONS_ERROR, ERROR
}

/**
 * Class to manage cards' download.
 */
internal class DownloadData(private val listener: OnDownloadComplete, private val source: DownloadSource) :
    AsyncTask<String, Void, String>() {
    private val tag = "DownloadData"
    private var status = DownloadStatus.IDLE

    /**
     * Interface for picture download completed.
     */
    interface OnDownloadComplete {
        fun onDownloadComplete(data: String, source: DownloadSource, status: DownloadStatus)
    }

    /**
     * Method to override AsyncTask doInBackground.
     *
     * @param params
     * @return result or appropriate error message
     */
    override fun doInBackground(vararg params: String?): String {
        if (params[0] == null) {
            status = DownloadStatus.NOT_INITIALIZED
            return "No URL specified"
        }

        var uri = StringBuilder()
        try {
            status = DownloadStatus.OK
            uri.append(URL(params[0]).readText())
        } catch (e: Exception) {
            val errorMessage = when (e) {
                is MalformedURLException -> {
                    status = DownloadStatus.NOT_INITIALIZED
                    "doInBackground: Invalid URL: ${e.message}"
                }
                is IOException -> {
                    status = DownloadStatus.FAILED_OR_EMPTY
                    "doInBackground: IO Exception reading data: ${e.message}"
                }
                is SecurityException -> {
                    status = DownloadStatus.PERMISSIONS_ERROR
                    "doInBackground: Security exception: ${e.message}"
                }
                else -> {
                    status = DownloadStatus.ERROR
                    "doInBackground: Unknown exception: $e"
                }
            }
            Log.e(tag, errorMessage)
            return errorMessage
        }
        return uri.toString()
    }

    /**
     * Method to override AsyncTask onPostExecute.
     *
     * @param result
     */
    override fun onPostExecute(result: String) {
        listener.onDownloadComplete(result, source, status)
    }
}