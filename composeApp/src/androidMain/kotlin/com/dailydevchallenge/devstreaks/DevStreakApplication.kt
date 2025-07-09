package com.dailydevchallenge.devstreaks

import android.app.Application
import com.dailydevchallenge.devstreaks.di.platformModule
import com.dailydevchallenge.devstreaks.di.*
import com.dailydevchallenge.devstreaks.koin.authModule
import com.dailydevchallenge.devstreaks.llm.llmModule
import com.google.firebase.crashlytics.FirebaseCrashlytics
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin


class DevStreakApplication : Application() {

    override fun onCreate() {
        super.onCreate()
//        initLogger(this@DevStreakApplication)
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
