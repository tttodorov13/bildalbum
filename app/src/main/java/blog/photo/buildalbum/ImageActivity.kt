package blog.photo.buildalbum

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.widget.AdapterView
import android.widget.ImageView
import androidx.core.view.isGone
import blog.photo.buildalbum.R.string.*
import blog.photo.buildalbum.model.Image
import blog.photo.buildalbum.tasks.SaveImage
import blog.photo.buildalbum.utils.ImagesAdapter
import kotlinx.android.synthetic.main.activity_image.*

/**
 * Class to manage the picture screen.
 */
class ImageActivity : BaseActivity() {

    private var imageNewName: String = ""
    private lateinit var imageNew: Image
    private lateinit var imageOriginal: Image

    /**
     * A companion object for static variables
     */
    companion object {
        private const val IMAGE_SIZE = 400
        private const val IMAGE_SIZE_BORDER = 100F
        private lateinit var framesAdapter: ImagesAdapter
        private lateinit var imageNewView: ImageView

        /**
         * Method to get a bitmap from the new image ImageView
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

        imageNew = Image(this)
        imageNewView = imageView
        imageOriginal = Image(
            this,
            intent.extras!!.get("imageOriginalName").toString(),
            intent.extras!!.get("imageOriginalOrigin").toString()
        )
        imageViewImageOriginal.setImageURI(
            imageOriginal.uri
        )

        framesAdapter = ImagesAdapter(
            this, frames
        )
        gridViewFrames.isExpanded = true
        gridViewFrames.adapter = framesAdapter

        imageScreenScroll.smoothScrollTo(0, 0)

        // Click listener for Add Frame
        gridViewFrames.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ ->
                val bitmapNew = addFrame(
                    imageOriginal,
                    frames[position]
                )
                imageView.setImageBitmap(bitmapNew)
                imageView.isGone = false
                imageViewImageOriginal.isGone = true

                if (imageNewName != "")
                    imageNew = Image(this, imageNewName, getString(app_name))
                else
                    imageNewName = imageNew.name

                SaveImage(this, imageNew).execute()
                toast(getString(image_saved))
            }

        // Click listener for Share Button
        buttonShare.setOnClickListener {
            var intent = Intent(Intent.ACTION_SEND)
            intent.type = "image/*"
            intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION

            if (imageNewName != "")
                intent.putExtra(
                    Intent.EXTRA_STREAM,
                    Uri.fromFile(imageNew.file)
                )
            else
                intent.putExtra(
                    Intent.EXTRA_STREAM,
                    Uri.fromFile(imageOriginal.file)
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

        // Click listener for Delete Button
        buttonDelete.setOnClickListener {
            if (imageViewImageOriginal.isGone) {
                imageNew.delete()
            } else {
                imageOriginal.delete()
            }
            toast(getString(image_deleted))
            finish()
        }
    }

    /**
     * OnRestoreInstanceState ImageActivity
     *
     * @param savedInstanceState
     */
    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        imageNewName = savedInstanceState.getString("imageNewName")!!
        if (imageNewName != "") {
            imageNew = Image(this, imageNewName)
            imageView.setImageURI(imageNew.uri)
            imageView.isGone = false
            imageViewImageOriginal.isGone = true
        }
    }

    /**
     * OnSaveInstanceState ImageActivity
     *
     * @param outState
     */
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("imageNewName", imageNewName)
        super.onSaveInstanceState(outState)
    }

    /**
     * Method to add a frame
     */
    private fun addFrame(image: Image, frame: Image): Bitmap? {
        val imageBitmap = BitmapFactory.decodeFile(image.file.canonicalPath)

        // Resize image to fit in the frame
        var editedBitmap: Bitmap =
            // Check if the original image is too small to cut a square from it
            if (IMAGE_SIZE >= imageBitmap.width || IMAGE_SIZE >= imageBitmap.height)
                Bitmap.createScaledBitmap(
                    imageBitmap,
                    IMAGE_SIZE,
                    IMAGE_SIZE,
                    false
                )
            // Check if the original image is large enough to cut a square from it
            else {
                val cutBitmap =
                    if (imageBitmap.width >= imageBitmap.height) Bitmap.createBitmap(
                        imageBitmap,
                        imageBitmap.width / 2 - imageBitmap.height / 2,
                        0,
                        imageBitmap.height,
                        imageBitmap.height
                    )
                    else Bitmap.createBitmap(
                        imageBitmap,
                        0,
                        imageBitmap.height / 2 - imageBitmap.width / 2,
                        imageBitmap.width,
                        imageBitmap.width
                    )
                Bitmap.createScaledBitmap(
                    cutBitmap,
                    IMAGE_SIZE,
                    IMAGE_SIZE,
                    false
                )
            }

        // Get the bitmap frame to be applied
        val imageNewBitmap = BitmapFactory.decodeFile(frame.file.canonicalPath).copy(
            Bitmap.Config.ARGB_8888,
            true
        )

        // Add the scaled image onto the frame
        Canvas(imageNewBitmap).drawBitmap(
            editedBitmap,
            IMAGE_SIZE_BORDER,
            IMAGE_SIZE_BORDER,
            null
        )

        return imageNewBitmap
    }
}
