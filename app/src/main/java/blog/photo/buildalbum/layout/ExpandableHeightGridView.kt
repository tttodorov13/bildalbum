package blog.photo.buildalbum.layout

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.GridView

/**
 * Class to manage render frames in image screen.
 */
class ExpandableHeightGridView : GridView {

    var isExpanded = false

    constructor(context: Context) : super(context) {}

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {}

    constructor(
        context: Context, attrs: AttributeSet,
        defStyle: Int
    ) : super(context, attrs, defStyle) {
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (isExpanded) {
            // Calculate entire height by providing a very large height hint.
            // View.MEASURED_SIZE_MASK represents the largest height possible.
            val expandSpec = MeasureSpec.makeMeasureSpec(
                View.MEASURED_SIZE_MASK,
                MeasureSpec.AT_MOST
            )
            super.onMeasure(widthMeasureSpec, expandSpec)

            val params = layoutParams
            params.height = measuredHeight
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        }
    }
}