package blog.photo.buildalbum

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.widget.AdapterView
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import blog.photo.buildalbum.R.string.*
import blog.photo.buildalbum.adapters.ImagesAdapter
import blog.photo.buildalbum.model.Image
import kotlinx.android.synthetic.main.activity_image.*

/**
 * Class to manage the picture screen.
 */
class ImageActivity : BaseActivity() {

    private lateinit var frame: Image
    private lateinit var image: Image
    private var rotateIndex = 0

    /**
     * A companion object for static variables
     */
    companion object {
        private const val IMAGE_SIZE = 400
        private const val IMAGE_SIZE_BORDER = 100F
        private lateinit var framesAdapter: ImagesAdapter
        private lateinit var imageNewView: ImageView

        /**
         * Method to get a bitmap from the new image
         */
        internal fun getBitmapFromImageView(): Bitmap {
            return (imageNewView.drawable as BitmapDrawable).bitmap
        }
    }

    /**
     * OnCreate ImageActivity
     *
     * @param savedInstanceState
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image)

        // Set ImageView to be used for saving changes
        imageNewView = imageViewCamera

        // Initialize the image object
        image = Image(
            this,
            intent.extras!!.get("originalName").toString(),
            intent.extras!!.get("originalOrigin").toString()
        )

        // Initialize the frame object
        frame = Image(this, true, "", "")

        // Set new Image URI
        imageViewCamera.setImageBitmap(image.bitmap)

        // Display frames to be added
        framesAdapter = ImagesAdapter(
            this, frames
        )
        gridViewFrames.isExpanded = true
        gridViewFrames.adapter = framesAdapter

        // Scroll to screen top
        screen.smoothScrollTo(0, 0)

        // Click listener for Scroll-to-Top Button
        fabTop.setOnClickListener {
            screen.smoothScrollTo(0, 0)
        }

        // Click listener for Share Button
        fabShare.setOnClickListener {
            var intent = Intent(Intent.ACTION_SEND)
            intent.type = "image/*"
            intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION

            intent.putExtra(
                Intent.EXTRA_STREAM,
                Uri.fromFile(image.file)
            )

            // See if official Facebook app is found
            var facebookAppFound = false
            val matches = packageManager.queryIntentActivities(intent, 0)
            for (info in matches) {
                if (info.activityInfo.packageName.equals(
                        getString(
                            com_facebook_katana
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
                    Uri.parse(getString(facebook_sharer).plus(getString(app_web)))
                )
                toast(getString(install_facebook_for_optimal_experience))
            }

            startActivity(intent)
        }

        // Click listener for Rotate Button
        fabRotate.setOnClickListener {
            imageSetOriginalView()

            // Rotate image with current index +1
            var bitmap = imageRotate(++rotateIndex)

            // Set current bitmap on image screen
            imageViewCamera.setImageBitmap(bitmap)

            // Check if frame is applied
            if (frame.name != "") {
                bitmap = imageAddFrame(frame)
                imageViewCamera.setImageBitmap(bitmap)
            }

            // Save current image
            ImageSave(true, image).execute()
        }

        // Click listener for Add Frame
        gridViewFrames.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ ->
                imageSetOriginalView()

                // Copy frame from selection
                frame = frames[position]

                // Rotate image with current index
                var bitmap = imageRotate(rotateIndex)

                // Set current bitmap on image screen
                imageViewCamera.setImageBitmap(bitmap)

                // Add current frame
                bitmap = imageAddFrame(frame)

                // Set current bitmap on image screen
                imageViewCamera.setImageBitmap(bitmap)

                // Save current image
                ImageSave(true, image).execute()
            }

        // Click listener for Delete Button
        fabDelete.setOnClickListener {
            val alertDialog = AlertDialog.Builder(this, R.style.BuildAlbumAlertDialog)
                .setTitle(getString(image_delete))
                .setIcon(android.R.drawable.ic_menu_delete).create()

            alertDialog.setButton(
                AlertDialog.BUTTON_NEGATIVE,
                getString(android.R.string.cancel),
                ContextCompat.getDrawable(
                    this,
                    android.R.drawable.ic_delete
                )
            ) { dialog, _ ->
                toast(getString(image_rescued))
                dialog.dismiss()
            }

            alertDialog.setButton(
                AlertDialog.BUTTON_POSITIVE,
                getString(android.R.string.ok),
                ContextCompat.getDrawable(
                    this,
                    android.R.drawable.checkbox_on_background
                )
            ) { _, _ ->
                image.delete()
                toast(getString(image_deleted))
                finish()
            }

            alertDialog.show()

            val btnPositive = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
            val btnNegative = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE)

            val layoutParams = btnPositive.layoutParams as LinearLayout.LayoutParams
            layoutParams.weight = 10f
            btnPositive.layoutParams = layoutParams
            btnNegative.layoutParams = layoutParams
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
            this,
            savedInstanceState.getString("imageName")!!,
            savedInstanceState.getString("imageOrigin")!!
        )
        frame = Image(
            this,
            true,
            savedInstanceState.getString("frameName")!!,
            savedInstanceState.getString("frameOrigin")!!
        )
        imageViewCamera.setImageBitmap(image.bitmap)
    }

    /**
     * OnSaveInstanceState ImageActivity
     *
     * @param outState
     */
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("imageName", image.name)
        outState.putString("imageOrigin", image.origin)
        outState.putString("frameName", frame.name)
        outState.putString("frameOrigin", frame.origin)
        super.onSaveInstanceState(outState)
    }

    /**
     * Method to get image origin
     */
    private fun imageSetOriginalView() {
        // Store image.name in temporary var
        val imageName = image.name

        // If it is not first edition, get image original
        if (image.name != intent.extras!!.get("originalName").toString()) {
            image = Image(
                this,
                intent.extras!!.get("originalName").toString(),
                intent.extras!!.get("originalOrigin").toString()
            )
        }

        // Set current bitmap on image screen
        imageViewCamera.setImageBitmap(image.bitmap)

        // If it is not first edition, get saved image
        image = if (imageName != intent.extras!!.get("originalName").toString())
            Image(this, imageName, getString(app_name))
        // Else create new image
        else
            Image(this, false, getString(app_name))
    }

    /**
     * Method to add a frame
     *
     * @param frame to be applied
     * @return new bitmap to be displayed as image
     */
    private fun imageAddFrame(frame: Image): Bitmap? {
        val bitmap = getBitmapFromImageView()

        // Resize image to fit in the frame
        var bitmapEdited: Bitmap =
            // Check if the original image is too small to cut a square from it and just resize it
            if (IMAGE_SIZE >= bitmap.width || IMAGE_SIZE >= bitmap.height)
                Bitmap.createScaledBitmap(
                    bitmap,
                    IMAGE_SIZE,
                    IMAGE_SIZE,
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
                    IMAGE_SIZE,
                    IMAGE_SIZE,
                    false
                )
            }

        // Get the bitmap frame to be applied
        val bitmapNew = BitmapFactory.decodeFile(frame.filePath).copy(
            Bitmap.Config.ARGB_8888,
            true
        )

        // Add the scaled image onto the frame
        Canvas(bitmapNew).drawBitmap(
            bitmapEdited,
            IMAGE_SIZE_BORDER,
            IMAGE_SIZE_BORDER,
            null
        )

        return bitmapNew
    }

    /**
     * Method to rotate by 90 degrees clockwise
     *
     * @return new bitmap to be displayed as image
     */
    private fun imageRotate(index: Int): Bitmap? {
        val imageBitmap = getBitmapFromImageView()
        if (index == 0)
            return imageBitmap

        rotateIndex == index % 4
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

    override fun onTaskBegin() {
        super.onTaskBegin()
        progressBarImageScreen.isGone = false
        fabTop.isEnabled = false
        fabShare.isEnabled = false
        fabRotate.isEnabled = false
        fabDelete.isEnabled = false
    }

    override fun onTaskComplete(stringId: Int) {
        super.onTaskComplete(stringId)
        if(taskCountDown <= 0) {
            progressBarImageScreen.isGone = true
            fabTop.isEnabled = true
            fabShare.isEnabled = true
            fabRotate.isEnabled = true
            fabDelete.isEnabled = true
        }
        toast(getString(stringId))
    }
}
