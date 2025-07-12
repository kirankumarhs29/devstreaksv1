// shared/src/iosMain/kotlin/com/dailydevchallenge/di/iOSModule.kt
package com.dailydevchallenge.devstreaks.di


import com.dailydevchallenge.devstreaks.auth.AuthService
import com.dailydevchallenge.devstreaks.auth.IOSAuthService
import com.dailydevchallenge.devstreaks.database.DatabaseDriverFactory
import com.dailydevchallenge.devstreaks.notification.IOSNotificationScheduler
import com.dailydevchallenge.devstreaks.notification.NotificationScheduler
import com.dailydevchallenge.devstreaks.session.SessionManager
import com.dailydevchallenge.devstreaks.session.IOSSessionManager
import org.koin.dsl.module
// AuthProvider from swift


val iosModule = module {
    single { DatabaseDriverFactory() }
    single<AuthService> { IOSAuthService }
    single<SessionManager> { IOSSessionManager() }
    single<NotificationScheduler> { IOSNotificationScheduler() }


}
