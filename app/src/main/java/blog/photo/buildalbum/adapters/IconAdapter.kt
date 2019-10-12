package blog.photo.buildalbum.adapters

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
class IconAdapter : ArrayAdapter<String> {

    private var icons: List<Int?>? = null

    constructor(context: Context, texts: List<String?>, icons: List<Int?>) : super(
        context,
        android.R.layout.select_dialog_item,
        texts
    ) {
        this.icons = icons
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent)
        val textView = view.findViewById(android.R.id.text1) as TextView

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            textView.setCompoundDrawablesRelativeWithIntrinsicBounds(icons!![position]!!, 0, 0, 0)
        } else {
            textView.setCompoundDrawablesWithIntrinsicBounds(icons!![position]!!, 0, 0, 0)
        }
        textView.setTextColor(
            ContextCompat.getColor(
                context,
                R.color.colorWhite
            )
        )
        textView.compoundDrawablePadding =
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                12f,
                context.resources.displayMetrics
            ).toInt()
        return view
    }
}