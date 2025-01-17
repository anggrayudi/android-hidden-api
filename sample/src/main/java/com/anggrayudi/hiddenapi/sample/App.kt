package com.anggrayudi.hiddenapi.sample

import androidx.multidex.MultiDexApplication
import timber.log.Timber

/**
 * Created on 17/01/25
 *
 * @author Anggrayudi Hardiannico A.
 */
class App : MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
    }
}
