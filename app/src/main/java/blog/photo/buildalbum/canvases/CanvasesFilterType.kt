/*
 * Copyright (c) 2018-2020 SAP and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 * Contributors:
 * SAP - initial API and implementation
 */
package blog.photo.buildalbum.canvases

/**
 * Used with the filter spinner in the canvases list.
 */
enum class CanvasesFilterType {
    /**
     * Do not filter canvases.
     */
    ALL_CANVASES,

    /**
     * Filters only the favorite canvases.
     */
    FAVORITE_CANVASES
}
