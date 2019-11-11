package blog.photo.buildalbum.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity(tableName = "pane_table")
data class Pane(
    @Ignore override var isEdited: Boolean,
    @ColumnInfo(name = "image_file") @PrimaryKey override var file: String,
    @ColumnInfo(name = "image_source") override var source: String
) :
    Card(isEdited, file, source)