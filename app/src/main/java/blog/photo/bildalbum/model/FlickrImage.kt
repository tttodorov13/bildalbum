package blog.photo.bildalbum.model

class FlickrImage(val title: String, val author: String, val authorId: String,
                  val link: String, val tags: String, val image: String) {
    override fun toString(): String {
        return "FlickrImage(title='$title', author='$author', authorId='$authorId', link='$link', tags='$tags', image='$image')"
    }
}