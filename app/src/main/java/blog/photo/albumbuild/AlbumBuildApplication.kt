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

import android.app.Application
import blog.photo.albumbuild.data.source.ImagesRepository
import timber.log.Timber
import timber.log.Timber.DebugTree

/**
 * An application that lazily provides a repository. Note that this Service Locator pattern is
 * used to simplify the sample. Consider a Dependency Injection framework.
 *
 * Also, sets up Timber in the DEBUG BuildConfig. Read Timber's documentation for production setups.
 */
class AlbumBuildApplication : Application() {

    // Depends on the flavor,
    val imageRepository: ImagesRepository
        get() = ServiceLocator.provideImagesRepository(this)

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) Timber.plant(DebugTree())
    }
}
