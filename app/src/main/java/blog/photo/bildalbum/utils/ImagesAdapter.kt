package blog.photo.bildalbum.utils

import android.content.Context
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import blog.photo.bildalbum.R

class ImagesAdapter(private val mContext: Context, private val imagesPaths: ArrayList<String>) : BaseAdapter() {

    override fun getCount(): Int {
        return imagesPaths.size
    }

    override fun getItemId(position: Int): Long {
        return 0
    }

    override fun getItem(position: Int): Any? {
        return null
    }

    override fun getView(position: Int, view: View?, parent: ViewGroup): View? {
        var convertView = view
        val imagePath = imagesPaths[position]
        var viewHolder: ViewHolder

        if (convertView == null) {
            val layoutInflater = LayoutInflater.from(mContext)
            convertView = layoutInflater.inflate(R.layout.image_layout, null)

            val imageView = convertView.findViewById(R.id.picture) as ImageView
            imageView.setImageBitmap(BitmapFactory.decodeFile(imagePath))

            viewHolder = ViewHolder(imageView)
            convertView.tag = viewHolder
        } else {
            viewHolder = convertView.tag as ViewHolder
            viewHolder.imageView.setImageBitmap(BitmapFactory.decodeFile(imagePath))
        }

        return convertView
    }

    private inner class ViewHolder(
        var imageView: ImageView
    )
}
