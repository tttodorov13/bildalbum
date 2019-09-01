package blog.photo.bildalbum.utils

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import blog.photo.bildalbum.R
import java.io.File

class PicturesAdapter(private val mContext: Context, private val pictures: ArrayList<String>) :
    BaseAdapter() {

    override fun getCount(): Int {
        return pictures.size
    }

    override fun getItemId(position: Int): Long {
        return 0
    }

    override fun getItem(position: Int): Any? {
        return null
    }

    override fun getView(position: Int, view: View?, parent: ViewGroup): View? {
        var convertView = view
        val picture = pictures[position]
        var viewHolder: ViewHolder

        if (convertView == null) {
            val layoutInflater = LayoutInflater.from(mContext)
            convertView = layoutInflater.inflate(R.layout.image_layout, null)

            val imageView = convertView.findViewById(R.id.picture) as ImageView
            imageView.setImageURI(Uri.parse(getPictureCanonicalPath(picture)))

            viewHolder = ViewHolder(imageView)
            convertView.tag = viewHolder
        } else {
            viewHolder = convertView.tag as ViewHolder
            viewHolder.imageView.setImageURI(
                Uri.parse(
                    getPictureCanonicalPath(
                        picture
                    )
                )
            )
        }

        return convertView
    }

    private inner class ViewHolder(
        var imageView: ImageView
    )

    private fun getPictureCanonicalPath(name: String): String {
        val storageDir = mContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        if (!storageDir!!.exists()) {
            storageDir.mkdirs()
        }

        return File(storageDir, name).canonicalPath
    }
}
