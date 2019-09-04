package blog.photo.buildalbum.model

import android.content.Context
import android.net.Uri
import android.os.Environment
import java.io.File

/**
 * Base class to manage the picture model.
 */
data class Image(val context: Context, val name: String, val origin: String) {

    override fun equals(other: Any?): Boolean {
        if (other == null ||
            other !is Image ||
            origin != other.origin || name != other.name
        ) return false

        return true
    }

    val file = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), name)
    val uri = Uri.parse(file.canonicalPath)
}