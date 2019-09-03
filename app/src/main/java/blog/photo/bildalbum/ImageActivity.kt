package blog.photo.bildalbum

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.os.Environment
import android.util.Log.e
import android.widget.AdapterView
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isGone
import blog.photo.bildalbum.MainActivity.Companion.images
import blog.photo.bildalbum.R.string.*
import blog.photo.bildalbum.model.Image
import blog.photo.bildalbum.model.Picture
import blog.photo.bildalbum.utils.BuildAlbumDBOpenHelper
import blog.photo.bildalbum.utils.PicturesAdapter
import kotlinx.android.synthetic.main.activity_image.*
import java.io.File
import java.io.FileOutputStream

/**
 * Class that manages the image screen.
 */
class ImageActivity : AppCompatActivity() {

    private var imageSize = 400
    private var imageSizeBorder = 100
    private var imageNewName: String? = null
    private var image = Picture()

    /**
     * A companion object to declare variables for displaying framesNames
     */
    companion object {
        private const val tag = "ImageActivity"
        private lateinit var framesAdapter: PicturesAdapter
        private lateinit var imageOriginal: String
    }

    /**
     * OnCreate Activity
     *
     * @param savedInstanceState
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image)

        imageOriginal = intent.extras!!.get("imageOriginal").toString()
        imageSize = getString(image_size).toInt()
        imageSizeBorder = getString(image_size_border).toInt()
        imageViewImageOriginal.setImageURI(
            Uri.parse(getPicture(imageOriginal).canonicalPath)
        )

        framesAdapter = PicturesAdapter(
            this, MainActivity.frames
        )
        gridViewFrames.isExpanded = true
        gridViewFrames.adapter = framesAdapter

        imageScreenScroll.smoothScrollTo(0, 0)

        gridViewFrames.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ ->
                try {
                    val bitmapNew = addFrame(
                        imageOriginal,
                        MainActivity.frames[position].name
                    )
                    imageViewImageNew.setImageBitmap(bitmapNew)
                    imageViewImageNew.isGone = false
                    imageViewImageOriginal.isGone = true
                    if (imageNewName == null)
                        imageNewName = image.name
                    else
                        image.name = imageNewName.toString()
                    SaveImage(image).execute()
                } catch (e: Exception) {
                    toast(getString(internal_error))
                    e(tag, e.message.toString())
                    e.printStackTrace()
                }
            }

        // Click listener for share button
        buttonShare.setOnClickListener {
            var intent = Intent(Intent.ACTION_SEND)
            intent.type = "image/*"
            intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION

            if (imageNewName != null)
                intent.putExtra(
                    Intent.EXTRA_STREAM,
                    Uri.fromFile(File(getPicture(imageNewName.toString()).canonicalPath))
                )
            else
                intent.putExtra(
                    Intent.EXTRA_STREAM,
                    Uri.fromFile(File(getPicture(imageOriginal).canonicalPath))
                )

            // See if official Facebook app is found
            var facebookAppFound = false
            val matches = packageManager.queryIntentActivities(intent, 0)
            for (info in matches) {
                if (info.activityInfo.packageName.toLowerCase().startsWith(
                        getString(
                            com_facebook_katana
                        )
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
    }

    /**
     * OnSaveInstanceState Activity
     *
     * @param outState
     */
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("imageNewName", imageNewName)
        super.onSaveInstanceState(outState)
    }

    /**
     * OnRestoreInstanceState Activity
     *
     * @param savedInstanceState
     */
    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        imageNewName = savedInstanceState.getString("imageNewName")
        if (imageNewName != null) {
            imageViewImageNew.setImageURI(Uri.parse(getPicture(imageNewName.toString()).canonicalPath))
            imageViewImageNew.isGone = false
            imageViewImageOriginal.isGone = true
        }
    }

    /**
     * Method to add a bitmap framesNames
     */
    private fun addFrame(imageOriginal: String, frame: String): Bitmap? {
        var scaledBitmap = Bitmap.createScaledBitmap(
            BitmapFactory.decodeFile(getPicture(imageOriginal).canonicalPath),
            imageSize,
            imageSize,
            false
        );
        val canvas: Canvas
        val imageNewBitmap = BitmapFactory.decodeFile(getPicture(frame).canonicalPath).copy(
            Bitmap.Config.ARGB_8888,
            true
        )
        canvas = Canvas(imageNewBitmap!!)
        canvas.drawBitmap(
            scaledBitmap,
            imageSizeBorder.toFloat(),
            imageSizeBorder.toFloat(),
            null
        )
        return imageNewBitmap
    }

    /**
     * Helper class for creating new image
     */
    inner class SaveImage(private val picture: Picture) :
        AsyncTask<String, Void, Bitmap>() {

        override fun doInBackground(vararg args: String?): Bitmap? {
            return convertImageViewToBitmap(imageViewImageNew)
        }

        override fun onPostExecute(result: Bitmap) {
            writeImage(result)
            if (picture !in images) {
                images.add(0, picture)
                BuildAlbumDBOpenHelper(applicationContext, null).addImage(
                    Image(picture)
                )
            }
            MainActivity.imagesAdapter.notifyDataSetChanged();
            toast(getString(image_saved))
        }

        private fun writeImage(finalBitmap: Bitmap) {
            val file = getPicture(imageNewName.toString())
            if (file.exists())
                file.delete()

            try {
                val out = FileOutputStream(file)
                finalBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                out.flush()
                out.close()
            } catch (e: Exception) {
                e(tag, e.message.toString())
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

    /**
     * Method to get image from file system
     */
    fun getPicture(name: String): File {
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        if (!storageDir!!.exists()) {
            storageDir.mkdirs()
        }

        return File(storageDir, name)
    }

    /**
     * Extension function to show toast message
     */
    fun Context.toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
