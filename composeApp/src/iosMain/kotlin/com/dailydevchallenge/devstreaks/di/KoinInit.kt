package com.dailydevchallenge.devstreaks.di

import org.koin.core.context.startKoin
import org.koin.core.module.Module

fun initKoin(vararg modules: Module) {
    startKoin {
        modules(listOf(appModule) + modules)
    }
}