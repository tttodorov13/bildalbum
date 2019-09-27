package blog.photo.buildalbum.adapters

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import blog.photo.buildalbum.R
import blog.photo.buildalbum.models.Image

/**
 * Class to manage render images on main screen.
 */
class ImagesAdapter(private val context: Context, private val images: ArrayList<Image>) :
    BaseAdapter() {

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
        var viewHolder: ViewHolder

        var options = BitmapFactory.Options()
        options.inDither = false
        options.inJustDecodeBounds = false
        options.inPreferredConfig = Bitmap.Config.ARGB_8888
        options.inSampleSize = 3
        options.inPurgeable = true

        val bitmap = BitmapFactory.decodeFile(images[position].filePath, options)

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.image_layout, null)

            val imageView = convertView.findViewById(R.id.picture) as ImageView

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
}
