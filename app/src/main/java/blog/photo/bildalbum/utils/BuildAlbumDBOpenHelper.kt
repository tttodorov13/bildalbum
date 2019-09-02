package blog.photo.bildalbum.utils

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import blog.photo.bildalbum.model.Frame
import blog.photo.bildalbum.model.Image

class BuildAlbumDBOpenHelper(
    context: Context,
    factory: SQLiteDatabase.CursorFactory?
) : SQLiteOpenHelper(
    context, DATABASE_NAME,
    factory, DATABASE_VERSION
) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE $TABLE_FRAMES ($COLUMN_ID INTEGER PRIMARY KEY, $COLUMN_NAME TEXT,$COLUMN_URI TEXT)")
        db.execSQL("CREATE TABLE $TABLE_IMAGES ($COLUMN_ID INTEGER PRIMARY KEY, $COLUMN_NAME TEXT,$COLUMN_URI TEXT)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_FRAMES")
        onCreate(db)
        db.execSQL("DROP TABLE IF EXISTS $TABLE_IMAGES")
        onCreate(db)
    }

    fun addFrame(frame: Frame) {
        val values = ContentValues()
        values.put(COLUMN_NAME, frame.name)
        values.put(COLUMN_URI, frame.uri)
        val db = this.writableDatabase
        db.insert(TABLE_FRAMES, null, values)
        db.close()
    }

    fun addImage(image: Image) {
        val values = ContentValues()
        values.put(COLUMN_NAME, image.name)
        values.put(COLUMN_URI, image.uri)
        val db = this.writableDatabase
        db.insert(TABLE_IMAGES, null, values)
        db.close()
    }

    fun getAllFrames(): Cursor? {
        val db = this.readableDatabase
        return db.rawQuery("SELECT * FROM $TABLE_FRAMES ORDER BY $COLUMN_ID ASC", null)
        db.close()
    }

//    fun getAllFramesNames(): ArrayList<String> {
//        var frameName = ""
//        val framesNames = ArrayList<String>()
//        val db = this.readableDatabase
//        val cursor = db.rawQuery("SELECT * FROM $TABLE_FRAMES ORDER BY $COLUMN_ID ASC", null)
//        if (cursor!!.moveToFirst()) {
//            frameName =
//                cursor.getString(
//                    cursor.getColumnIndex(
//                        BuildAlbumDBOpenHelper.COLUMN_NAME
//                    )
//                )
//            if (!frameName.isBlank())
//                framesNames.add(frameName)
//            while (cursor.moveToNext()) {
//                frameName =
//                    cursor.getString(
//                        cursor.getColumnIndex(
//                            BuildAlbumDBOpenHelper.COLUMN_NAME
//                        )
//                    )
//                if (!frameName.isBlank())
//                    framesNames.add(frameName)
//            }
//        }
//        db.close()
//        return framesNames
//    }

    fun getAllImagesReverse(): Cursor? {
        val db = this.readableDatabase
        return db.rawQuery("SELECT * FROM $TABLE_IMAGES ORDER BY $COLUMN_ID DESC", null)
        db.close()
    }

    companion object {
        private const val DATABASE_VERSION = 1
        private const val DATABASE_NAME = "buildalbum.db"
        private const val TABLE_IMAGES = "images"
        private const val TABLE_FRAMES = "frames"
        private const val COLUMN_ID = "_id"
        const val COLUMN_NAME = "name"
        const val COLUMN_URI = "uri"
    }
}