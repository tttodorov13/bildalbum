package blog.photo.buildalbum.db.entity

open class Card(
    open var isEdited: Boolean = false,
    open var file: String = "",
    open var source: String = ""
) {
    override fun equals(other: Any?): Boolean {
        return other != null &&
                other is Card &&
                (source == other.source ||
                        file == other.file)
    }
}