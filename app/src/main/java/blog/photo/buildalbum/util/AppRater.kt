/*
 * Copyright (c) 2018-2020 SAP and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 * Contributors:
 * SAP - initial API and implementation
 */
package blog.photo.buildalbum.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences.Editor
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Build
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import blog.photo.buildalbum.R

/**
 * Display an AlertDialog to Rate the App.
 * Show popup on every 3rd image edited.
 * Wait 1 day until first alert.
 */
object AppRater {
    // Min number of days
    private const val DAYS_UNTIL_PROMPT = 1

    // Min number of launches
    private const val LAUNCHES_UNTIL_PROMPT = 3

    fun appLaunched(mContext: Context) {
        val prefs = mContext.getSharedPreferences("apprater", 0)
        if (prefs.getBoolean("dontshowagain", false)) {
            return
        }
        val editor = prefs.edit()

        // Increment launch counter
        val launchCount = prefs.getLong("launch_count", 0) + 1
        editor.putLong("launch_count", launchCount)

        // Get date of first launch
        var dateFirstLaunch = prefs.getLong("date_firstlaunch", 0)
        if (dateFirstLaunch == 0L) {
            dateFirstLaunch = System.currentTimeMillis()
            editor.putLong("date_firstlaunch", dateFirstLaunch)
        }

        // Wait at least n days and n times to open before alert
        if ((launchCount.toInt() % LAUNCHES_UNTIL_PROMPT).equals(0)) {
            if (System.currentTimeMillis() >= dateFirstLaunch +
                DAYS_UNTIL_PROMPT * 24 * 60 * 60 * 1000
            ) {
                showRateDialog(mContext, editor)
            }
        }
        editor.commit()
    }

    private fun showRateDialog(mContext: Context, editor: Editor?) {
        val alertDialog: AlertDialog? = mContext.let {
            val builder = AlertDialog.Builder(it, R.style.Popup)

            // Get resources from context
            val res = mContext.resources

            val title = res.getString(R.string.rate) + " " + res.getString(R.string.app_name)

            builder.setIcon(R.drawable.ic_camera_24dp)
                .setTitle(title)

            // Get the layout inflater
            val inflater = (mContext as Activity).layoutInflater;

            val negativeButtonIcon = ContextCompat.getDrawable(
                mContext,
                R.drawable.ic_close_24dp
            )

            val positiveButtonIcon = ContextCompat.getDrawable(
                mContext,
                R.drawable.ic_check_24dp
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                negativeButtonIcon?.setTint(
                    ContextCompat.getColor(
                        mContext,
                        R.color.colorSecondary
                    )
                )
                positiveButtonIcon?.setTint(
                    ContextCompat.getColor(
                        mContext,
                        R.color.colorSecondary
                    )
                )
            } else {
                negativeButtonIcon?.setColorFilter(
                    ContextCompat.getColor(
                        mContext,
                        R.color.colorSecondary
                    ),
                    PorterDuff.Mode.SRC_IN
                )
                positiveButtonIcon?.setColorFilter(
                    ContextCompat.getColor(
                        mContext,
                        R.color.colorSecondary
                    ),
                    PorterDuff.Mode.SRC_IN
                )
            }

            builder.apply {
                setView(inflater.inflate(R.layout.dialog_rate, null))
                setPositiveButton(
                    /**
                     * The color is set by the system [R.color.colorAccent] is just for reference.
                     */
                    HtmlCompat.fromHtml(
                        "<font color='@color/colorAccent'>" + res.getString(R.string.ok) + "</font>",
                        HtmlCompat.FROM_HTML_MODE_LEGACY
                    )
                ) { dialog, _ ->
                    mContext.startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("market://details?id=${mContext.packageName}")
                        )
                    )
                    dialog.dismiss()
                }
                setPositiveButtonIcon(
                    positiveButtonIcon
                )
                setNegativeButton(
                    /**
                     * The color is set by the system [R.color.colorAccent] is just for reference.
                     */
                    HtmlCompat.fromHtml(
                        "<font color='@color/colorAccent'>" + res.getString(R.string.no_thanks) + "</font>",
                        HtmlCompat.FROM_HTML_MODE_LEGACY
                    )
                ) { dialog, _ ->
                    if (editor != null) {
                        editor.putBoolean("dontshowagain", true)
                        editor.commit()
                    }
                    dialog.dismiss()
                }
                setNegativeButtonIcon(
                    negativeButtonIcon
                )
                setNeutralButton(
                    /**
                     * The color is set by the system [R.color.colorAccent] is just for reference.
                     */
                    HtmlCompat.fromHtml(
                        "<font color='@color/colorAccent'>" + res.getString(R.string.remind_me_later) + "</font>",
                        HtmlCompat.FROM_HTML_MODE_LEGACY
                    )
                ) { dialog, _ ->
                    dialog.dismiss()
                }
            }

            // Create the AlertDialog
            builder.create()
        }

        alertDialog?.show()

        alertDialog?.findViewById<ImageView>(android.R.id.icon)?.setColorFilter(
            ContextCompat.getColor(
                mContext,
                R.color.colorSecondary
            ),
            PorterDuff.Mode.SRC_IN
        )
    }
}