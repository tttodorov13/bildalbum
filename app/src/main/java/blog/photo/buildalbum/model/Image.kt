package blog.photo.buildalbum.model

import android.content.Context
import android.net.Uri
import android.os.Environment
import java.io.File

/**
 * Base class to manage the picture model.
 */
data class Image(val context: Context, val isFrame: Boolean, val name: String, val origin: String) {

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
                (name == other.name || (!origin.isBlank() && origin == other.origin))
    }

    val file = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), name)
    val uri = Uri.parse(file.canonicalPath)!!
}