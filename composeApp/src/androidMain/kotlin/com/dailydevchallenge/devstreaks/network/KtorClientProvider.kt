package com.dailydevchallenge.devstreaks.network


import android.os.Build
import androidx.annotation.RequiresApi
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import java.time.*
import io.ktor.client.plugins.HttpTimeout

@RequiresApi(Build.VERSION_CODES.O)
actual fun getHttpClient(): HttpClient {
    return HttpClient(OkHttp) {
        engine {
            config {
                connectTimeout(Duration.ofSeconds(30))
                readTimeout(Duration.ofSeconds(60))
                writeTimeout(Duration.ofSeconds(60))
            }
        }

        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 90_000  // Total request timeout
            connectTimeoutMillis = 30_000
            socketTimeoutMillis = 60_000
        }
    }
}