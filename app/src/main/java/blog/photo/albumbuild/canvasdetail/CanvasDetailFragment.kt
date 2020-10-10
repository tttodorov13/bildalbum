/*
 * Copyright (c) 2018-2020 SAP and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 * Contributors:
 * SAP - initial API and implementation
 */
package blog.photo.albumbuild.canvasdetail

import android.graphics.PorterDuff
import android.os.Build
import android.os.Bundle
import android.view.*
import androidx.core.content.ContextCompat.getColor
import androidx.core.view.forEach
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import blog.photo.albumbuild.EventObserver
import blog.photo.albumbuild.R
import blog.photo.albumbuild.canvases.CANVAS_DELETE_RESULT_OK
import blog.photo.albumbuild.canvases.CANVAS_SAVE_RESULT_OK
import blog.photo.albumbuild.data.Image
import blog.photo.albumbuild.databinding.CanvasdetailFragBinding
import blog.photo.albumbuild.util.deleteImages
import blog.photo.albumbuild.util.getImageViewModelFactory
import blog.photo.albumbuild.util.setupSnackBar
import com.google.android.material.snackbar.Snackbar

/**
 * Main UI for the canvas detail screen.
 */
class CanvasDetailFragment : Fragment() {

    private lateinit var menuItemFavorite: MenuItem

    private lateinit var viewDataBinding: CanvasdetailFragBinding

    private val args: CanvasDetailFragmentArgs by navArgs()

    private val viewModel by viewModels<CanvasDetailViewModel> { getImageViewModelFactory() }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        setupNavigation()
        view?.setupSnackBar(this, viewModel.snackbarText, Snackbar.LENGTH_LONG)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.canvasdetail_frag, container, false)
        viewDataBinding = CanvasdetailFragBinding.bind(view).apply {
            viewmodel = viewModel
        }
        viewDataBinding.lifecycleOwner = this.viewLifecycleOwner

        viewModel.start(args.imageId)

        setHasOptionsMenu(true)

        return view
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.canvasdetail_fragment_menu, menu)

        menu.forEach { menuItem ->

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

        menuItemFavorite = menu.findItem(R.id.menu_favorite_canvas)
        setupFavorite(args.isFavorite)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_delete_canvas -> {
                val canvasList = ArrayList<Image>()
                canvasList.add(viewModel.canvas.value!!)

                if (deleteImages(canvasList))
                    viewModel.deleteCanvas()

                false
            }
            R.id.menu_favorite_canvas -> {
                viewModel.favoriteCanvas()
                true
            }
            else -> false
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

    private fun setupNavigation() {
        viewModel.canvasDeleteEvent.observe(viewLifecycleOwner, EventObserver {
            findNavController().navigate(
                CanvasDetailFragmentDirections
                    .actionCanvasDetailFragmentToCanvasesFragment(CANVAS_DELETE_RESULT_OK)
            )
        })
        viewModel.canvasFavoriteEvent.observe(viewLifecycleOwner, EventObserver {
            setupFavorite(it)
        })
        viewModel.canvasSaveEvent.observe(viewLifecycleOwner, EventObserver {
            findNavController().navigate(
                CanvasDetailFragmentDirections
                    .actionCanvasDetailFragmentToCanvasesFragment(CANVAS_SAVE_RESULT_OK)
            )
        })
    }
}
