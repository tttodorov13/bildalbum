package blog.photo.bildalbum.utils

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import blog.photo.bildalbum.R
import java.io.File

class PicturesAdapter(private val mContext: Context, private val picturesPaths: ArrayList<String>) : BaseAdapter() {

    override fun getCount(): Int {
        return picturesPaths.size
    }

    override fun getItemId(position: Int): Long {
        return 0
    }

    override fun getItem(position: Int): Any? {
        return null
    }

    override fun getView(position: Int, view: View?, parent: ViewGroup): View? {
        var convertView = view
        val picturePath = picturesPaths[position]
        var viewHolder: ViewHolder

        if (convertView == null) {
            val layoutInflater = LayoutInflater.from(mContext)
            convertView = layoutInflater.inflate(R.layout.image_layout, null)

            val imageView = convertView.findViewById(R.id.picture) as ImageView
            imageView.setImageURI(Uri.parse(picturePath))

            viewHolder = ViewHolder(imageView)
            convertView.tag = viewHolder
        } else {
            viewHolder = convertView.tag as ViewHolder
            viewHolder.imageView.setImageURI(Uri.parse(picturePath))
        }

        return convertView
    }

    private inner class ViewHolder(
        var imageView: ImageView
    )
}
