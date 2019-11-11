package blog.photo.buildalbum.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity(tableName = "image_table")
data class Image(
    @Ignore override var isEdited: Boolean,
    @ColumnInfo(name = "pane_file") @PrimaryKey override var file: String,
    @ColumnInfo(name = "pane_source") override var source: String
) :
    Card(isEdited, file, source)