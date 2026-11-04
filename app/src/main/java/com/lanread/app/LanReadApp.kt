package com.lanread.app

import android.app.Application
import com.lanread.app.ads.AdMobManager
import com.lanread.app.di.AppContainer

class LanReadApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        AdMobManager.initialize(this)
    }
}
