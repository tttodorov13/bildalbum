/*
 * Copyright (c) 2018-2020 SAP and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 * Contributors:
 * SAP - initial API and implementation
 */

package blog.photo.albumbuild.images

import android.app.Activity
import androidx.appcompat.widget.Toolbar
import androidx.test.core.app.ActivityScenario
import blog.photo.albumbuild.R

fun <T : Activity> ActivityScenario<T>.getToolbarNavigationContentDescription(): String {
    var description = ""
    onActivity {
        description =
            it.findViewById<Toolbar>(R.id.toolbar).navigationContentDescription as String
    }
    return description
}