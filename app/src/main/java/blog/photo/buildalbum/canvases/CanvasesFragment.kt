/*
 * Copyright (c) 2018-2020 SAP and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 * Contributors:
 * SAP - initial API and implementation
 */
package blog.photo.buildalbum.canvases

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PorterDuff
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.*
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat.getColor
import androidx.core.view.forEach
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
import blog.photo.buildalbum.databinding.CanvasesFragBinding
import blog.photo.buildalbum.util.*
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import timber.log.Timber
import java.io.File

/**
 * Display a grid of [Image]s to be used as canvas.
 * User can select one to view details.
 */
class CanvasesFragment : Fragment(),
    AsyncResponse, DownloadData.OnDownloadComplete,
    DownloadJson.OnDataAvailable {

    private var _allowDownload = false

    private val args: CanvasesFragmentArgs by navArgs()

    private var countNewCanvasesAvailable = 0

    private var taskCountDown = 0

    private val viewModel by viewModels<CanvasesViewModel> { getCanvasViewModelFactory() }

    private lateinit var listAdapter: CanvasesAdapter

    private lateinit var navCanvases: MenuItem

    private lateinit var viewDataBinding: CanvasesFragBinding

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        // Set the lifecycle owner to the lifecycle of the view
        viewDataBinding.lifecycleOwner = viewLifecycleOwner
        setupAddCanvases()
        setupListAdapter()
        setupNavigation()
        setupRefreshLayout(viewDataBinding.canvasesRefreshLayout, viewDataBinding.canvasesList)
        view?.setupSnackBar(this, viewModel.snackBarText, Snackbar.LENGTH_LONG)
        setupSnackBar()

        // Scroll grid to top
        val recyclerView: RecyclerView? = activity?.findViewById(R.id.canvases_list)
        recyclerView?.viewTreeObserver!!
            .addOnGlobalLayoutListener {
                recyclerView.smoothScrollToPosition(0)
            }

        // Find MenuItem canvases in Menu from NavigationView of activity
        navCanvases =
            (activity?.findViewById(R.id.albumbuild_nav_view) as NavigationView).menu.findItem(R.id.canvases_fragment_dest)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        viewDataBinding = CanvasesFragBinding.inflate(inflater, container, false).apply {
            viewmodel = viewModel
        }

        setHasOptionsMenu(true)

        return viewDataBinding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.canvases_fragment_menu, menu)

        // Disable all menu items during loading
        viewModel.dataLoading.observe(viewLifecycleOwner, Observer {
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

        // Create notification on new canvases available.
        notifyNewCanvasesAvailable(
            requireActivity().application,
            navCanvases,
            countNewCanvasesAvailable
        )

        // Set download button active
        _allowDownload = countNewCanvasesAvailable > 0

        countNewCanvasesAvailable = 0

        viewModel.setAllowDownload(_allowDownload)
    }

    /**
     * Download canvases.
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

    override fun onOptionsItemSelected(item: MenuItem) =
        when (item.itemId) {
            R.id.menu_clear_favorite_canvases -> {
                viewModel.clearFavoriteCanvases()
                true
            }
            R.id.menu_filter_all -> {
                viewModel.setFiltering(CanvasesFilterType.ALL_CANVASES)
                true
            }
            R.id.menu_filter_favorite -> {
                viewModel.setFiltering(CanvasesFilterType.FAVORITE_CANVASES)
                true
            }
            R.id.menu_refresh_canvases -> {
                viewModel.loadCanvases(true)
                true
            }
            else -> false
        }

    override fun onResume() {
        super.onResume()

        checkInternet()
    }

    /**
     * On canvas save task begin
     */
    override fun onTaskBegin() {
        taskCountDown++
    }

    /**
     * When all image save tasks have completed
     * enable buttons and hide progress spinner.
     */
    override fun onTaskComplete(stringId: Int) {
        taskCountDown--
        if (taskCountDown <= 0) {
            activateButtons(true)
        }
    }

    /**
     * Toggle option buttons with running ImageSave task.
     */
    private fun activateButtons(enable: Boolean) {
        if (enable) {
            activity?.findViewById<FloatingActionButton>(R.id.canvases_add_new_fab)?.isEnabled =
                true
            activity?.findViewById<FloatingActionButton>(R.id.canvases_add_new_fab)?.alpha = 1.0f
            activity?.findViewById<RecyclerView>(R.id.canvases_list)?.isEnabled = true
            activity?.findViewById<RecyclerView>(R.id.canvases_list)?.alpha = 1.0f
            activity?.findViewById<LinearLayout>(R.id.no_canvases_layout)?.isEnabled = true
            activity?.findViewById<LinearLayout>(R.id.no_canvases_layout)?.alpha = 1.0f
            activity?.findViewById<LinearLayout>(R.id.canvases_no_internet)?.isEnabled = true
            activity?.findViewById<LinearLayout>(R.id.canvases_no_internet)?.alpha = 1.0f
            activity?.findViewById<LinearLayout>(R.id.canvases_progress_spinner)?.visibility =
                View.GONE
            activity?.findViewById<TextView>(R.id.progress_percentage)?.text = "0 %"
        } else {
            activity?.findViewById<FloatingActionButton>(R.id.canvases_add_new_fab)?.isEnabled =
                false
            activity?.findViewById<FloatingActionButton>(R.id.canvases_add_new_fab)?.alpha = 0.1f
            activity?.findViewById<RecyclerView>(R.id.canvases_list)?.isEnabled = false
            activity?.findViewById<RecyclerView>(R.id.canvases_list)?.alpha = 0.1f
            activity?.findViewById<LinearLayout>(R.id.no_canvases_layout)?.isEnabled = false
            activity?.findViewById<LinearLayout>(R.id.no_canvases_layout)?.alpha = 0.1f
            activity?.findViewById<LinearLayout>(R.id.canvases_no_internet)?.isEnabled = false
            activity?.findViewById<LinearLayout>(R.id.canvases_no_internet)?.alpha = 0.1f
            activity?.findViewById<LinearLayout>(R.id.canvases_progress_spinner)?.visibility =
                View.VISIBLE
        }
    }

    private fun checkInternet() {
        hasInternet().subscribe { hasInternet ->

            // Check for new canvases available
            if (hasInternet)
                downloadCanvases(activateButtons = true, allowDownload = false)
            else {
                hideNotificationNewCanvasesAvailable(requireActivity().application, navCanvases)

                viewModel.setAllowDownload(false)
            }

            viewModel.setHasPermissionInternet(hasInternet)
        }
    }

    private fun downloadCanvases(activateButtons: Boolean, allowDownload: Boolean) {
        activateButtons(activateButtons)
        _allowDownload = allowDownload
        download(this, ImageSource.CANVASES, requireActivity()?.getString(R.string.URI_API_CANVASES))
    }

    private fun setupAddCanvases() {
        activity?.findViewById<FloatingActionButton>(R.id.canvases_add_new_fab)
            ?.setOnClickListener {
                downloadCanvases(activateButtons = false, allowDownload = true)
            }

        activity?.findViewById<LinearLayout>(R.id.no_canvases_layout)?.setOnClickListener {
            downloadCanvases(activateButtons = false, allowDownload = true)
        }

        activity?.findViewById<ImageView>(R.id.no_canvases_icon)?.setOnClickListener {
            downloadCanvases(activateButtons = false, allowDownload = true)
        }

        activity?.findViewById<LinearLayout>(R.id.canvases_no_internet)?.setOnClickListener {
            openNetworkManager(requireActivity())
        }

        activity?.findViewById<ImageView>(R.id.canvases_no_internet_icon)?.setOnClickListener {
            openNetworkManager(requireActivity())
        }

        activity?.findViewById<TextView>(R.id.canvases_no_internet_text)?.setOnClickListener {
            openNetworkManager(requireActivity())
        }
    }

    private fun setupListAdapter() {
        val viewModel = viewDataBinding.viewmodel
        if (viewModel != null) {
            listAdapter = CanvasesAdapter(viewModel)
            viewDataBinding.canvasesList.adapter = listAdapter
        } else {
            Timber.w("ViewModel not initialized when attempting to set up adapter.")
        }
    }

    private fun setupNavigation() {
        viewModel.canvasOpenEvent.observe(viewLifecycleOwner, EventObserver {
            findNavController().navigate(
                CanvasesFragmentDirections
                    .actionCanvasesFragmentToCanvasDetailFragment(
                        it, viewModel.isFavorite(it)
                    )
            )
        })
    }

    private fun setupSnackBar() {
        view?.setupSnackBar(this, viewModel.snackBarText, Snackbar.LENGTH_LONG)
        arguments?.let {
            viewModel.showResultMessage(args.userMessage)
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

                // Get bitmap from Internet
                bitmap = try {
                    BitmapFactory.decodeStream(java.net.URL(it.source).openStream())
                } catch (e: Exception) {
                    null
                }

                // Write on file system
                if (writeOnFileSystem(it, bitmap)) {
                    if (it.isCanvas)
                        viewModel.saveCanvas(it)

                    countAdded++

                    publishProgress(
                        "".plus((countAdded.toDouble() / args.size * 100).toInt()).plus(
                            " %"
                        )
                    )
                }
            }

            // Show SnackBar message
            return if (countAdded > 0 && args.first().isCanvas) {

                R.string.successfully_canvases_downloaded_message
            } else {

                R.string.there_are_no_new_canvases
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
