package blog.photo.bildalbum.model

class FlickrPhoto(val title: String, val author: String, val authorId: String,
                  val link: String, val tags: String, val image: String) {
    override fun toString(): String {
        return "FlickrPhoto(title='$title', author='$author', authorId='$authorId', link='$link', tags='$tags', image='$image')"
    }
}