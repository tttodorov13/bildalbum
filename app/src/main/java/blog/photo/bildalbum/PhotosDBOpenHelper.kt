package blog.photo.bildalbum

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class PhotosDBOpenHelper(
    context: Context,
    factory: SQLiteDatabase.CursorFactory?
) : SQLiteOpenHelper(
    context, DATABASE_NAME,
    factory, DATABASE_VERSION
) {

    override fun onCreate(db: SQLiteDatabase) {
        val CREATE_PRODUCTS_TABLE = ("CREATE TABLE " +
                TABLE_PHOTO + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY," +
                COLUMN_NAME
                + " TEXT" + ")")
        db.execSQL(CREATE_PRODUCTS_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PHOTO)
        onCreate(db)
    }

    fun addPhoto(photo: Photo) {
        val values = ContentValues()
        values.put(COLUMN_NAME, photo.path)
        val db = this.writableDatabase
        db.insert(TABLE_PHOTO, null, values)
        db.close()
    }

    fun getAllPhotos(): Cursor? {
        val db = this.readableDatabase
        return db.rawQuery("SELECT * FROM $TABLE_PHOTO", null)
    }

    companion object {
        private val DATABASE_VERSION = 1
        private val DATABASE_NAME = "bildalbum.db"
        val TABLE_PHOTO = "photos"
        val COLUMN_ID = "_id"
        val COLUMN_NAME = "path"
    }
}