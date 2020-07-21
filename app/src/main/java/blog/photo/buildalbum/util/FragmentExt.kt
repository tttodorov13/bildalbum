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

/**
 * Extension functions for Fragment.
 */

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.MenuItem
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.Fragment
import blog.photo.buildalbum.*
import blog.photo.buildalbum.async.DownloadData
import blog.photo.buildalbum.async.ImageSource
import blog.photo.buildalbum.canvases.CanvasesActivity
import blog.photo.buildalbum.data.Image
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.util.*

const val notificationId = 8888

fun Fragment.getCanvasViewModelFactory(): CanvasViewModelFactory {
    val imagesRepository =
        (requireContext().applicationContext as BuildAlbumApplication).imageRepository
    return CanvasViewModelFactory(imagesRepository)
}

fun Fragment.getImageViewModelFactory(): ImageViewModelFactory {
    val imagesRepository =
        (requireContext().applicationContext as BuildAlbumApplication).imageRepository
    return ImageViewModelFactory(imagesRepository)
}

fun Fragment.getLaunchViewModelFactory(): LaunchViewModelFactory {
    val imagesRepository =
        (requireContext().applicationContext as BuildAlbumApplication).imageRepository
    return LaunchViewModelFactory(imagesRepository)
}

/**
 * Download from Internet.
 *
 * @param listener class to return to
 * @param imageSource predefined enum
 * @param url predefined resource string
 */
internal fun download(
    listener: DownloadData.OnDownloadComplete,
    imageSource: ImageSource,
    url: String
) {
    DownloadData(
        listener,
        imageSource
    ).execute(url)
}

/**
 * Delete file on device.
 *
 * @param imageList to be deleted
 */
fun deleteImages(imageList: List<Image>): Boolean {
    var exists = false
    imageList.forEach { image ->
        exists = try {
            val file = File(image.file)

            if (file.exists())
                file.delete()

            // File is successfully deleted.
            true
        } catch (e: FileNotFoundException) {
            Timber.e(e.message)

            // File is not found.
            true
        }
    }
    return exists
}

/**
 * Check for connection to app server.
 */
fun Fragment.hasInternet(): Single<Boolean> {
    return Single.fromCallable {
        try {
            val timeoutMs = 100
            val socket = Socket()
            val socketAddress = InetSocketAddress(
                getString(R.string.app_web_host),
                getString(R.string.app_web_port).toInt()
            )

            socket.connect(socketAddress, timeoutMs)
            socket.close()

            true
        } catch (e: IOException) {
            false
        }
    }
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
}

/**
 * Hide notification on new canvases available.
 *
 * @param mContext of the current activity
 * @param menuItem's title to be updated
 */
fun hideNotificationNewCanvasesAvailable(mContext: Context, menuItem: MenuItem) {
    menuItem.title = mContext.getString(R.string.canvases)

    // Cancel notification
    with(NotificationManagerCompat.from(mContext)) {
        // notificationId is a unique int for each notification that you must define
        cancel(notificationId)
    }
}

/**
 * Create notification on new canvases available.
 *
 * @param mContext of the current activity
 * @param menuItem's title to be updated
 * @param countNewCanvasesAvailable to be downloaded
 */
fun notifyNewCanvasesAvailable(
    mContext: Context,
    menuItem: MenuItem,
    countNewCanvasesAvailable: Int
) {
    // Create channel id for API 26+
    val channelId = mContext.applicationInfo.packageName

    // Create the NotificationChannel, but only on API 26+ because
    // the NotificationChannel class is new and not in the support library
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val name = mContext.getString(R.string.app_name)
        val descriptionText = mContext.getString(R.string.app_info)
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(channelId, name, importance).apply {
            description = descriptionText
        }
        // Register the channel with the system
        val notificationManager: NotificationManager =
            mContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    // Create an explicit intent for an Activity in the app
    val intent = Intent(mContext, CanvasesActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }

    val pendingIntent: PendingIntent =
        PendingIntent.getActivity(mContext, 0, intent, 0)

    var builder = NotificationCompat.Builder(mContext, channelId)
        .setSmallIcon(R.drawable.ic_camera_alt_96dp)
        .setLargeIcon(
            BitmapFactory.decodeResource(
                mContext.resources,
                R.drawable.ic_camera_alt_96dp
            )
        )
        .setContentTitle(mContext.getString(R.string.add_new))
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        // Set the intent that will fire when the user taps the notification
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
        builder.setSmallIcon(R.mipmap.ic_launcher)
            .setLargeIcon(
                BitmapFactory.decodeResource(
                    mContext.resources,
                    R.mipmap.ic_launcher
                )
            )

    // set title to the MenuItem new canvases available
    if (countNewCanvasesAvailable > 0) {
        val text =
            String.format(
                "%s +%d %s",
                mContext.getString(R.string.canvases),
                countNewCanvasesAvailable,
                mContext.getString(R.string.new_to_add)
            )

        menuItem.title = text

        builder
            .setContentText(text)

        // Show notification
        with(NotificationManagerCompat.from(mContext)) {
            // notificationId is a unique int for each notification that you must define
            notify(notificationId, builder.build())
        }
    } else {
        menuItem.title = mContext.getString(R.string.canvases)

        // Cancel notification
        with(NotificationManagerCompat.from(mContext)) {
            // notificationId is a unique int for each notification that you must define
            cancel(notificationId)
        }
    }
}

/**
 * Convert image List to Array to be sent as parameter for [ImageSave].
 *
 * @param list of images to be converted to array
 */
fun imageListToArray(list: List<Image>): Array<Image?> {
    val arrayList = ArrayList<Image>()
    arrayList.addAll(list)
    val array = arrayOfNulls<Image>(list.size)
    return arrayList.toArray(array)
}

/**
 * Open Permission Manager for the app.
 *
 * @param mContext of the current activity
 */
fun openPermissionManager(mContext: Context) {
    val intent = Intent()
    intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
    val uri: Uri = Uri.fromParts("package", mContext.packageName, null)
    intent.data = uri
    mContext.startActivity(intent)
}

/**
 * Open Network Manager.
 *
 * @param mContext of the current activity
 */
fun openNetworkManager(mContext: Context) {
    val intent = Intent()
    intent.action = Settings.ACTION_AIRPLANE_MODE_SETTINGS
    mContext.startActivity(intent)
}

/**
 * Write image on the file system.
 *
 * @param image     -   image file to be written to
 * @param bitmap    -   bitmap to be applied on image
 *
 * @return true if completed successfully
 */
fun writeOnFileSystem(image: Image, bitmap: Bitmap?): Boolean {
    val compressQuality = 100
    val file = File(image.file)

    if (file.exists())
        file.delete()

    return try {
        val out = FileOutputStream(file)
        bitmap?.compress(Bitmap.CompressFormat.PNG, compressQuality, out)
        out.flush()
        out.close()
        true
    } catch (e: IOException) {
        Timber.e(e)
        false
    }
}