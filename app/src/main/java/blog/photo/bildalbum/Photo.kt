package blog.photo.bildalbum

class Photo(var path: String?) {
    var id: Int = 0
    var album: String? = null

    constructor(id: Int, path: String) : this(path) {
        this.id = id
    }

    constructor(id: Int, path: String, album: String) : this(id, path) {
        this.album = album
    }
}