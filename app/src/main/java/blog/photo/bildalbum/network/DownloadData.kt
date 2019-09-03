package blog.photo.bildalbum.network

import android.os.AsyncTask
import android.util.Log
import java.io.IOException
import java.net.MalformedURLException
import java.net.URL

/**
 * Enum for image's download sources.
 */
enum class DownloadSource {
    FLICKR, PIXABAY, FRAMES
}

/**
 * Enum for image's download statuses.
 */
enum class DownloadStatus {
    OK, IDLE, NOT_INITIALIZED, FAILED_OR_EMPTY, NETWORK_ERROR, PERMISSIONS_ERROR, ERROR
}

/**
 * Class that manages the download of imagesNames.
 */
class DownloadData(private val listener: OnDownloadComplete, private val source: DownloadSource) :
    AsyncTask<String, Void, String>() {
    private val tag = "DownloadData"
    private var status = DownloadStatus.IDLE

    /**
     * Interface for image download completed.
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
            if (source == DownloadSource.FRAMES)
                uri.append(URL(params[0]).toString())
            else
                uri.append(URL(params[0]).readText())
        } catch (e: Exception) {
            val errorMessage = when (e) {
                is MalformedURLException -> {
                    status = DownloadStatus.NOT_INITIALIZED
                    "doInBackground: Invalid URL: ${e.message}"
                }
                is IOException -> {
                    status = DownloadStatus.FAILED_OR_EMPTY
                    if (e.message.toString().startsWith("Unable to resolve host"))
                        status = DownloadStatus.NETWORK_ERROR
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