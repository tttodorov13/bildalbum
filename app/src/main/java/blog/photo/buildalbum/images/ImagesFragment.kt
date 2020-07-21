/*
 * Copyright (c) 2018-2020 SAP and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 * Contributors:
 * SAP - initial API and implementation
 */
package blog.photo.buildalbum.images

import android.graphics.PorterDuff
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.ImageView
import android.widget.LinearLayout
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
import blog.photo.buildalbum.async.DownloadData
import blog.photo.buildalbum.async.DownloadJson
import blog.photo.buildalbum.async.DownloadStatus
import blog.photo.buildalbum.async.ImageSource
import blog.photo.buildalbum.data.Image
import blog.photo.buildalbum.databinding.ImagesFragBinding
import blog.photo.buildalbum.util.*
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import timber.log.Timber

/**
 * Display a grid of [Image]s.
 * User can choose to view all, added or edited images.
 * User can select one to view details.
 */
class ImagesFragment : Fragment(),
    DownloadData.OnDownloadComplete,
    DownloadJson.OnDataAvailable {

    private val args: ImagesFragmentArgs by navArgs()

    private var countNewCanvasesAvailable = 0

    private val viewModel by viewModels<ImagesViewModel> { getImageViewModelFactory() }

    private lateinit var viewDataBinding: ImagesFragBinding

    private lateinit var navCanvases: MenuItem

    private lateinit var listAdapter: ImagesAdapter

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        // Set the lifecycle owner to the lifecycle of the view
        viewDataBinding.lifecycleOwner = this.viewLifecycleOwner
        setupAddNew()
        setupListAdapter()
        setupNavigation()
        setupRefreshLayout(viewDataBinding.imagesRefreshLayout, viewDataBinding.imagesList)
        setupSnackBar()

        // Scroll grid to top
        val recyclerView: RecyclerView? = activity?.findViewById(R.id.images_list)
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

        viewDataBinding = ImagesFragBinding.inflate(inflater, container, false).apply {
            viewmodel = viewModel
        }

        setHasOptionsMenu(true)

        return viewDataBinding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.images_fragment_menu, menu)

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
        if (data.isNotEmpty())
            data.forEach {
                // Get source from first entry and skip it.
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
                if (!viewModel.exist(canvas))
                // Count available canvases for download
                    countNewCanvasesAvailable++
            }

        // Create notification on new canvases available.
        notifyNewCanvasesAvailable(
            requireActivity().application,
            navCanvases,
            countNewCanvasesAvailable
        )

        countNewCanvasesAvailable = 0
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
        // TODO onError Show snackBar message
        Timber.e(exception)
    }

    override fun onOptionsItemSelected(item: MenuItem) =
        when (item.itemId) {
            R.id.menu_clear_favorite_images -> {
                viewModel.clearFavoriteImages()
                true
            }
            R.id.menu_delete_edited -> {
                if (deleteImages(viewModel.editedImages.value!!))
                    viewModel.deleteEditedImages()

                true
            }
            R.id.menu_filter_all -> {
                viewModel.setFiltering(ImagesFilterType.ALL_IMAGES)
                true
            }
            R.id.menu_filter_added -> {
                viewModel.setFiltering(ImagesFilterType.ADDED_IMAGES)
                true
            }
            R.id.menu_filter_edited -> {
                viewModel.setFiltering(ImagesFilterType.EDITED_IMAGES)
                true
            }
            R.id.menu_filter_favorite_images -> {
                viewModel.setFiltering(ImagesFilterType.FAVORITE_IMAGES)
                true
            }
            R.id.menu_refresh_images -> {
                viewModel.loadImages(true)
                true
            }
            else -> false
        }

    override fun onResume() {
        super.onResume()
        checkInternet()
    }

    private fun checkInternet() {
        hasInternet().subscribe { hasInternet ->

            // Check for new canvases available
            if (hasInternet)
                download(this, ImageSource.CANVASES, requireActivity().getString(R.string.URI_API_CANVASES))
            else
                hideNotificationNewCanvasesAvailable(requireActivity().application, navCanvases)
        }
    }

    private fun navigateToAddNew() {
        findNavController().navigate(
            ImagesFragmentDirections
                .actionImagesFragmentToAddNewFragment()
        )
    }

    private fun openImageDetails(imageId: Int) {
        findNavController().navigate(
            ImagesFragmentDirections.actionImagesFragmentToImageDetailFragment(
                imageId, viewModel.isFavorite(imageId)
            )
        )
    }

    private fun setupAddNew() {
        activity?.findViewById<FloatingActionButton>(R.id.images_add_new_fab)?.setOnClickListener {
            navigateToAddNew()
        }
        activity?.findViewById<LinearLayout>(R.id.no_images_layout)?.setOnClickListener {
            navigateToAddNew()
        }
        activity?.findViewById<ImageView>(R.id.no_images_icon)?.setOnClickListener {
            navigateToAddNew()
        }
    }

    private fun setupListAdapter() {
        val viewModel = viewDataBinding.viewmodel
        if (viewModel != null) {
            listAdapter = ImagesAdapter(viewModel)
            viewDataBinding.imagesList.adapter = listAdapter
            viewDataBinding.imagesEditedList.adapter = listAdapter
            viewDataBinding.imagesCanvasesList.adapter = listAdapter
        } else {
            Timber.w("ViewModel not initialized when attempting to set up adapter.")
        }
    }

    private fun setupNavigation() {
        viewModel.openImageEvent.observe(viewLifecycleOwner, EventObserver {
            openImageDetails(it)
        })
        viewModel.newImageEvent.observe(viewLifecycleOwner, EventObserver {
            navigateToAddNew()
        })
    }

    private fun setupSnackBar() {
        view?.setupSnackBar(this, viewModel.snackbarText, Snackbar.LENGTH_LONG)
        arguments?.let {
            viewModel.showResultMessage(args.userMessage)

            if (args.userMessage == IMAGE_EDIT_RESULT_OK)
                AppRater.appLaunched(requireActivity())
        }
    }
}
