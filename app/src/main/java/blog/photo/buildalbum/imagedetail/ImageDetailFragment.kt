/*
 * Copyright (c) 2018-2020 SAP and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 * Contributors:
 * SAP - initial API and implementation
 */
package blog.photo.buildalbum.imagedetail

import android.content.Intent
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.*
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat.getColor
import androidx.core.content.FileProvider.getUriForFile
import androidx.core.view.forEach
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.RecyclerView
import blog.photo.buildalbum.EventObserver
import blog.photo.buildalbum.R
import blog.photo.buildalbum.async.*
import blog.photo.buildalbum.data.Image
import blog.photo.buildalbum.databinding.ImagedetailFragBinding
import blog.photo.buildalbum.images.IMAGE_DELETE_RESULT_OK
import blog.photo.buildalbum.images.IMAGE_EDIT_RESULT_CANCEL
import blog.photo.buildalbum.images.IMAGE_EDIT_RESULT_OK
import blog.photo.buildalbum.util.*
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
import kotlin.math.roundToInt

/**
 * Main UI for the image detail screen.
 */
class ImageDetailFragment : Fragment(),
    AsyncResponse, DownloadData.OnDownloadComplete,
    DownloadJson.OnDataAvailable {

    private var _allowDownload = false

    private val args: ImageDetailFragmentArgs by navArgs()

    private var countNewCanvasesAvailable = 0

    private val imageSize = 300

    private val imageSizeBorder = 100f

    private var rotateIndex = 0

    private var taskCountDown = 0

    private val viewModel by viewModels<ImageDetailViewModel> { getImageViewModelFactory() }

    private lateinit var listAdapter: CanvasesAdapter

    private lateinit var menuItemFavorite: MenuItem

    private lateinit var viewDataBinding: ImagedetailFragBinding

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        setupFab()
        setupListAdapter()
        setupNavigation()
        view?.setupSnackBar(this, viewModel.snackBarText, Snackbar.LENGTH_LONG)
        setupRefreshLayout(
            viewDataBinding.imagedetailsRefreshLayout,
            viewDataBinding.imagedetailCanvasesList
        )

        val recyclerView: RecyclerView? = activity?.findViewById(R.id.imagedetail_canvases_list)
        recyclerView?.viewTreeObserver!!
            .addOnGlobalLayoutListener {
                recyclerView.smoothScrollToPosition(0)
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.imagedetail_frag, container, false)
        viewDataBinding = ImagedetailFragBinding.bind(view).apply {
            viewmodel = viewModel
        }
        viewDataBinding.lifecycleOwner = this.viewLifecycleOwner

        viewModel.start(args.imageId)

        setHasOptionsMenu(true)

        return view
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.imagedetail_fragment_menu, menu)

        // Disable all menu items during loading
        viewModel.dataLoading.observe(viewLifecycleOwner, Observer {
            activity?.findViewById<LinearLayout>(R.id.imagedetail_canvases_linear_layout)?.isVisible =
                !it

            activity?.findViewById<RecyclerView>(R.id.imagedetail_canvases_list)?.isVisible =
                !it

            activity?.findViewById<FloatingActionButton>(R.id.imagedetail_fab)?.isVisible =
                !it

            menu.forEach { menuItem ->
                menuItem.isVisible = !it

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                    menuItem.icon?.setTint(
                        getColor(
                            requireActivity(),
                            R.color.colorSecondary
                        )
                    )
                else
                    menuItem.icon?.setColorFilter(
                        getColor(
                            requireActivity(),
                            R.color.colorSecondary
                        ),
                        PorterDuff.Mode.SRC_IN
                    )
            }
        })

        menuItemFavorite = menu.findItem(R.id.menu_favorite_image)
        setupFavorite(args.isFavorite)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_delete_image -> {
                val imageList = ArrayList<Image>()
                imageList.add(viewModel.image.value!!)

                if (deleteImages(imageList))
                    viewModel.deleteImage()

                true
            }
            R.id.menu_favorite_image -> {
                viewModel.favoriteImage()
                true
            }
            R.id.menu_print -> {
                viewModel.printImage()
                true
            }
            R.id.menu_rotate -> {
                viewModel.rotateImage()
                true
            }
            R.id.menu_share -> {
                viewModel.shareImage()
                true
            }
            else -> false
        }
    }

    override fun onResume() {
        super.onResume()

        checkInternet()
    }

    /**
     * Download URIs.
     *
     * @param data - URIs
     */
    override fun onDataAvailable(data: ArrayList<String>) {
        activity?.findViewById<TextView>(R.id.progress_title)?.text =
            getString(R.string.progress_downloading)

        val list = ArrayList<Image>()

        if (data.isNotEmpty())
            data.forEach {
                // Get download source from first entry and skip it.
                // If source different than canvases, skip it
                if (data.first() == it ||
                    data.first() != ImageSource.CANVASES.name
                )
                    return@forEach

                val canvas = Image(
                    source = it,
                    isCanvas = true
                )

                // Check canvas in db
                if (!viewModel.exist(canvas)) {
                    if (_allowDownload)
                    // Add available canvases for download
                        list.add(canvas)
                    else
                    // Count available canvases for download
                        countNewCanvasesAvailable++
                }
            }

        if (_allowDownload && list.isNotEmpty())
            ImageSave().execute(*imageListToArray(list))
        else
            activateButtons(true)

        // Set download button active
        _allowDownload = countNewCanvasesAvailable > 0

        countNewCanvasesAvailable = 0
    }

    /**
     * Download canvases.
     *
     * @param data
     * @param source
     * @param status
     */
    override fun onDownloadComplete(data: String, source: ImageSource, status: DownloadStatus) {
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
     * Toggle buttons while running ImageSave task.
     */
    private fun activateButtons(enable: Boolean) {
        activity?.findViewById<FloatingActionButton>(R.id.imagedetail_fab)?.isGone = enable

        activity?.findViewById<LinearLayout>(R.id.imagedetail_no_canvases_linear_layout)?.alpha =
            if (enable) 1.0f else 0.1f
        activity?.findViewById<LinearLayout>(R.id.imagedetail_no_canvases_linear_layout)?.isClickable =
            enable

        activity?.findViewById<LinearLayout>(R.id.imagedetail_progress_spinner)?.isGone = enable
    }

    private fun checkInternet() {
        hasInternet().subscribe { hasInternet ->
            if (hasInternet) {
                downloadCanvases(activateButtons = true, allowDownload = false)
            }

            activity?.findViewById<View>(R.id.menu_print)?.isGone = hasInternet
            activity?.findViewById<View>(R.id.menu_share)?.isGone = hasInternet

            viewModel.setHasPermissionInternet(hasInternet)

            setupAddCanvases()
        }
    }

    private fun downloadCanvases(activateButtons: Boolean, allowDownload: Boolean) {
        activateButtons(activateButtons)
        _allowDownload = allowDownload
        download(this, ImageSource.CANVASES, requireActivity().getString(R.string.URI_API_CANVASES))
    }

    private fun setupFab() {
        activity?.findViewById<FloatingActionButton>(R.id.imagedetail_fab)?.setOnClickListener {
            // Create image to be stored in db
            val image = Image(
                source = ImageSource.ALBUM_BUILD.name, isEdited = true
            )

            // Check if writing on device possible and store image
            if (viewModel.isNewImage.value!!) {
                ImageSave().execute(image)
                return@setOnClickListener
            }

            viewModel.saveImage()
        }
    }

    private fun setupFavorite(isFavorite: Boolean) {
        if (isFavorite)
            menuItemFavorite.setIcon(R.drawable.ic_favorite_24dp)
        else
            menuItemFavorite.setIcon(R.drawable.ic_favorite_border_24dp)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            menuItemFavorite.icon?.setTint(
                getColor(
                    requireActivity(),
                    R.color.colorSecondary
                )
            )
        else
            menuItemFavorite.icon?.setColorFilter(
                getColor(
                    requireActivity(),
                    R.color.colorSecondary
                ),
                PorterDuff.Mode.SRC_IN
            )
    }

    private fun setupListAdapter() {
        val viewModel = viewDataBinding.viewmodel
        if (viewModel != null) {
            listAdapter = CanvasesAdapter(viewModel)
            viewDataBinding.imagedetailCanvasesList.adapter = listAdapter
        } else {
            Timber.w("ViewModel not initialized when attempting to set up adapter.")
        }
    }

    private fun setupNavigation() {
        viewModel.imageEditEvent.observe(viewLifecycleOwner, EventObserver {
            findNavController().navigate(
                ImageDetailFragmentDirections
                    .actionImageDetailFragmentToImagesFragment(IMAGE_EDIT_RESULT_OK)
            )
        })
        viewModel.imageDeleteEvent.observe(viewLifecycleOwner, EventObserver {
            findNavController().navigate(
                ImageDetailFragmentDirections
                    .actionImageDetailFragmentToImagesFragment(IMAGE_DELETE_RESULT_OK)
            )
        })
        viewModel.imageFavoriteEvent.observe(viewLifecycleOwner, EventObserver {
            setupFavorite(it)
        })
        viewModel.imagePrintEvent.observe(viewLifecycleOwner, EventObserver {
            imagePrint()
        })
        viewModel.imageRotateEvent.observe(viewLifecycleOwner, EventObserver {
            imageRotate()
        })
        viewModel.imageSaveEvent.observe(viewLifecycleOwner, EventObserver {
            findNavController().navigate(
                ImageDetailFragmentDirections
                    .actionImageDetailFragmentToImagesFragment(IMAGE_EDIT_RESULT_CANCEL)
            )
        })
        viewModel.imageShareEvent.observe(viewLifecycleOwner, EventObserver {
            imageShare()
        })
        viewModel.applyCanvasEvent.observe(viewLifecycleOwner, EventObserver {
            imageApplyCanvas(it)
        })
    }

    private fun setupAddCanvases() {
        activity?.findViewById<LinearLayout>(R.id.imagedetail_no_canvases_linear_layout)
            ?.setOnClickListener {
                downloadCanvases(activateButtons = false, allowDownload = true)
            }

        activity?.findViewById<TextView>(R.id.imagedetail_no_canvases_text)
            ?.setOnClickListener {
                downloadCanvases(activateButtons = false, allowDownload = true)
            }

        activity?.findViewById<ImageView>(R.id.imagedetail_no_canvases_icon)
            ?.setOnClickListener {
                downloadCanvases(activateButtons = false, allowDownload = true)
            }

        activity?.findViewById<LinearLayout>(R.id.imagedetail_no_internet)?.setOnClickListener {
            openNetworkManager(requireActivity())
        }

        activity?.findViewById<TextView>(R.id.imagedetail_no_internet_text)?.setOnClickListener {
            openNetworkManager(requireActivity())
        }

        activity?.findViewById<ImageView>(R.id.imagedetail_no_internet_icon)?.setOnClickListener {
            openNetworkManager(requireActivity())
        }
    }

    private fun imageApplyCanvas(canvasId: Int) {
        // Get bitmap of image rotated by current index
        var bitmapOld =
            bitmapRotate(getBitmapFromImageOriginal(), rotateIndex)

        // Get bitmap of canvas
        var bitmapNew: Bitmap? = null
        viewModel.canvases.value?.forEach {
            if (it.id == canvasId)
                bitmapNew = try {
                    BitmapFactory.decodeFile(it.file)
                } catch (e: NullPointerException) {
                    null
                }
        }

        // If canvas can not be found, redirect to Image List
        if (bitmapNew == null || bitmapOld == null) {
            viewModel.imageSaveEvent.observe(viewLifecycleOwner, EventObserver {
                findNavController().navigate(
                    ImageDetailFragmentDirections
                        .actionImageDetailFragmentToImagesFragment(IMAGE_EDIT_RESULT_CANCEL)
                )
            })
            Timber.e("ImageDetailFragment.imageApplyCanvas: File not found")
            return
        }

        // Add white background
        // 1. Create bitmap square to fit the image
        var bitmapWhite =
            if (bitmapOld.height > bitmapOld.width)
                Bitmap.createScaledBitmap(
                    bitmapOld,
                    bitmapOld.height,
                    bitmapOld.height,
                    false
                )
            else
                Bitmap.createScaledBitmap(
                    bitmapOld,
                    bitmapOld.width,
                    bitmapOld.width,
                    false
                )

        // 2. Paint the bitmap square with white
        var mutableBitmap = bitmapWhite.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)
        canvas.drawColor(Color.WHITE)

        // 3. Place the image bitmap on the white square
        canvas.drawBitmap(
            bitmapOld,
            ((bitmapWhite.width - bitmapOld.width) / 2).toFloat(),
            ((bitmapWhite.height - bitmapOld.height) / 2).toFloat(),
            null
        )

        // Resize the image to fit in the canvas
        mutableBitmap =
            Bitmap.createScaledBitmap(
                mutableBitmap,
                (imageSize - imageSizeBorder).roundToInt(),
                (imageSize - imageSizeBorder).roundToInt(),
                false
            )

        // Resize the canvas to be applied
        bitmapNew =
            Bitmap.createScaledBitmap(
                bitmapNew!!.copy(
                    Bitmap.Config.ARGB_8888,
                    true
                ),
                imageSize,
                imageSize,
                false
            )

        // Add the scaled image onto the canvas
        Canvas(bitmapNew!!).drawBitmap(
            mutableBitmap,
            imageSizeBorder / 2,
            imageSizeBorder / 2,
            null
        )

        if (!viewModel.isNewImage.value!!)
            viewModel.isNewImage.value = true

        activity?.findViewById<ImageView>(R.id.imagedetail_bitmap)?.setImageBitmap(bitmapNew)
    }

    private fun imageRotate() {
        // Get bitmap of image rotated by current index
        val bitmapNew = bitmapRotate(getBitmapFromImageOriginal(), ++rotateIndex)

        if (!viewModel.isNewImage.value!!)
            viewModel.isNewImage.value = true

        activity?.findViewById<ImageView>(R.id.imagedetail_bitmap)?.setImageBitmap(bitmapNew)
    }

    private fun imagePrint() {
        var intent = Intent(Intent.ACTION_SEND)
        intent.type = "image/*"
        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION

        intent.putExtra(
            Intent.EXTRA_STREAM,
            getUriForFile(
                requireContext(),
                requireContext().applicationContext.packageName + ".FileProvider",
                File(viewModel.image.value?.file!!)
            )
        )

        // Set to printint service
        intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse(getString(R.string.URI_API_PRINT))
        )

        startActivity(intent)
    }

    private fun imageShare() {
        var intent = Intent(Intent.ACTION_SEND)
        intent.type = "image/*"
        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION

        intent.putExtra(
            Intent.EXTRA_STREAM,
            getUriForFile(
                requireContext(),
                requireContext().applicationContext.packageName + ".FileProvider",
                File(viewModel.image.value?.file!!)
            )
        )

        // See if official Facebook app is found
        var facebookAppFound = false
        val matches = activity?.packageManager?.queryIntentActivities(intent, 0)
        for (info in matches!!) {
            if (info.activityInfo.packageName.equals(
                    getString(
                        R.string.facebook_package
                    ),
                    true
                )
            ) {
                intent.setPackage(info.activityInfo.packageName)
                facebookAppFound = true
                break
            }
        }

        // As fallback, launch sharer.php in a browser
        if (!facebookAppFound) {
            intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse(getString(R.string.URI_API_SHARE).plus(getString(R.string.app_web_host)))
            )
        }

        startActivity(intent)
    }

    private fun bitmapRotate(bitmap: Bitmap?, index: Int): Bitmap? {
        rotateIndex = index % 4

        if (rotateIndex == 0 || bitmap == null)
            return bitmap

        val degrees = 90f * rotateIndex
        val matrix = Matrix()
        matrix.setRotate(degrees)

        return Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            matrix,
            true
        )
    }

    private fun getBitmapFromImageOriginal(): Bitmap? {
        return try {
            BitmapFactory.decodeFile(viewModel.image.value?.file)
        } catch (e: FileNotFoundException) {
            null
        }
    }

    private fun getBitmapFromImageView(): Bitmap {
        return (activity?.findViewById<ImageView>(R.id.imagedetail_bitmap)?.drawable as BitmapDrawable).bitmap
    }

    /**
     * Save image in parallel threads.
     */
    inner class ImageSave :
        AsyncTask<Image, String, Int>() {

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

                // Canvases are downloaded
                if (it.isCanvas) {
                    // Get bitmap from Internet
                    bitmap = try {
                        BitmapFactory.decodeStream(java.net.URL(it.source).openStream())
                    } catch (e: Exception) {
                        null
                    }

                    // Write on file system
                    if (writeOnFileSystem(it, bitmap)) {
                        viewModel.saveImage(it)

                        countAdded++

                        publishProgress(
                            "".plus((countAdded.toDouble() / args.size * 100).toInt()).plus(
                                " %"
                            )
                        )
                    }
                } else if (args.size == 1) {
                    val image = args.first()

                    image.isEdited = true

                    // Get bitmap from ImageView
                    bitmap = getBitmapFromImageView()

                    // Write on device
                    if (writeOnFileSystem(image, bitmap))
                        viewModel.editImage(image)

                    // Image edited
                    return R.string.successfully_image_edited_message
                }
            }

            // Show SnackBar message
            return if (countAdded > 0) {

                // Canvas added
                R.string.successfully_canvases_downloaded_message
            } else {

                // Image left unchanged
                viewModel.saveImage()
                return R.string.image_edit_cancelled_message
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
