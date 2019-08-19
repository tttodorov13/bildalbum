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
                DATABASE_TABLE + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY," +
                COLUMN_PATH
                + " TEXT" + ")")
        db.execSQL(createTableImages)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS " + DATABASE_TABLE)
        onCreate(db)
    }

    fun addImage(image: Image) {
        val values = ContentValues()
        values.put(COLUMN_PATH, image.path)
        val db = this.writableDatabase
        db.insert(DATABASE_TABLE, null, values)
        db.close()
    }

    fun getAllPhotosReverse(): Cursor? {
        val db = this.readableDatabase
        return db.rawQuery("SELECT * FROM $DATABASE_TABLE ORDER BY $COLUMN_ID DESC", null)
    }

    companion object {
        private const val DATABASE_VERSION = 1
        private const val DATABASE_NAME = "bildalbum.db"
        private const val DATABASE_TABLE = "images"
        private const val COLUMN_ID = "_id"
        const val COLUMN_PATH = "path"
    }
}