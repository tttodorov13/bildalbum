package blog.photo.bildalbum.model

open class Image(var path: String) {
    var uri: String = ""

    override fun toString(): String {
        return "Image(path='$path', uri='$uri')"
    }
}