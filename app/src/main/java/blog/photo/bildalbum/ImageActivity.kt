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
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isGone
import blog.photo.bildalbum.model.Image
import blog.photo.bildalbum.utils.ImagesDBOpenHelper
import kotlinx.android.synthetic.main.activity_image.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Class that manages the image screen.
 */
class ImageActivity : AppCompatActivity() {

    private val frameDarkBrown = "frameDarkBrown.png"
    private val frameGolden = "frameGolden.png"
    private val frameLightBrown = "frameLightBrown.png"
    private val frameSilver = "frameSilver.png"
    private val frameWhite = "frameWhite.png"
    private val imageSize = 400
    private val imageBorderSize = 100
    private lateinit var imageNewFilePath: String

    /**
     * OnCreate Activity
     *
     * @param savedInstanceState
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image)

        val bitmap = BitmapFactory.decodeFile(intent.extras!!.get("imageUri").toString())
        imageOriginal.setImageBitmap(bitmap)
        var bitmapNew: Bitmap? = null

        // Click listener for edit button add frame
        buttonAddFrame.setOnClickListener {
            val frame = when (radioFrame.checkedRadioButtonId) {
                buttonFrameDarkBrown.id -> frameDarkBrown
                buttonFrameGolden.id -> frameGolden
                buttonFrameLightBrown.id -> frameLightBrown
                buttonFrameSilver.id -> frameSilver
                buttonFrameWhite.id -> frameWhite
                else -> null
            }
            if (bitmap != null) {
                bitmapNew = addBitmapFrame(
                    bitmap,
                    frame
                )
                imageNew.setImageBitmap(bitmapNew)
                imageNew.isGone = false
                buttonSave.isGone = false

                toast(getString(R.string.image_edited))
            } else {
                toast(getString(R.string.image_not_found))
            }
        }

        // Click listener for save button
        buttonSave.setOnClickListener {
            CreateImage(
                this,
                LayoutInflater.from(this).inflate(
                    R.layout.activity_image,
                    null
                ).findViewById(R.id.imageNew)
            ).execute()
            buttonFacebookShare.isGone = false
        }

        // Click listener for share button
        buttonFacebookShare.setOnClickListener {
            var intent = Intent(Intent.ACTION_SEND)
            intent.type = "image/*"
            intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(File(imageNewFilePath)))

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
                intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.facebook.com/sharer/sharer.php?u=" + R.string.app_web))
                toast(getString(R.string.install_facebook_for_optimal_experience))
            }

            startActivity(intent)
        }
    }

    /**
     * Method to add a bitmap frame
     */
    private fun addBitmapFrame(bitmap: Bitmap, frame: String?): Bitmap? {
        var scaledBitmap = Bitmap.createScaledBitmap(bitmap, imageSize, imageSize, false);
        val editedBitmap: Bitmap?
        val canvas: Canvas
        if (frame != null) {
            editedBitmap = assetsToBitmap(frame)?.copy(Bitmap.Config.ARGB_8888, true)
            canvas = Canvas(editedBitmap!!)
            canvas.drawBitmap(
                scaledBitmap,
                imageBorderSize.toFloat(),
                imageBorderSize.toFloat(),
                null
            )
        } else {
            editedBitmap = Bitmap.createBitmap(
                scaledBitmap.width + imageBorderSize * 2,
                scaledBitmap.height + imageBorderSize * 2,
                scaledBitmap.config
            )
            canvas = Canvas(editedBitmap)
            canvas.drawColor(resources.getColor(R.color.colorPrimary))
            canvas.drawBitmap(
                scaledBitmap,
                imageBorderSize.toFloat(),
                imageBorderSize.toFloat(),
                null
            )
        }
        return editedBitmap
    }

    /**
     * Method to get a bitmap from assets
     */
    private fun assetsToBitmap(fileName: String): Bitmap? {
        return try {
            val stream = assets.open(fileName)
            BitmapFactory.decodeStream(stream)
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Helper class for creating new image
     *
     * @param context
     * @param imageView
     */
    inner class CreateImage(context: Context, var imageView: ImageView) :
        AsyncTask<String, Void, Bitmap>() {
        val context = context

        override fun doInBackground(vararg arg: String?): Bitmap? {
            return convertImageViewToBitmap(imageNew)
        }

        override fun onPostExecute(result: Bitmap) {
            imageNewFilePath = writeImage(result)

            // Update the images GridView in main screen
            MainActivity.storedImagesPaths.add(0, imageNewFilePath)
            MainActivity.imagesAdapter.notifyDataSetChanged();

            ImagesDBOpenHelper(context, null).addImage(Image(imageNewFilePath))
            toast(getString(R.string.image_created))
        }

        private fun writeImage(finalBitmap: Bitmap): String {
            val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)

            if (!storageDir!!.exists()) {
                storageDir.mkdirs()
            }

            val file = File(storageDir, "img" + System.currentTimeMillis() + ".jpg")
            if (file.exists())
                file.delete()

            try {
                val out = FileOutputStream(file)
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                out.flush()
                out.close()
            } catch (e: Exception) {
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
