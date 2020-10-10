/*
 * Copyright (c) 2018-2020 SAP and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 * Contributors:
 * SAP - initial API and implementation
 */
package blog.photo.albumbuild.launch

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import blog.photo.albumbuild.R
import blog.photo.albumbuild.async.DownloadData
import blog.photo.albumbuild.async.DownloadJson
import blog.photo.albumbuild.async.ImageSource
import blog.photo.albumbuild.async.DownloadStatus
import blog.photo.albumbuild.data.Image
import blog.photo.albumbuild.databinding.LaunchFragBinding
import blog.photo.albumbuild.images.ImagesActivity
import blog.photo.albumbuild.util.*
import timber.log.Timber
import java.io.File
import kotlin.concurrent.thread

/**
 * Display App Logo.
 */
class LaunchFragment : Fragment(),
    DownloadData.OnDownloadComplete,
    DownloadJson.OnDataAvailable {

    private val viewModel by viewModels<LaunchViewModel> { getLaunchViewModelFactory() }

    private lateinit var listAdapterImages: ImagesAdapter

    private lateinit var listAdapterCanvases: CanvasesAdapter

    private lateinit var viewDataBinding: LaunchFragBinding

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        // Set the lifecycle owner to the lifecycle of the view
        viewDataBinding.lifecycleOwner = this.viewLifecycleOwner

        setupListAdapters()
        setupRefreshLayout(viewDataBinding.launchRefreshLayout, viewDataBinding.launchListImages)

        thread {

            val res = requireActivity().resources

            hasInternet().subscribe { hasInternet ->
                if (hasInternet) {

                    // Check for new images available if no images exist
                    download(
                        this,
                        ImageSource.ALBUM_BUILD,
                        res.getString(R.string.URI_API_DEFAULT_IMAGES)
                    )

                    // Check for new canvases available if no canvases exist
                    download(
                        this,
                        ImageSource.CANVASES,
                        res.getString(R.string.URI_API_DEFAULT_CANVASES)
                    )
                }
            }

            Thread.sleep((3 * 1000).toLong())
            startImagesActivity()
        }.priority = Thread.MAX_PRIORITY
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewDataBinding = LaunchFragBinding.inflate(inflater, container, false).apply {
            viewmodel = viewModel
        }

        return viewDataBinding.root
    }

    /**
     * Download URIs.
     *
     * @param data - URIs
     */
    override fun onDataAvailable(data: ArrayList<String>) {
        val list = ArrayList<Image>()

        // Check if valid image sources list
        if (data.isNotEmpty()) {
            // Do not download default images if some exists
            if (data.first() == ImageSource.ALBUM_BUILD.name && viewModel.hasImages())
                return

            // Do not download default canvases if some exists
            if (data.first() == ImageSource.CANVASES.name && viewModel.hasCanvases())
                return

            data.forEach {
                // Read source from first entry and skip it
                if (it == data.first())
                    return@forEach

                val image = Image(
                    source = it,
                    isCanvas = data.first() == ImageSource.CANVASES.name
                )

                if (!viewModel.exist(image))
                    list.add(image)
            }
        }

        if (list.isNotEmpty())
            ImageSave().execute(*imageListToArray(list))
    }

    /**
     * Download images/canvases.
     *
     * @param data
     * @param source
     * @param status
     */
    override fun onDownloadComplete(
        data: String,
        source: ImageSource,
        status: DownloadStatus
    ) {
        if (status == DownloadStatus.OK && data.isNotBlank())
            DownloadJson(this, source).execute(data)
    }

    /**
     * Log error message on download failed.
     *
     * @param exception - exception thrown
     */
    override fun onError(exception: Exception) {
        Timber.e(exception)
    }

    private fun setupListAdapters() {
        val viewModel = viewDataBinding.viewmodel
        if (viewModel != null) {
            listAdapterImages = ImagesAdapter(viewModel)
            viewDataBinding.launchListImages.adapter = listAdapterImages
            listAdapterCanvases = CanvasesAdapter(viewModel)
            viewDataBinding.launchListCanvases.adapter = listAdapterCanvases
        } else {
            Timber.w("ViewModel not initialized when attempting to set up adapter.")
        }
    }

    /**
     * Method to redirect to the main screen
     */
    private fun startImagesActivity() {
        startActivity(Intent(activity?.applicationContext, ImagesActivity::class.java))
        activity?.finish()
    }

    /**
     * Save images in parallel threads.
     */
    inner class ImageSave :
        AsyncTask<Image, String, Void>() {

        override fun doInBackground(vararg args: Image): Void? {
            var bitmap: Bitmap?

            args.forEach {
                // Get bitmap from Internet
                bitmap = try {
                    BitmapFactory.decodeStream(java.net.URL(it.source).openStream())
                } catch (e: Exception) {
                    null
                }

                it.id = viewModel.getLatestImageId().let {
                    if (it != null)
                        it + 1
                    else
                        0
                }

                it.file = File(
                    activity?.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                    it.publicId + ".png"
                ).canonicalPath

                // Write on file system
                if (writeOnFileSystem(it, bitmap))
                    if (it.isCanvas)
                        viewModel.saveCanvas(it)
                    else
                        viewModel.saveImage(it)
            }
            return null
        }
    }
}