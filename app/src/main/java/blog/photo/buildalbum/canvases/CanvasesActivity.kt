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

import android.app.Activity
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.NavGraph
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import blog.photo.buildalbum.R
import com.google.android.material.navigation.NavigationView

/**
 * Canvases activity for the app.
 * Holds the canvases information.
 */
class CanvasesActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.images_act)
        setupNavigationDrawer()
        setSupportActionBar(findViewById(R.id.images_toolbar))

        val navController: NavController = findNavController(R.id.images_nav_host_fragment)

        val graph: NavGraph = navController.navInflater.inflate(R.navigation.images_nav_graph)

        graph.startDestination = (R.id.canvases_fragment_dest)

        navController.graph = graph

        appBarConfiguration =
            AppBarConfiguration.Builder(
                R.id.images_fragment_dest,
                R.id.statistics_fragment_dest,
                R.id.canvases_fragment_dest
            )
                .setDrawerLayout(drawerLayout)
                .build()

        setupActionBarWithNavController(navController, appBarConfiguration)

        findViewById<NavigationView>(R.id.albumbuild_nav_view)
            .setupWithNavController(navController)
    }

    override fun onSupportNavigateUp(): Boolean {
        return findNavController(R.id.images_nav_host_fragment).navigateUp(appBarConfiguration) ||
                super.onSupportNavigateUp()
    }

    private fun setupNavigationDrawer() {
        drawerLayout = (findViewById<DrawerLayout>(R.id.image_drawer_layout))
            .apply {
                setStatusBarBackground(R.color.colorPrimaryDark)
            }
    }
}

// Keys for navigation
const val CANVAS_ADD_RESULT_OK = Activity.RESULT_FIRST_USER + 4
const val CANVAS_SAVE_RESULT_OK = Activity.RESULT_FIRST_USER + 5
const val CANVAS_DELETE_RESULT_OK = Activity.RESULT_FIRST_USER + 6
const val CANVAS_DOWNLOAD_RESULT_CANCEL = Activity.RESULT_FIRST_USER + 11