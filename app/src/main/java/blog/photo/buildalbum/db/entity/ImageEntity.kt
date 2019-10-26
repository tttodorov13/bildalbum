package blog.photo.buildalbum.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "images")
data class ImageEntity(@PrimaryKey val name: String, val origin: String)