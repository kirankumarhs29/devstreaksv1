package com.dailydevchallenge.devstreaks.auth


actual fun getAuthService(): AuthService {
    return IOSAuthService
}
