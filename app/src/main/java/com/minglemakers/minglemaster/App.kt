package com.minglemakers.minglemaster

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import com.minglemakers.minglemaster.data.dataModule
import com.minglemakers.minglemaster.ui.uiModule

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@App)
            modules(
                dataModule,
                uiModule
            )
        }
    }
}