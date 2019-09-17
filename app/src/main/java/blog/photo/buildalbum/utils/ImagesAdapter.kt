package blog.photo.buildalbum.utils

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import blog.photo.buildalbum.R
import blog.photo.buildalbum.model.Image

class ImagesAdapter(private val context: Context, private val images: ArrayList<Image>) :
    BaseAdapter() {

    override fun getCount(): Int {
        return images.size
    }

    override fun getItemId(position: Int): Long {
        return 0
    }

    override fun getItem(position: Int): Any? {
        return null
    }

    override fun getView(position: Int, view: View?, parent: ViewGroup): View? {
        var convertView = view
        var viewHolder: ViewHolder

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.image_layout, null)

            val imageView = convertView.findViewById(R.id.picture) as ImageView
            imageView.setImageURI(images[position].uri)

            viewHolder = ViewHolder(imageView)
            convertView.tag = viewHolder
        } else {
            viewHolder = convertView.tag as ViewHolder
            viewHolder.imageView.setImageURI(
                images[position].uri
            )
        }

        return convertView
    }

    private inner class ViewHolder(
        var imageView: ImageView
    )
}
