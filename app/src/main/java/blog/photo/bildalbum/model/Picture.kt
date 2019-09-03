package blog.photo.bildalbum.model

import java.lang.System.currentTimeMillis

/**
 * Base class that manages the picture model.
 */
open class Picture() {
    var name: String = "img".plus(currentTimeMillis()).plus(".png")
    var uri: String = ""

    constructor(name: String) : this() {
        this.name = name
    }

    constructor(name: String, uri: String) : this(name) {
        this.name = name
        this.uri = uri
    }

    override fun equals(other: Any?): Boolean {
        if (other == null ||
            other !is Picture ||
            uri != other.uri || name != other.name
        ) return false

        return true
    }
}

/**
 * Class that manages the framesNames model.
 */
class Frame(name: String, uri: String) : Picture(name, uri) {
    constructor(picture: Picture) : this(picture.name, picture.uri)

    override fun toString(): String {
        return "Frame(name='$name', uri='$uri')"
    }
}

/**
 * Class that manages the image model.
 */
class Image(name: String, uri: String) : Picture(name, uri) {
    constructor(picture: Picture) : this(picture.name, picture.uri)

    override fun toString(): String {
        return "Image(name='$name', uri='$uri')"
    }
}