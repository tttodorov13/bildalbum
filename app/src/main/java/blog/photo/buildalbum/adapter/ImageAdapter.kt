package blog.photo.buildalbum.adapter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import blog.photo.buildalbum.db.entity.Image
import blog.photo.buildalbum.R
import java.io.File

/**
 * Class to manage render images on main screen.
 */
class ImageAdapter(private val context: Context) :
    BaseAdapter() {

    // Cached copy of images
    private var images = emptyList<Image>()

    override fun getCount(): Int {
        return images.size
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItem(position: Int): Any? {
        return images[position]
    }

    override fun getView(position: Int, view: View?, parent: ViewGroup): View? {
        var convertView = view
        val viewHolder: ViewHolder

        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = false
        options.inPreferredConfig = Bitmap.Config.ARGB_8888
        options.inSampleSize = 3

        val bitmap = BitmapFactory.decodeFile(
            File(
                context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                images[position].file
            ).canonicalPath, options
        )

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.card_layout, null)

            val imageView = convertView.findViewById(R.id.card) as ImageView

            viewHolder = ViewHolder(imageView)
            viewHolder.imageView.setImageBitmap(bitmap)

            convertView.tag = viewHolder
        } else {
            viewHolder = convertView.tag as ViewHolder
            viewHolder.imageView.setImageBitmap(bitmap)
        }

        return convertView
    }

    private inner class ViewHolder(
        var imageView: ImageView
    )

    internal fun getImages(): List<Image> {
        return this.images
    }

    internal fun setImages(images: List<Image>) {
        this.images = images
        notifyDataSetChanged()
    }
}
