package com.anggrayudi.hiddenapi.sample

import android.app.Application
import timber.log.Timber

/**
 * Created on 17/01/25
 *
 * @author Anggrayudi Hardiannico A.
 */
class App : Application() {

    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
    }
}
