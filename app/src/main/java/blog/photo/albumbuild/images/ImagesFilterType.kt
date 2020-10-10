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

/**
 * Used with the filter spinner in the images list.
 */
enum class ImagesFilterType {
    /**
     * Do not filter images.
     */
    ALL_IMAGES,

    /**
     * Filters only the added (not edited yet) images.
     */
    ADDED_IMAGES,

    /**
     * Filters only the edited images.
     */
    EDITED_IMAGES,

    /**
     * Filters only the favorite images.
     */
    FAVORITE_IMAGES
}
