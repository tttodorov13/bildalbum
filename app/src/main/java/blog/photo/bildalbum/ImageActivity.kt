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
import android.view.LayoutInflater
import android.widget.AdapterView
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isGone
import blog.photo.bildalbum.MainActivity.Companion.images
import blog.photo.bildalbum.model.Image
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
    private var imageNew: String? = null

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
        imageSize = getString(R.string.image_size).toInt()
        imageSizeBorder = getString(R.string.image_size_border).toInt()
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
                    var new = false
                    if (imageNew == null) {
                        imageNew = "img" + System.currentTimeMillis() + ".jpg"
                        new = true
                    }

                    SaveImage(
                        applicationContext,
                        LayoutInflater.from(applicationContext).inflate(
                            R.layout.activity_image,
                            null
                        ).findViewById(R.id.imageViewImageNew)
                        , new
                    ).execute()
                } catch (e: Exception) {
                    toast(getString(R.string.internal_error))
                    e(tag, e.message.toString())
                    e.printStackTrace()
                }
            }

        // Click listener for share button
        buttonShare.setOnClickListener {
            var intent = Intent(Intent.ACTION_SEND)
            intent.type = "image/*"
            intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION

            if (imageNew != null)
                intent.putExtra(
                    Intent.EXTRA_STREAM,
                    Uri.fromFile(File(getPicture(imageNew.toString()).canonicalPath))
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
                if (info.activityInfo.packageName.toLowerCase().startsWith("com.facebook.katana")) {
                    intent.setPackage(info.activityInfo.packageName)
                    facebookAppFound = true
                    break
                }
            }

            // As fallback, launch sharer.php in a browser
            if (!facebookAppFound) {
                intent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://www.facebook.com/sharer/sharer.php?u=${getString(R.string.app_web)}")
                )
                toast(getString(R.string.install_facebook_for_optimal_experience))
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
        outState.putString("imageNew", imageNew)
        super.onSaveInstanceState(outState)
    }

    /**
     * OnRestoreInstanceState Activity
     *
     * @param savedInstanceState
     */
    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        imageNew = savedInstanceState.getString("imageNew")
        if (imageNew != null) {
            imageViewImageNew.setImageURI(Uri.parse(getPicture(imageNew.toString()).canonicalPath))
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
     *
     * @param context
     * @param imageView
     */
    inner class SaveImage(context: Context, var imageView: ImageView, private var new: Boolean) :
        AsyncTask<String, Void, Bitmap>() {
        val context = context
        private var image = Image("","")

        override fun doInBackground(vararg args: String?): Bitmap? {
            return convertImageViewToBitmap(imageViewImageNew)
        }

        override fun onPostExecute(result: Bitmap) {
            writeImage(result)

            if (new) {
                // Update the imagesNames GridView in main screen
                image.name = imageNew.toString()
                images.add(0,  image)
                BuildAlbumDBOpenHelper(context, null).addImage(
                    Image(
                        imageNew.toString(),
                        ""
                    )
                )
            }
            MainActivity.imagesAdapter.notifyDataSetChanged();
            toast(getString(R.string.image_saved))
        }

        private fun writeImage(finalBitmap: Bitmap) {
            val file = getPicture(imageNew.toString())
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
     * Extension function to show toast message
     */
    fun Context.toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    /**
     * Method to get picture from file system
     */
    fun getPicture(name: String): File {
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        if (!storageDir!!.exists()) {
            storageDir.mkdirs()
        }

        return File(storageDir, name)
    }
}
