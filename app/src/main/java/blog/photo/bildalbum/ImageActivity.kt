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
import blog.photo.bildalbum.MainActivity.Companion.storedFramesPaths
import blog.photo.bildalbum.MainActivity.Companion.storedImagesPaths
import blog.photo.bildalbum.utils.BuildAlbumDBOpenHelper
import blog.photo.bildalbum.utils.PicturesAdapter
import kotlinx.android.synthetic.main.activity_image.*
import java.io.File
import java.io.FileOutputStream

/**
 * Class that manages the image screen.
 */
class ImageActivity : AppCompatActivity() {

    private val tag = "ImageActivity"
    private var imageSize = 400
    private var imageSizeBorder = 100
    private var imageNewFilePath: String? = null
    private var imageNewName: String? = null

    /**
     * A companion object to declare variables for displaying frames
     */
    companion object {
        private lateinit var framesAdapter: PicturesAdapter
        private lateinit var imageOriginalFilePath: String
        /**
         * Method to get picture from file system
         */
        fun getPicture(imageActivity: ImageActivity, name: String): File {
            val storageDir = imageActivity.getExternalFilesDir(Environment.DIRECTORY_PICTURES)

            if (!storageDir!!.exists()) {
                storageDir.mkdirs()
            }

            return File(storageDir, name)
        }
    }

    /**
     * OnCreate Activity
     *
     * @param savedInstanceState
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image)

        imageOriginalFilePath = intent.extras!!.get("imageFilePath").toString()
        imageSize = getString(R.string.image_size).toInt()
        imageSizeBorder = getString(R.string.image_size_border).toInt()
        imageOriginal.setImageURI(Uri.parse(imageOriginalFilePath))

        framesAdapter = PicturesAdapter(this, storedFramesPaths)
        frames.adapter = framesAdapter

        frames.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ ->
                try {
                    val bitmapNew = addFrame(
                        imageOriginalFilePath,
                        storedFramesPaths[position]
                    )
                    imageNew.setImageBitmap(bitmapNew)
                    imageNew.isGone = false
                    imageOriginal.isGone = true
                    var new = false
                    if (imageNewName == null) {
                        imageNewName = "img" + System.currentTimeMillis() + ".jpg"
                        new = true
                    }

                    SaveImage(
                        applicationContext,
                        LayoutInflater.from(applicationContext).inflate(
                            R.layout.activity_image,
                            null
                        ).findViewById(R.id.imageNew)
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

            if (imageNewFilePath != null)
                intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(File(imageNewFilePath)))
            else
                intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(File(imageOriginalFilePath)))

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
        outState.putString("imageNewFilePath", imageNewFilePath)
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
        imageNewFilePath = savedInstanceState.getString("imageNewFilePath")
        if (imageNewFilePath != null) {
            imageNew.setImageURI(Uri.parse(imageNewFilePath))
            imageNew.isGone = false
            imageOriginal.isGone = true
        }
    }

    /**
     * Method to add a bitmap frame
     */
    private fun addFrame(imageOriginalFilePath: String, frame: String): Bitmap? {
        var scaledBitmap = Bitmap.createScaledBitmap(
            BitmapFactory.decodeFile(imageOriginalFilePath),
            imageSize,
            imageSize,
            false
        );
        val canvas: Canvas
        val imageNewBitmap = BitmapFactory.decodeFile(frame).copy(
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

        override fun doInBackground(vararg args: String?): Bitmap? {
            return convertImageViewToBitmap(imageNew)
        }

        override fun onPostExecute(result: Bitmap) {
            imageNewFilePath = writeImage(result)

            if (new) {
                // Update the images GridView in main screen
                storedImagesPaths.add(0, imageNewFilePath.toString())
                BuildAlbumDBOpenHelper(context, null).addImage(
                    Image(
                        imageNewFilePath.toString(),
                        ""
                    )
                )
            }
            MainActivity.imagesAdapter.notifyDataSetChanged();

            toast(getString(R.string.image_saved))
        }

        private fun writeImage(finalBitmap: Bitmap): String {
            val file = Companion.getPicture(this@ImageActivity, imageNewName.toString())
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

            return file.canonicalPath
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
}
