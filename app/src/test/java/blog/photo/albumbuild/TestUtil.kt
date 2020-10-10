/*
 * Copyright (c) 2018-2020 SAP and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 * Contributors:
 * SAP - initial API and implementation
 */

package blog.photo.albumbuild

import androidx.lifecycle.LiveData
import org.junit.Assert.assertEquals

fun assertImageLiveDataEventTriggered(
    liveData: LiveData<Event<Int>>,
    imageId: Int
) {
    val value = liveData.getOrAwaitValue()
    assertEquals(value.getContentIfNotHandled(), imageId)
}

fun assertCanvasLiveDataEventTriggered(
    liveData: LiveData<Event<Int>>,
    imageId: Int
) {
    val value = liveData.getOrAwaitValue()
    assertEquals(value.getContentIfNotHandled(), imageId)
}

fun assertSnackbarMessage(snackbarLiveData: LiveData<Event<Int>>, messageId: Int) {
    val value: Event<Int> = snackbarLiveData.getOrAwaitValue()
    assertEquals(value.getContentIfNotHandled(), messageId)
}
