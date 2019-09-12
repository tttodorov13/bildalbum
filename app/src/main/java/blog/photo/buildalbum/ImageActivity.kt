package blog.photo.buildalbum

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log.e
import android.widget.AdapterView
import android.widget.ImageView
import androidx.core.view.isGone
import blog.photo.buildalbum.R.string.*
import blog.photo.buildalbum.model.Image
import blog.photo.buildalbum.utils.BuildAlbumDBOpenHelper
import blog.photo.buildalbum.utils.PicturesAdapter
import kotlinx.android.synthetic.main.activity_image.*
import java.io.FileOutputStream
import java.io.IOException

/**
 * Class to manage the picture screen.
 */
// TODO: Make this class Fragment to MainActivity
class ImageActivity : BaseActivity() {

    private var imageNewName: String = ""
    private lateinit var imageNew: Image
    private lateinit var imageOriginal: Image

    /**
     * A companion object for static variables
     */
    companion object {
        private const val tag = "ImageActivity"
        private const val IMAGE_SIZE = 400
        private const val IMAGE_SIZE_BORDER = 100F
        private lateinit var framesAdapter: PicturesAdapter
    }

    /**
     * OnCreate ImageActivity
     *
     * @param savedInstanceState
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image)

        imageNew = Image(this, "img".plus(System.nanoTime()).plus(".png"))
        imageOriginal = Image(
            this,
            intent.extras!!.get("imageOriginalName").toString(),
            intent.extras!!.get("imageOriginalOrigin").toString()
        )
        imageViewImageOriginal.setImageURI(
            imageOriginal.uri
        )

        framesAdapter = PicturesAdapter(
            this, frames
        )
        // TODO: Show gridViewFrames on 1 line
        gridViewFrames.isExpanded = true
        gridViewFrames.adapter = framesAdapter

        imageScreenScroll.smoothScrollTo(0, 0)

        // Click listener for add frame
        gridViewFrames.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ ->
                val bitmapNew = addFrame(
                    imageOriginal,
                    frames[position]
                )
                imageView.setImageBitmap(bitmapNew)
                imageView.isGone = false
                imageViewImageOriginal.isGone = true
                if (!imageNewName.isBlank())
                    imageNew = Image(this, imageNewName)
                else
                    imageNewName = imageNew.name
                SavePicture(imageNew).execute()
            }

        // Click listener for share button
        buttonShare.setOnClickListener {
            var intent = Intent(Intent.ACTION_SEND)
            intent.type = "image/*"
            intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION

            if (!imageNewName.isBlank())
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

        // Click listener for delete button
        buttonDelete.setOnClickListener {
            if (imageViewImageOriginal.isGone) {
                BuildAlbumDBOpenHelper(applicationContext, null).deleteImage(
                    imageNew
                )
                if (imageNew.file.exists())
                    imageNew.file.delete()
                images.remove(imageNew)
            } else {
                BuildAlbumDBOpenHelper(applicationContext, null).deleteImage(
                    imageOriginal
                )
                if (imageOriginal.file.exists())
                    imageOriginal.file.delete()
                images.remove(imageOriginal)
            }
            imagesAdapter.notifyDataSetChanged()
            toast(getString(image_deleted))
            finish()
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
     * OnRestoreInstanceState ImageActivity
     *
     * @param savedInstanceState
     */
    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        imageNewName = savedInstanceState.getString("imageNewName")!!
        if (!imageNewName.isBlank()) {
            imageNew = Image(this, imageNewName)
            imageView.setImageURI(imageNew.uri)
            imageView.isGone = false
            imageViewImageOriginal.isGone = true
        }
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

    /**
     * Helper class for creating new image
     */
    inner class SavePicture(private val image: Image) :
        AsyncTask<String, Void, Bitmap>() {

        override fun doInBackground(vararg args: String?): Bitmap? {
            return convertImageViewToBitmap(imageView)
        }

        override fun onPostExecute(result: Bitmap) {
            writeImage(result)
            if (image !in images) {
                images.add(0, image)
                BuildAlbumDBOpenHelper(applicationContext, null).addImage(
                    image
                )
            }
            imagesAdapter.notifyDataSetChanged()
            toast(getString(image_saved))
        }

        // Write new image on the file system
        private fun writeImage(finalBitmap: Bitmap) {
            if (image.file.exists())
                image.file.delete()

            try {
                val out = FileOutputStream(image.file)
                finalBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                out.flush()
                out.close()
            } catch (e: IOException) {
                toast(getString(not_enough_space_on_disk))
                e(tag, e.message!!)
                e.printStackTrace()
            }
        }
    }

    /**
     * Method to get a bitmap from ImageView
     */
    private fun convertImageViewToBitmap(view: ImageView): Bitmap {
        return (view.drawable as BitmapDrawable).bitmap
    }
}
