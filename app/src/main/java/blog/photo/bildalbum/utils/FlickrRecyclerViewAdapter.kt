package blog.photo.bildalbum.utils

import androidx.recyclerview.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import blog.photo.bildalbum.R
import blog.photo.bildalbum.model.FlickrPhoto
import com.squareup.picasso.Picasso

class FlickrImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val title: TextView = view.findViewById(R.id.title)
}

class FlickrRecyclerViewAdapter(private var flickrPhotos: List<FlickrPhoto>) : RecyclerView.Adapter<FlickrImageViewHolder>() {
    private val TAG = "FlickrRVAdapter"

    override fun onBindViewHolder(holder: FlickrImageViewHolder, position: Int) {
        val photoItem = flickrPhotos[position]
        Picasso.get()
                .load(photoItem.image)
                .error(R.drawable.placeholder)
                .placeholder(R.drawable.placeholder)

        holder.title.text = photoItem.title
    }

    fun loadNewData(newFlickrPhotos: List<FlickrPhoto>) {
        flickrPhotos = newFlickrPhotos
        notifyDataSetChanged()
    }

    fun getPhoto(position: Int): FlickrPhoto? =
        if (flickrPhotos.isNotEmpty()) flickrPhotos[position] else null

    override fun getItemCount(): Int {
        Log.d(TAG, "getItemCount called")
        return if (flickrPhotos.isNotEmpty()) flickrPhotos.size else 0
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FlickrImageViewHolder {
        Log.d(TAG,"onCreateViewHolder")
        val view = LayoutInflater.from(parent.context).inflate(R.layout.picture_layout, parent, false)
        return FlickrImageViewHolder(view)
    }
}