package blog.photo.bildalbum.model

/**
 * Base class that manages the picture model.
 */
open class Picture(var name: String, var uri: String)

/**
 * Class that manages the frames model.
 */
class Frame(name: String, uri: String) : Picture(name, uri) {

    override fun toString(): String {
        return "Frame(name='$name', uri='$uri')"
    }
}

/**
 * Class that manages the image model.
 */
class Image(name: String, uri: String) : Picture(name, uri) {

    override fun toString(): String {
        return "Image(name='$name', uri='$uri')"
    }
}