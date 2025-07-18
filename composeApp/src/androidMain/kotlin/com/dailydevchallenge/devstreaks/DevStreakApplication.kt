package com.dailydevchallenge.devstreaks

import android.app.Application
import com.dailydevchallenge.devstreaks.di.platformModule
import com.dailydevchallenge.devstreaks.di.*
import com.dailydevchallenge.devstreaks.koin.authModule
import com.dailydevchallenge.devstreaks.llm.llmModule
import com.dailydevchallenge.devstreaks.session.initSessionManager
import com.dailydevchallenge.devstreaks.utils.initLogger
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin


class DevStreakApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        PDFBoxResourceLoader.init(applicationContext)
//        initLogger(this)
//        initSessionManager(this)
        FirebaseCrashlytics.getInstance().isCrashlyticsCollectionEnabled = true


        startKoin {
            androidContext(this@DevStreakApplication)
            printLogger()
            modules(
                platformModule(this@DevStreakApplication),
                authModule,
                databaseModule,
                repositoryModule,
                llmModule,
                viewModelModule,
            )
        }
    }
}
