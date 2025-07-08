package com.dailydevchallenge.devstreaks

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform