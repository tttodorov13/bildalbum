/*
 * Copyright (c) 2018-2020 SAP and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 * Contributors:
 * SAP - initial API and implementation
 */
package blog.photo.buildalbum.imagedetail

import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import blog.photo.buildalbum.data.Image

/**
 * [BindingAdapter]s for the [ImageDetailFragment] [Image]s canvas list.
 */
@BindingAdapter("android:canvasItems")
fun setItems(listView: RecyclerView, items: List<Image>?) {
    items?.let {
        (listView.adapter as CanvasesAdapter).submitList(items)
    }
}