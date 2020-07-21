/*
 * Copyright (c) 2018-2020 SAP and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 * Contributors:
 * SAP - initial API and implementation
 */
package blog.photo.buildalbum.addimage

import android.Manifest
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import android.os.AsyncTask
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import blog.photo.buildalbum.EventObserver
import blog.photo.buildalbum.R
import blog.photo.buildalbum.async.*
import blog.photo.buildalbum.data.Image
import blog.photo.buildalbum.databinding.AddimageFragBinding
import blog.photo.buildalbum.images.IMAGE_ADD_RESULT_CANCEL
import blog.photo.buildalbum.images.IMAGE_ADD_RESULT_OK
import blog.photo.buildalbum.images.IMAGE_DOWNLOAD_RESULT_CANCEL
import blog.photo.buildalbum.images.IMAGE_DOWNLOAD_RESULT_OK
import blog.photo.buildalbum.util.*
import com.google.android.material.snackbar.Snackbar
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException

/**
 * Display a grid of [ImageSource]s.
 * User can select one to add new image.
 */
class AddImageFragment : Fragment(),
    AsyncResponse, DownloadData.OnDownloadComplete,
    DownloadJson.OnDataAvailable {

    private var permissionsGranted = mutableListOf<String>()

    private val permissionsRequired =
        arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

    private var permissionsRequestedOnce = false

    private val requestCode = 8888

    private var taskCountDown = 0

    private val viewModel by viewModels<AddImageViewModel> { getImageViewModelFactory() }

    private lateinit var listAdapterImages: ImagesAdapter

    private lateinit var viewDataBinding: AddimageFragBinding

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        // Set the lifecycle owner to the lifecycle of the view
        viewDataBinding.lifecycleOwner = this.viewLifecycleOwner

        setupListAdapters()
        setupNavigation()
        setupRefreshLayout(viewDataBinding.addnewRefreshLayout, viewDataBinding.addimageImagesList)
        view?.setupSnackBar(this, viewModel.snackBarText, Snackbar.LENGTH_LONG)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewDataBinding = AddimageFragBinding.inflate(inflater, container, false).apply {
            viewmodel = viewModel
        }

        return viewDataBinding.root
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        activateButtons(false)

        // Check for the right request/result codes and data available
        if (requestCode == this.requestCode && resultCode == RESULT_OK && data != null) {

            when {
                // Image added with Camera
                data.extras?.get("data") != null -> {

                    activity?.findViewById<ImageView>(R.id.new_image)
                        ?.setImageBitmap(data.extras?.get("data") as Bitmap?)

                    ImageSave().execute(
                        Image(
                            source = ImageSource.CAMERA.name
                        )
                    )
                }

                // Image added from Gallery
                data.data != null -> {
                    try {

                        activity?.findViewById<ImageView>(R.id.new_image)?.setImageBitmap(
                            BitmapFactory.decodeStream(
                                activity?.contentResolver?.openInputStream(data.data!!)
                            )
                        )

                        ImageSave().execute(
                            Image(
                                source = ImageSource.STORAGE.name
                            )
                        )
                    } catch (e: FileNotFoundException) {
                        Timber.e(e)
                        return
                    }
                }
            }
        } else {
            // Image capturing is cancelled
            activateButtons(true)

            findNavController().navigate(
                AddImageFragmentDirections
                    .actionAddNewFragmentToImagesFragment(IMAGE_ADD_RESULT_CANCEL)
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        // If permissions were granted add them all.
        if (requestCode == this.requestCode &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            permissionsGranted.addAll(permissions)
        }
        // If permissions were not granted remove them.
        else {
            permissionsGranted.removeAll(permissions)
        }
    }

    override fun onResume() {
        super.onResume()

        checkInternet()

        getPermissions()
    }

    /**
     * Download URIs.
     *
     * @param data - URIs
     */
    override fun onDataAvailable(data: ArrayList<String>) {
        val list = ArrayList<Image>()

        if (data.isNotEmpty()) {
            // Get download source from first entry.
            // If source different than Pixabay, skip it.
            if (data.first() != ImageSource.PIXABAY.name)
                return

            data.forEach {
                // If first entry, skip it.
                if (data.first() == it)
                    return@forEach

                val image = Image(
                    source = it
                )

                // Add unique image for download
                if (!viewModel.exist(image))
                    list.add(image)
            }
        }

        if (list.isNotEmpty())
            ImageSave().execute(*imageListToArray(list))
        else
            findNavController().navigate(
                AddImageFragmentDirections
                    .actionAddNewFragmentToImagesFragment(IMAGE_DOWNLOAD_RESULT_CANCEL)
            )
    }

    /**
     * Download images.
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
        // TODO onError Show snackBar message
        Timber.e(exception)
    }

    /**
     * On picture save task begin
     * increase active tasks counter.
     */
    override fun onTaskBegin() {
        taskCountDown++
    }

    /**
     * When all image save tasks have completed,
     * enable buttons and hide progress spinner.
     */
    override fun onTaskComplete(stringId: Int) {
        taskCountDown--
        if (taskCountDown <= 0) {
            activateButtons(true)
        }
    }

    /**
     * Add image by [R.string.add_with_camera]
     */
    private fun addWithCamera() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { intent ->
            // Ensure there is an activity to handle the intent
            intent.resolveActivity(activity?.packageManager!!)?.also {
                startActivityForResult(intent, requestCode)
            }
        }
    }

    /**
     * Add image by [R.string.add_from_storage]
     */
    private fun addFromStorage() {
        Intent(
            Intent.ACTION_PICK
        ).also { intent ->
            intent.type = "image/*"
            startActivityForResult(intent, requestCode)
        }
    }

    // Check Internet connection
    private fun checkInternet() {
        hasInternet().subscribe { hasInternet ->
            viewModel.setHasPermissionInternet(hasInternet)

            setupButtons()
        }
    }

    /**
     * Observe add events.
     * Redirect with proper message.
     */
    private fun setupNavigation() {
        viewModel.imageAddEvent.observe(viewLifecycleOwner, EventObserver {
            findNavController().navigate(
                AddImageFragmentDirections
                    .actionAddNewFragmentToImagesFragment(IMAGE_ADD_RESULT_OK)
            )
        })
        viewModel.imageDownloadEvent.observe(viewLifecycleOwner, EventObserver {
            findNavController().navigate(
                AddImageFragmentDirections
                    .actionAddNewFragmentToImagesFragment(IMAGE_DOWNLOAD_RESULT_OK)
            )
        })
        viewModel.imageDownloadCancelEvent.observe(viewLifecycleOwner, EventObserver {
            findNavController().navigate(
                AddImageFragmentDirections
                    .actionAddNewFragmentToImagesFragment(IMAGE_DOWNLOAD_RESULT_CANCEL)
            )
        })
    }

    /**
     * Toggle buttons while running ImageSave task.
     */
    private fun activateButtons(enable: Boolean) {

        activity?.findViewById<ImageView>(R.id.addimage_camera)?.let {
            it.isClickable = enable
            it.setColorFilter(
                ContextCompat.getColor(
                    requireActivity(),
                    if (enable) R.color.colorPrimary
                    else R.color.colorAccent
                ),
                PorterDuff.Mode.SRC_IN
            )
        }

        activity?.findViewById<ImageView>(R.id.addimage_storage)?.let {
            it.isClickable = enable
            it.setColorFilter(
                ContextCompat.getColor(
                    requireActivity(),
                    if (enable) R.color.colorPrimary
                    else R.color.colorAccent
                ),
                PorterDuff.Mode.SRC_IN
            )
        }

        activity?.findViewById<ImageView>(R.id.addimage_cloud)?.let {
            it.isClickable = enable
            it.setColorFilter(
                ContextCompat.getColor(
                    requireActivity(),
                    if (enable) R.color.colorPrimary
                    else R.color.colorAccent
                ),
                PorterDuff.Mode.SRC_IN
            )
        }

        activity?.findViewById<LinearLayout>(R.id.addnew_progress_spinner)?.isVisible = !enable

        if (!enable)
            activity?.findViewById<TextView>(R.id.progress_percentage)?.text = "0 %"
    }

    private fun getBitmapFromImageView(): Bitmap {
        return (activity?.findViewById<ImageView>(R.id.new_image)?.drawable as BitmapDrawable).bitmap
    }

    /**
     * Ask for required permission.
     */
    private fun getPermissions() {
        // Create a list of permissions to request
        val permissionsRequested = ArrayList<String>()

        permissionsRequired.forEach {
            // If permission is granted, skip it from request
            if (ActivityCompat.checkSelfPermission(
                    activity?.applicationContext!!,
                    it
                ) == PackageManager.PERMISSION_GRANTED
            )
                permissionsGranted.add(it)
            // If permission is not granted, add it to request
            else
                permissionsRequested.add(it)
        }

        /**
         * If permissions are still needed request them at once.
         * Popup for anyone of them will be displayed separately.
         */
        if (permissionsRequested.isNotEmpty() && !permissionsRequestedOnce) {
            permissionsRequestedOnce = true

            val permissionsRequestedArray = arrayOfNulls<String>(permissionsRequested.size)

            permissionsRequested.toArray(permissionsRequestedArray)

            ActivityCompat.requestPermissions(
                requireActivity(),
                permissionsRequestedArray,
                requestCode
            )
        }

        setupButtons()
    }

    private fun setupButtons() {
        activity?.findViewById<TextView>(R.id.no_permission_camera)?.setOnClickListener {
            openPermissionManager(requireActivity())
        }

        /**
         * Toggle button [R.id.addimage_camera]
         * Set colorFilter for API level <= 21
         */
        (Manifest.permission.CAMERA in permissionsGranted).let { setHasPermissionCamera ->
            viewModel.setHasPermissionCamera(setHasPermissionCamera)

            activity?.findViewById<ImageView>(R.id.addimage_camera)?.setColorFilter(
                ContextCompat.getColor(
                    requireActivity(),
                    if (setHasPermissionCamera) R.color.colorPrimary
                    else R.color.colorAccent
                ),
                PorterDuff.Mode.SRC_IN
            )

            activity?.findViewById<ImageView>(R.id.addimage_camera)?.setOnClickListener {
                if (setHasPermissionCamera) {
                    activateButtons(false)

                    activity?.findViewById<TextView>(R.id.progress_title)?.text =
                        getString(R.string.progress_capturing)

                    addWithCamera()
                } else
                    openPermissionManager(requireActivity())
            }
        }

        activity?.findViewById<TextView>(R.id.no_permission_storage)?.setOnClickListener {
            openPermissionManager(requireActivity())
        }

        /**
         * Toggle button [R.id.addimage_storage]
         * Set colorFilter for API level <= 21
         */
        (Manifest.permission.WRITE_EXTERNAL_STORAGE in permissionsGranted).let { setHasPermissionStorage ->
            viewModel.setHasPermissionStorage(setHasPermissionStorage)

            activity?.findViewById<ImageView>(R.id.addimage_storage)?.setColorFilter(
                ContextCompat.getColor(
                    requireActivity(),
                    if (setHasPermissionStorage) R.color.colorPrimary
                    else R.color.colorAccent
                ),
                PorterDuff.Mode.SRC_IN
            )

            activity?.findViewById<ImageView>(R.id.addimage_storage)?.setOnClickListener {
                if (setHasPermissionStorage) {
                    activateButtons(false)

                    activity?.findViewById<TextView>(R.id.progress_title)?.text =
                        getString(R.string.progress_capturing)

                    addFromStorage()
                } else
                    openPermissionManager(requireActivity())
            }
        }

        activity?.findViewById<LinearLayout>(R.id.addimage_no_internet)?.setOnClickListener {
            openNetworkManager(requireActivity())
        }

        /**
         * Toggle button [R.id.addimage_cloud]
         * Set colorFilter for API level <= 21
         */
        (viewModel.hasPermissionInternet.value).let { hasPermissionInternet ->
            activity?.findViewById<ImageView>(R.id.addimage_cloud)?.setColorFilter(
                ContextCompat.getColor(
                    requireActivity(),
                    if (hasPermissionInternet!!) R.color.colorPrimary
                    else R.color.colorAccent
                ),
                PorterDuff.Mode.SRC_IN
            )

            activity?.findViewById<ImageView>(R.id.addimage_cloud)?.setOnClickListener {
                if (hasPermissionInternet!!) {
                    activateButtons(false)

                    activity?.findViewById<TextView>(R.id.progress_title)?.text =
                        getString(R.string.progress_downloading)

                    download(this, ImageSource.PIXABAY, getString(R.string.URI_API_PIXABAY))
                } else
                    openNetworkManager(requireActivity())
            }
        }
    }

    private fun setupListAdapters() {
        val viewModel = viewDataBinding.viewmodel

        if (viewModel != null) {
            listAdapterImages = ImagesAdapter(viewModel)
            viewDataBinding.addimageImagesList.adapter = listAdapterImages
        } else {
            Timber.w("ViewModel not initialized when attempting to set up adapter.")
        }
    }

    /**
     * Save images in parallel threads.
     */
    inner class ImageSave :
        AsyncTask<Image, String, Int>() {

        // Mark task beginning
        override fun onPreExecute() {
            onTaskBegin()
        }

        override fun doInBackground(vararg args: Image): Int? {
            var bitmap: Bitmap?
            var countAdded = 0

            args.forEach {
                it.id = viewModel.getLatestImageId().let {
                    if (it != null)
                        it + countAdded + 1
                    else
                        countAdded
                }

                it.file = File(
                    activity?.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                    it.publicId + ".png"
                ).canonicalPath

                // Multiple images are downloaded
                if (it.source != ImageSource.CAMERA.name &&
                    it.source != ImageSource.STORAGE.name
                ) {
                    // Get bitmap from Internet
                    bitmap = try {
                        BitmapFactory.decodeStream(java.net.URL(it.source).openStream())
                    } catch (e: Exception) {
                        null
                    }

                    // Write on file system
                    if (writeOnFileSystem(it, bitmap)) {
                        viewModel.imageDownload(it)

                        countAdded++

                        publishProgress(
                            "".plus((countAdded.toDouble() / args.size * 100).toInt()).plus(
                                " %"
                            )
                        )
                    }
                }

                // Single image is added
                else if (args.size == 1) {
                    // Get bitmap from ImageView
                    bitmap = getBitmapFromImageView()

                    publishProgress("50 %")

                    // Write on device
                    if (writeOnFileSystem(it, bitmap))
                        viewModel.imageAdd(it)

                    return R.string.successfully_image_added_message
                }
            }

            // Show SnackBar message
            return if (countAdded > 0) {

                viewModel.imageDownload(countAdded)
                R.string.successfully_images_downloaded_message
            } else {

                viewModel.imageDownloadCancel()
                R.string.there_are_no_new_images
            }
        }

        // Message on progress
        override fun onProgressUpdate(vararg values: String?) {
            if (values.isNotEmpty()) {
                activity?.findViewById<TextView>(R.id.progress_percentage)?.text = values[0]
            }
        }

        // Message on end
        override fun onPostExecute(result: Int) {
            onTaskComplete(result)
        }
    }
}