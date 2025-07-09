package com.dailydevchallenge.devstreaks.koin

import com.dailydevchallenge.devstreaks.auth.AuthService
import com.dailydevchallenge.devstreaks.auth.AuthServiceAndroid
import org.koin.dsl.module

val authModule = module {
    single<AuthService> { AuthServiceAndroid() }
}
