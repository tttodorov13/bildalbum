/*
 * Copyright (c) 2018-2020 SAP and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 * Contributors:
 * SAP - initial API and implementation
 */
package blog.photo.albumbuild.async

import android.os.AsyncTask
import timber.log.Timber
import java.io.IOException
import java.net.MalformedURLException
import java.net.URL

/**
 * Enum for download sources.
 */
enum class ImageSource {
    ALBUM_BUILD, CAMERA, CANVASES, STORAGE, PIXABAY
}

/**
 * Enum for download statuses.
 */
enum class DownloadStatus {
    OK, IDLE, NOT_INITIALIZED, FAILED_OR_EMPTY, PERMISSIONS_ERROR, ERROR
}

/**
 * Manage picture download.
 */
internal class DownloadData(
    private val listener: OnDownloadComplete,
    private val source: ImageSource
) :
    AsyncTask<String, Void, String>() {
    private var status = DownloadStatus.IDLE

    /**
     * Interface for picture download completed.
     */
    interface OnDownloadComplete {
        fun onDownloadComplete(data: String, source: ImageSource, status: DownloadStatus)
    }

    /**
     * Override AsyncTask doInBackground to return proper error message.
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
                    status =
                        DownloadStatus.NOT_INITIALIZED
                    "doInBackground: Invalid URL: ${e.message}"
                }
                is IOException -> {
                    status =
                        DownloadStatus.FAILED_OR_EMPTY
                    "doInBackground: IO Exception reading data: ${e.message}"
                }
                is SecurityException -> {
                    status =
                        DownloadStatus.PERMISSIONS_ERROR
                    "doInBackground: Security exception: ${e.message}"
                }
                else -> {
                    status = DownloadStatus.ERROR
                    "doInBackground: Unknown exception: $e"
                }
            }
            Timber.e(errorMessage)
            return errorMessage
        }
        return uri.toString()
    }

    /**
     * Override AsyncTask onPostExecute.
     *
     * @param result
     */
    override fun onPostExecute(result: String) {
        listener.onDownloadComplete(result, source, status)
    }
}