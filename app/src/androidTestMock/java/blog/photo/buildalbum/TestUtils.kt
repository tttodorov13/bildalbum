package blog.photo.buildalbum

import android.app.Activity
import androidx.appcompat.widget.Toolbar
import androidx.test.core.app.ActivityScenario

fun <T : Activity> ActivityScenario<T>.getImagesToolbarNavigationContentDescription(): String {
    var description = ""
    onActivity {
        description =
            it.findViewById<Toolbar>(R.id.images_toolbar).navigationContentDescription as String
    }
    return description
}

fun <T : Activity> ActivityScenario<T>.getCanvasesToolbarNavigationContentDescription(): String {
    var description = ""
    onActivity {
        description =
            it.findViewById<Toolbar>(R.id.canvases_toolbar).navigationContentDescription as String
    }
    return description
}
