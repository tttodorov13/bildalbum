/*
 * Copyright (c) 2018-2020 SAP and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 * Contributors:
 * SAP - initial API and implementation
 */
package blog.photo.albumbuild.statistics

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.databinding.DataBindingUtil.inflate
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import blog.photo.albumbuild.R
import blog.photo.albumbuild.databinding.StatisticsFragBinding
import blog.photo.albumbuild.util.getImageViewModelFactory
import blog.photo.albumbuild.util.setupRefreshLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton

/**
 * Main UI for the statistics screen.
 */
class StatisticsFragment : Fragment() {

    private lateinit var viewDataBinding: StatisticsFragBinding

    private val viewModel by viewModels<StatisticsViewModel> { getImageViewModelFactory() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewDataBinding = inflate(
            inflater, R.layout.statistics_frag, container,
            false
        )
        return viewDataBinding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewDataBinding.viewmodel = viewModel
        viewDataBinding.lifecycleOwner = this.viewLifecycleOwner
        setupAddNew()
        setupRefreshLayout(viewDataBinding.imageStatisticsRefreshLayout)
    }

    private fun navigateToAddNew() {
        findNavController().navigate(
            StatisticsFragmentDirections
                .actionStatisticsFragmentToAddNewFragment()
        )
    }

    private fun setupAddNew() {
        activity?.findViewById<FloatingActionButton>(R.id.image_statistics_add_new_fab)?.let {
            it.setOnClickListener {
                navigateToAddNew()
            }
        }
        activity?.findViewById<LinearLayout>(R.id.image_statistics_no_images_layout)?.let {
            it.setOnClickListener {
                navigateToAddNew()
            }
        }
        activity?.findViewById<ImageView>(R.id.image_statistics_no_images_icon)?.let {
            it.setOnClickListener {
                navigateToAddNew()
            }
        }
    }
}
