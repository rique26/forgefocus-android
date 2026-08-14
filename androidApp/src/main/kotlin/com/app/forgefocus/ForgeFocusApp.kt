package com.app.forgefocus

import android.app.Application
import com.app.forgefocus.di.initKoin
import org.koin.android.ext.koin.androidContext

class ForgeFocusApp : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@ForgeFocusApp)
        }
    }
}