package blog.photo.buildalbum.utils

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import blog.photo.buildalbum.models.Image

/**
 * Class to manage local database transactions.
 */
// TODO: Save to local database with Room
// TODO: Fix A SQLiteConnection object for database '+data+data+blog_photo_buildalbum+databases+buildalbum_db' was leaked!  Please fix your application to end transactions in progress properly and to close the database when it is no longer needed.
class DatabaseHelper(private val context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

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
        val db = writableDatabase
        db.insert(TABLE_FRAMES, null, values)
        db.close()
    }

    fun addImage(image: Image) {
        val values = ContentValues()
        values.put(COLUMN_NAME, image.name)
        values.put(COLUMN_ORIGIN, image.origin)
        val db = writableDatabase
        db.insert(TABLE_IMAGES, null, values)
        db.close()
    }

    fun deleteImage(image: Image) {
        val db = writableDatabase
        db.delete(TABLE_IMAGES, "$COLUMN_NAME=?", arrayOf(image.name))
        db.close()
    }

    fun getAllFramesReverse(): ArrayList<Image> {
        var frames = ArrayList<Image>()
        val cursor =
            readableDatabase.rawQuery("SELECT * FROM $TABLE_FRAMES ORDER BY $COLUMN_ID DESC", null)

        if (cursor!!.moveToFirst())
            frames.add(
                Image(
                    context,
                    true,
                    cursor.getString(
                        cursor.getColumnIndex(
                            COLUMN_NAME
                        )
                    ), cursor.getString(
                        cursor.getColumnIndex(
                            COLUMN_ORIGIN
                        )
                    )
                )
            )

        while (cursor.moveToNext())
            frames.add(
                Image(
                    context,
                    true,
                    cursor.getString(
                        cursor.getColumnIndex(
                            COLUMN_NAME
                        )
                    ),
                    cursor.getString(
                        cursor.getColumnIndex(
                            COLUMN_ORIGIN
                        )
                    )
                )
            )

        cursor.close()
        return frames
    }

    fun getAllImagesReverse(): ArrayList<Image> {
        var images = ArrayList<Image>()
        val cursor =
            readableDatabase.rawQuery("SELECT * FROM $TABLE_IMAGES ORDER BY $COLUMN_ID DESC", null)

        if (cursor!!.moveToFirst()) {
            images.add(
                Image(
                    context,
                    cursor.getString(
                        cursor.getColumnIndex(
                            COLUMN_NAME
                        )
                    ), cursor.getString(
                        cursor.getColumnIndex(
                            COLUMN_ORIGIN
                        )
                    )
                )
            )

            while (cursor.moveToNext())
                images.add(
                    Image(
                        context,
                        cursor.getString(
                            cursor.getColumnIndex(
                                COLUMN_NAME
                            )
                        ), cursor.getString(
                            cursor.getColumnIndex(
                                COLUMN_ORIGIN
                            )
                        )
                    )
                )
        }

        cursor.close()
        return images
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