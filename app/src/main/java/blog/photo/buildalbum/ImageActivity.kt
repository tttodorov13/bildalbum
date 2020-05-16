package blog.photo.buildalbum

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.AdapterView
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import blog.photo.buildalbum.adapter.IconAdapter
import blog.photo.buildalbum.db.entity.Card
import blog.photo.buildalbum.db.entity.Image
import blog.photo.buildalbum.db.entity.Pane
import blog.photo.buildalbum.task.DownloadData
import blog.photo.buildalbum.task.DownloadSource
import kotlinx.android.synthetic.main.activity_image.*
import kotlinx.android.synthetic.main.spinner_layout.*
import java.io.File

/**
 * Class to manage the image screen.
 */
class ImageActivity : BaseActivity() {

    private val imageSize = 400
    private val imageSizeBorder = 100f
    private lateinit var image: Image
    private var pane: Pane = Pane(false, "", "")
    private var rotateIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image)

        // Initialize the image object
        image = Image(
            false,
            intent.extras!!.get("file").toString(),
            intent.extras!!.get("source").toString()
        )

        newImageView = signalImageView

        // Set the image view
        newImageView.setImageBitmap(
            BitmapFactory.decodeFile(
                File(
                    getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                    image.file
                ).canonicalPath
            )
        )

        // Display panes in an interesting layout
        panesGridView.isExpanded = true
        panesGridView.adapter = paneAdapter

        scrollToTop()

        // Click listener for Add Frame
        panesGridView.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ ->
                imageSetOriginal()

                // Copy pane from selection
                pane = paneAdapter.getPanes()[position]

                // Rotate image with current index
                var bitmap = bitmapRotate(rotateIndex)

                // Set current bitmap on image screen
                newImageView.setImageBitmap(bitmap)

                // Add current pane
                bitmap = getPaneBitmap(pane)

                // Set current bitmap on image screen
                newImageView.setImageBitmap(bitmap)

                // Save current image
                spinner_title.text = getString(R.string.progress_saving)
                CardSave().execute(Card(image.isEdited, image.file, image.source))
                image.isEdited = true
            }

        // Click listener for Scroll-to-Bottom Button
        fabBottomSingle.setOnClickListener {
            scrollToBottom()
        }

        // Click listener for edit button
        fabEdit.setOnClickListener {
            scrollToTop()

            val itemTexts = ArrayList<String>()
            val itemIcons = ArrayList<Int>()

            if (hasInternet) {
                itemTexts.add(getString(R.string.latest_panes))
                itemIcons.add(R.drawable.ic_download_24dp)
            } else
                toast(R.string.enable_internet)

            itemTexts.add(getString(R.string.rotate))
            itemIcons.add(R.drawable.ic_rotate_24dp)

            itemTexts.add(getString(R.string.share))
            itemIcons.add(R.drawable.ic_share_24dp)

            itemTexts.add(getString(R.string.delete))
            itemIcons.add(R.drawable.ic_delete_24dp)

            itemTexts.add(getString(R.string.close))
            itemIcons.add(R.drawable.ic_close_24dp)

            val adapter = IconAdapter(this, itemTexts, itemIcons)

            val alertDialog = AlertDialog.Builder(this)
                .setTitle(getString(R.string.image_edit))
                .setIcon(R.drawable.ic_edit_24dp)
            alertDialog.setAdapter(adapter) { dialog, item ->
                when (adapter.getItem(item)) {
                    getString(R.string.latest_panes) -> downloadPanes()
                    getString(R.string.rotate) -> imageRotate()
                    getString(R.string.share) -> imageShare()
                    getString(R.string.delete) -> imageDelete()
                    else -> dialog.dismiss()
                }
            }.show()
        }

        // Click listener for Scroll-to-Top Button
        fabTopSingle.setOnClickListener {
            scrollToTop()
        }
    }

    /**
     * OnRestoreInstanceState ImageActivity
     *
     * @param savedInstanceState
     */
    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        image = Image(
            savedInstanceState.getString("isEdited")!!.toBoolean(),
            savedInstanceState.getString("imageFile")!!,
            savedInstanceState.getString("imageSource")!!
        )
        newImageView.setImageBitmap(
            BitmapFactory.decodeFile(
                File(
                    getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                    image.file
                ).canonicalPath
            )
        )
        pane = Pane(
            false,
            savedInstanceState.getString("paneFile")!!,
            savedInstanceState.getString("paneSource")!!
        )
        rotateIndex = savedInstanceState.getString("rotateIndex")!!.toInt()
    }

    /**
     * OnSaveInstanceState ImageActivity
     *
     * @param outState
     */
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("imageFile", image.file)
        outState.putString("imageSource", image.source)
        outState.putString("isEdited", image.isEdited.toString())
        outState.putString("paneFile", pane.file)
        outState.putString("paneSource", pane.source)
        outState.putString("rotateIndex", rotateIndex.toString())
        super.onSaveInstanceState(outState)
    }

    /**
     * On image save task begin
     * disable all FABs and show progress bar spinner
     */
    override fun onTaskBegin() {
        super.onTaskBegin()
        progressSpinner.isGone = false
        fabBottomSingle.isEnabled = false
        fabBottomSingle.alpha = 0.1F
        fabEdit.isEnabled = false
        fabEdit.alpha = 0.1F
        fabTopSingle.isEnabled = false
        fabTopSingle.alpha = 0.1F
        panesGridView.isEnabled = false
        panesGridView.alpha = 0.1F
    }

    /**
     * On image save task complete
     * enable all FABs and hide progress bar spinner
     */
    override fun onTaskComplete(stringId: Int) {
        super.onTaskComplete(stringId)
        if (taskCountDown <= 0) {
            fabBottomSingle.isEnabled = true
            fabBottomSingle.alpha = 1.0F
            fabEdit.isEnabled = true
            fabEdit.alpha = 1.0F
            fabTopSingle.isEnabled = true
            fabTopSingle.alpha = 1.0F
            panesGridView.isEnabled = true
            panesGridView.alpha = 1.0F
            progressSpinner.isGone = true
        }
        toast(stringId)
    }


    /**
     * Method to rotate current image bitmap by 90 degrees clockwise
     *
     * @param index - times image to be rotated by 90 degrees
     * @return new bitmap to be displayed as image
     */
    private fun bitmapRotate(index: Int): Bitmap? {
        rotateIndex = index % 4
        val imageBitmap = getBitmapFromImageView()

        if (rotateIndex == 0)
            return imageBitmap

        val degrees = 90f * rotateIndex
        val matrix = Matrix()
        matrix.setRotate(degrees)

        return Bitmap.createBitmap(
            imageBitmap,
            0,
            0,
            imageBitmap.width,
            imageBitmap.height,
            matrix,
            true
        )
    }

    /**
     * Method to download latest panes.
     */
    private fun downloadPanes() {
        DownloadData(
            this,
            DownloadSource.FRAMES
        ).execute(getString(R.string.PANES_API_URI))
    }

    /**
     * Method to get a pane bitmap.
     *
     * @param pane  -   to be applied
     * @return the bitmap from pane file
     */
    private fun getPaneBitmap(pane: Pane): Bitmap? {
        val bitmap = getBitmapFromImageView()

        // Resize image to fit on the pane
        val bitmapEdited: Bitmap =
            // Check if the original image is too small to cut a square from it and just resize it
            if (imageSize >= bitmap.width || imageSize >= bitmap.height)
                Bitmap.createScaledBitmap(
                    bitmap,
                    imageSize,
                    imageSize,
                    false
                )
            // If the original image is large enough cut a square from it and resize it
            else {
                val bitmapCut =
                    if (bitmap.width >= bitmap.height) Bitmap.createBitmap(
                        bitmap,
                        bitmap.width / 2 - bitmap.height / 2,
                        0,
                        bitmap.height,
                        bitmap.height
                    )
                    else Bitmap.createBitmap(
                        bitmap,
                        0,
                        bitmap.height / 2 - bitmap.width / 2,
                        bitmap.width,
                        bitmap.width
                    )
                Bitmap.createScaledBitmap(
                    bitmapCut,
                    imageSize,
                    imageSize,
                    false
                )
            }

        // Get the bitmap frame to be applied
        val bitmapNew = BitmapFactory.decodeFile(
            File(
                getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                pane.file
            ).canonicalPath
        ).copy(
            Bitmap.Config.ARGB_8888,
            true
        )

        // Add the scaled image onto the frame
        Canvas(bitmapNew).drawBitmap(
            bitmapEdited,
            imageSizeBorder,
            imageSizeBorder,
            null
        )

        return bitmapNew
    }

    /**
     * Method to delete current image.
     */
    private fun imageDelete() {
        val alertDialog = AlertDialog.Builder(this)
            .setTitle(getString(R.string.delete))
            .setIcon(R.drawable.ic_delete_24dp).create()

        alertDialog.setButton(
            AlertDialog.BUTTON_NEGATIVE,
            getString(android.R.string.cancel),
            ContextCompat.getDrawable(
                this,
                R.drawable.ic_close_24dp
            )
        ) { dialog, _ ->
            toast(R.string.image_rescued)
            dialog.dismiss()
        }

        alertDialog.setButton(
            AlertDialog.BUTTON_POSITIVE,
            getString(android.R.string.ok),
            ContextCompat.getDrawable(
                this,
                R.drawable.ic_check_24dp
            )
        ) { _, _ ->
            imageViewModel.delete(image)
            val file = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), image.file)
            if (file.exists())
                file.delete()
            toast(R.string.image_deleted)
            finish()
        }

        alertDialog.show()

        val btnPositive = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
        val btnNegative = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE)

        val layoutParams = btnPositive.layoutParams as LinearLayout.LayoutParams
        layoutParams.weight = 10f
        btnPositive.layoutParams = layoutParams
        btnPositive.setTextColor(
            ContextCompat.getColor(
                this,
                R.color.colorYellow
            )
        )
        btnNegative.layoutParams = layoutParams
        btnNegative.setTextColor(
            ContextCompat.getColor(
                this,
                R.color.colorYellow
            )
        )
    }

    /**
     * Method to rotate current image.
     */
    private fun imageRotate() {
        imageSetOriginal()

        // Rotate image with current index +1
        var bitmap = bitmapRotate(++rotateIndex)

        // Set current bitmap on image screen
        newImageView.setImageBitmap(bitmap)

        // Check if pane is chosen
        if (pane.file != "") {
            bitmap = getPaneBitmap(pane)
            newImageView.setImageBitmap(bitmap)
        }

        // Save current image
        spinner_title.text = getString(R.string.progress_saving)
        CardSave().execute(Card(image.isEdited, image.file, image.source))
        image.isEdited = true
    }

    /**
     * Method to get image origin
     */
    private fun imageSetOriginal() {
        // Store image.file and image.source in temporary vars
        val imageFile = image.file
        val imageSource = image.source

        // If it is not first edition, get image original
        if (image.file != intent.extras!!.get("file").toString()) {
            image = Image(
                false,
                intent.extras!!.get("file").toString(),
                intent.extras!!.get("source").toString()
            )
        }

        // Set current bitmap on image screen
        newImageView.setImageBitmap(
            BitmapFactory.decodeFile(
                File(
                    getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                    image.file
                ).canonicalPath
            )
        )

        // If it is not first edition, get saved image
        image = if (imageFile != intent.extras!!.get("file").toString())
            Image(true, imageFile, imageSource)
        // Else create new image
        else
            Image(false, "".plus(System.nanoTime()).plus(".png"), application.packageName)
    }

    /**
     * Method to share current image on Facebook.
     */
    private fun imageShare() {
        var intent = Intent(Intent.ACTION_SEND)
        intent.type = "image/*"
        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION

        intent.putExtra(
            Intent.EXTRA_STREAM,
            Uri.fromFile(
                File(
                    getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                    image.file
                )
            )
        )

        // See if official Facebook app is found
        var facebookAppFound = false
        val matches = packageManager.queryIntentActivities(intent, 0)
        for (info in matches) {
            if (info.activityInfo.packageName.equals(
                    getString(
                        R.string.com_facebook_katana
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
                Uri.parse(getString(R.string.facebook_sharer).plus(getString(R.string.app_web)))
            )
            toast(R.string.install_facebook_for_optimal_experience)
        }

        startActivity(intent)
    }

    /**
     * Method to scroll to screen top.
     */
    private fun scrollToBottom() {
        screen.fullScroll(View.FOCUS_DOWN)
    }

    /**
     * Method to scroll to screen top.
     */
    private fun scrollToTop() {
        screen.smoothScrollTo(0, 0)
    }
}