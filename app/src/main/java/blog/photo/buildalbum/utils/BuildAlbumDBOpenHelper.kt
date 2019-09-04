package blog.photo.buildalbum.utils

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import blog.photo.buildalbum.model.Image

class BuildAlbumDBOpenHelper(
    context: Context,
    factory: SQLiteDatabase.CursorFactory?
) : SQLiteOpenHelper(
    context, DATABASE_NAME,
    factory, DATABASE_VERSION
) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE $TABLE_FRAMES ($COLUMN_ID INTEGER PRIMARY KEY, $COLUMN_NAME TEXT,$COLUMN_ORIGIN TEXT)")
        db.execSQL("CREATE TABLE $TABLE_IMAGES ($COLUMN_ID INTEGER PRIMARY KEY, $COLUMN_NAME TEXT,$COLUMN_ORIGIN TEXT)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_FRAMES")
        onCreate(db)
        db.execSQL("DROP TABLE IF EXISTS $TABLE_IMAGES")
        onCreate(db)
    }

    fun addFrame(frame: Image) {
        val values = ContentValues()
        values.put(COLUMN_NAME, frame.name)
        values.put(COLUMN_ORIGIN, frame.origin)
        val db = this.writableDatabase
        db.insert(TABLE_FRAMES, null, values)
        db.close()
    }

    fun addImage(image: Image) {
        val values = ContentValues()
        values.put(COLUMN_NAME, image.name)
        values.put(COLUMN_ORIGIN, image.origin)
        val db = this.writableDatabase
        db.insert(TABLE_IMAGES, null, values)
        db.close()
    }

    fun getAllFrames(): Cursor? {
        val db = this.readableDatabase
        return db.rawQuery("SELECT * FROM $TABLE_FRAMES ORDER BY $COLUMN_ID ASC", null)
    }

    fun getAllImagesReverse(): Cursor? {
        val db = this.readableDatabase
        return db.rawQuery("SELECT * FROM $TABLE_IMAGES ORDER BY $COLUMN_ID DESC", null)
    }

    companion object {
        private const val DATABASE_VERSION = 1
        private const val DATABASE_NAME = "buildalbum.db"
        private const val TABLE_IMAGES = "images"
        private const val TABLE_FRAMES = "frames"
        private const val COLUMN_ID = "_id"
        const val COLUMN_NAME = "name"
        const val COLUMN_ORIGIN = "origin"
    }
}