/*
 * Copyright (c) 2018-2020 SAP and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 * Contributors:
 * SAP - initial API and implementation
 */
package blog.photo.buildalbum.async

import android.os.AsyncTask
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber

/**
 * Manage URI download.
 */
internal class DownloadJson(
    private val listener: OnDataAvailable,
    private val source: ImageSource
) :
    AsyncTask<String, Void, ArrayList<String>>() {
    private val tag = "JsonData"

    /**
     * Picture source download completed.
     */
    interface OnDataAvailable {
        fun onDataAvailable(data: ArrayList<String>)
        fun onError(exception: Exception)
    }

    /**
     * Override AsyncTask doInBackground to download from different sources.
     *
     * @param params
     * @return list of URIs
     */
    override fun doInBackground(vararg params: String): ArrayList<String> {
        val uriList = ArrayList<String>()
        uriList.add(source.name)
        val jsonData: JSONObject
        val itemsArray: JSONArray

        if (params.isEmpty()) {
            Timber.e("doInBackground: JSON processing exception")
            cancel(true)
            listener.onError(ArrayIndexOutOfBoundsException(1))
            return uriList
        }

        try {
            jsonData = JSONObject(params[0])
            when (source) {
                ImageSource.ALBUM_BUILD -> {
                    itemsArray = jsonData.getJSONArray("items")
                    for (i in 0 until itemsArray.length()) {
                        uriList.add(
                            itemsArray.getJSONObject(i).getJSONObject("media").getString(
                                "m"
                            )
                        )
                    }
                }
                ImageSource.CANVASES -> {
                    itemsArray = jsonData.getJSONArray("items")
                    for (i in 0 until itemsArray.length()) {
                        uriList.add(
                            itemsArray.getJSONObject(i).getJSONObject("media").getString(
                                "m"
                            )
                        )
                    }
                }
                ImageSource.PIXABAY -> {
                    itemsArray = jsonData.getJSONArray("hits")
                    for (i in 0 until itemsArray.length()) {
                        uriList.add(itemsArray.getJSONObject(i).getString("previewURL"))
                    }
                }
                else -> {
                    // TODO Implement other download sources
                }
            }
        } catch (e: JSONException) {
            Timber.e("doInBackground: JSON processing exception")
            cancel(true)
            listener.onError(e)
        }
        return uriList
    }

    /**
     * Override AsyncTask onPostExecute.
     *
     * @param result
     */
    override fun onPostExecute(result: ArrayList<String>) {
        super.onPostExecute(result)
        listener.onDataAvailable(result)
    }
}