# Album Build v1
<p align="center">
<img src="https://drive.google.com/uc?export=view&id=1rwU7K7IBWugih1tv9Pkl9aSZ17V-bV3h" alt="Image by Album Build"/>
</p>

Album Build is a project to showcase different architectural approaches to developing Android apps. In its different branches you'll find the same app (an AlbumBuild app) implemented with small differences.

In this branch you'll find:
*   Kotlin **[Coroutines](https://kotlinlang.org/docs/reference/coroutines-overview.html)** for background operations.
*   A single-activity architecture, using the **[Navigation component](https://developer.android.com/guide/navigation/navigation-getting-started)** to manage fragment operations.
*   A presentation layer that contains a fragment (View) and a **ViewModel** per screen (or feature).
*   Reactive UIs using **LiveData** observables and **Data Binding**.
*   A **data layer** with a repository and two data sources (local using Room and remote) that are queried with one-shot operations (no listeners or data streams).
*   Two **product flavors**, `mock` and `prod`, [to ease development and testing](https://android-developers.googleblog.com/2015/12/leveraging-product-flavors-in-android.html) (except in the Dagger branch).
*   A collection of unit, integration and e2e **tests**, including "shared" tests that can be run on emulator/device or Robolectric.

## Variations

This project hosts each sample app in separate repository branches. For more information, see the `README.md` file in each branch.

### Stable samples - Kotlin
|     Sample     | Description |
| ------------- | ------------- |
| [master](https://github.com/tttodorov13/albumbuild/tree/master) | The base for the rest of the branches. <br/>Uses Kotlin, Architecture Components, coroutines, Data Binding, etc. |

### Samples in development - Kotlin

| Sample | Description |
| ------------- | ------------- |
| [reactive-master](https://github.com/tttodorov13/albumbuild/tree/reactive_master)<br/>[[compare](https://github.com/tttodorov13/albumbuild/tree/reactive_master#files_bucket)] | Modifies the data layer so UIs react to changes automatically using Room as source of truth. |

## Why an Album Build app?

<img align="right" src="https://drive.google.com/uc?export=view&id=1W7VH45LqhP3Ew7ycf2JdKXZDAMukVfa0" alt="A screenshot illustraating the UI of the app" width="288" height="512" style="display: inline; float: right"/>

The app in this project aims to be simple enough that you can understand it quickly, but complex enough to showcase difficult design decisions and testing scenarios.

## What is it not?

*   A UI/Material Design sample. The interface of the app is deliberately kept simple to focus on architecture. Check out [Plaid](https://github.com/android/plaid) instead.
*   A complete Jetpack sample covering all libraries. Check out [Android Sunflower](https://github.com/googlesamples/android-sunflower) or the advanced [Github Browser Sample](https://github.com/googlesamples/android-architecture-components/tree/master/GithubBrowserSample) instead.

## Who is it for?

*   Intermediate developers and beginners looking for a way to structure their app in a testable and maintainable way.
*   Advanced developers looking for quick reference.

## Opening a sample in Android Studio

To open one of the samples in Android Studio, begin by checking out one of the sample branches, and then open the root directory in Android Studio. The following series of steps illustrate how to open the [usecases](tree/usecases/) sample.

Clone the repository:

```
git clone git@github.com:tttodorov13/albumbuild.git
```
This step checks out the master branch. If you want to change to a different sample: 

```
git checkout usecases
```

**Note:** To review a different sample, replace `usecases` with the name of sample you want to check out.

Finally open the `albumbuild/` directory in Android Studio.

### License


```
Copyright (c) 2018-2020 SAP and others.
All rights reserved. This program and the accompanying materials
are made available under the terms of the Eclipse Public License v2.0
which accompanies this distribution, and is available at
http://www.eclipse.org/legal/epl-v20.html
Contributors:
SAP - initial API and implementation
```

### Proudly powered by [Android Architecture Blueprints v2](https://github.com/android/architecture-samples)
