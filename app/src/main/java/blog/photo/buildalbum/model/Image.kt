package blog.photo.buildalbum.model

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.util.Log
import blog.photo.buildalbum.BaseActivity.Companion.frames
import blog.photo.buildalbum.BaseActivity.Companion.images
import blog.photo.buildalbum.BaseActivity.Companion.imagesAdapter
import blog.photo.buildalbum.utils.DatabaseHelper
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Base class to manage the picture model.
 */
data class Image(val context: Context, val isFrame: Boolean, val name: String, val origin: String) {

    /**
     * A companion object for class variables.
     */
    companion object {
        private const val tag = "Image"
    }

    constructor(context: Context, isFrame: Boolean, origin: String) : this(
        context,
        isFrame,
        "img".plus(System.nanoTime()).plus(".png"),
        origin
    )

    constructor(context: Context, name: String, origin: String) : this(context, false, name, origin)

    constructor(context: Context, name: String) : this(context, false, name, "")

    constructor(context: Context) : this(context, "img".plus(System.nanoTime()).plus(".png"))

    override fun equals(other: Any?): Boolean {
        return other != null &&
                other is Image &&
                isFrame == other.isFrame &&
                origin == other.origin &&
                (isFrame || name == other.name)
    }

    val file = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), name)
    val filePath: String = file.canonicalPath
    val uri = Uri.parse(filePath)!!

    internal fun delete() {
        DatabaseHelper(context).deleteImage(
            this
        )

        if (file.exists())
            file.delete()

        images.remove(this)
        imagesAdapter.notifyDataSetChanged()
    }

    /**
     * Method to save image data in database.
     */
    internal fun save(): Boolean {
        return when {
            // Check if the image is frame
            isFrame -> {
                if (this !in frames) {
                    DatabaseHelper(context).addFrame(
                        this
                    )
                    frames.add(0, this)
                    return true
                }
                false
            }

            // Check if the image already exists
            this !in images -> {
                DatabaseHelper(context).addImage(
                    this
                )
                images.add(0, this)
                imagesAdapter.notifyDataSetChanged()
                true
            }
            else -> false
        }
    }

    /**
     * Method to write new image on the file system.
     */
    internal fun write(bitmap: Bitmap?) {
        if (file.exists())
            file.delete()

        try {
            val out = FileOutputStream(file)
            bitmap?.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.flush()
            out.close()
        } catch (e: IOException) {
            Log.e(tag, e.message.toString())
        }
    }
}