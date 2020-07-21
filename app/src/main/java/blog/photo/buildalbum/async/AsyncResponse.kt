/*
 * Copyright (c) 2018-2020 SAP and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 * Contributors:
 * SAP - initial API and implementation
 */
package blog.photo.buildalbum.async

/**
 * Asynchronous responses.
 */
interface AsyncResponse {
    /**
     * Mark task begin execution.
     */
    fun onTaskBegin()

    /**
     * Mark task end execution.
     */
    fun onTaskComplete(stringId: Int)
}