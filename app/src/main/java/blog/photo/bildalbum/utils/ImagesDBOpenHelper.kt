package blog.photo.bildalbum.utils

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import blog.photo.bildalbum.model.Image

class ImagesDBOpenHelper(
    context: Context,
    factory: SQLiteDatabase.CursorFactory?
) : SQLiteOpenHelper(
    context, DATABASE_NAME,
    factory, DATABASE_VERSION
) {

    override fun onCreate(db: SQLiteDatabase) {
        val createTableImages = ("CREATE TABLE " +
                TABLE_IMAGES + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY," +
                COLUMN_NAME
                + " TEXT" + ")")
        db.execSQL(createTableImages)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_IMAGES)
        onCreate(db)
    }

    fun addPhoto(photo: Image) {
        val values = ContentValues()
        values.put(COLUMN_NAME, photo.path)
        val db = this.writableDatabase
        db.insert(TABLE_IMAGES, null, values)
        db.close()
    }

    fun getAllPhotos(): Cursor? {
        val db = this.readableDatabase
        return db.rawQuery("SELECT * FROM $TABLE_IMAGES", null)
    }

    companion object {
        private val DATABASE_VERSION = 1
        private val DATABASE_NAME = "bildalbum.db"
        const val TABLE_IMAGES = "images"
        const val COLUMN_ID = "_id"
        const val COLUMN_NAME = "path"
    }
}