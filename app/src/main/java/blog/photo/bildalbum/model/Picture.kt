package blog.photo.bildalbum.model

/**
 * Base class that manages the picture model.
 */
open class Picture(var path: String, var uri: String)

/**
 * Class that manages the frame model.
 */
class Frame(path: String, uri: String) : Picture(path, uri) {

    override fun toString(): String {
        return "Frame(path='$path', uri='$uri')"
    }
}

/**
 * Class that manages the image model.
 */
class Image(path: String, uri: String) : Picture(path, uri) {

    override fun toString(): String {
        return "Image(path='$path', uri='$uri')"
    }
}