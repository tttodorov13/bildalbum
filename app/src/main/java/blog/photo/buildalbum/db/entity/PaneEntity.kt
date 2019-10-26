package blog.photo.buildalbum.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "panes")
data class PaneEntity(@PrimaryKey val name: String, val origin: String)