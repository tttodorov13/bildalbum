package blog.photo.buildalbum.adapter

import android.content.Context
import android.os.Build
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.core.content.ContextCompat
import blog.photo.buildalbum.R

/**
 * Class to manage render icons and texts in Add Image dialog.
 */
class IconAdapter(context: Context, texts: List<String?>, icons: List<Int?>) : ArrayAdapter<String>(
    context,
    android.R.layout.select_dialog_item,
    texts
) {

    private var icons: List<Int?>? = icons
    private val iconSize: Float = 12f

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent)
        val textView = view.findViewById(android.R.id.text1) as TextView

        textView.compoundDrawablePadding =
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                iconSize,
                context.resources.displayMetrics
            ).toInt()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
            textView.setCompoundDrawablesRelativeWithIntrinsicBounds(icons!![position]!!, 0, 0, 0)
        else
            textView.setCompoundDrawablesWithIntrinsicBounds(icons!![position]!!, 0, 0, 0)

        textView.setTextColor(
            ContextCompat.getColor(
                context,
                R.color.colorYellow
            )
        )

        return view
    }
}