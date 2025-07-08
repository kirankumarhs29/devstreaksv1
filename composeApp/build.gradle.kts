import org.gradle.declarative.dsl.schema.FqName.Empty.packageName
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.googleServicesPlugin)
    alias(libs.plugins.sqlDelight)
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
    
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "composeApp"
            isStatic = true
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.ktor.client.android)
            implementation(libs.koin.android)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.androidx.core.splashscreen)
            implementation(libs.androidx.material)
            implementation(project.dependencies.platform(libs.firebase.bom))
            implementation(libs.sqldelight.android)
            implementation(libs.androidx.security.crypto)
            implementation(libs.androidx.datastore.preferences)
            implementation(libs.material)
            implementation(libs.firebase.crashlytics.ktx)
            implementation(libs.google.firebase.analytics.ktx)
            implementation(libs.com.google.firebase.firebase.auth.ktx)
            implementation(libs.google.firebase.firestore.ktx)
            implementation(libs.firebase.messaging)
        }
        commonMain.dependencies {
            implementation(libs.navigation.compose)
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(libs.kotlinx.datetime)

            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)

            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)

            implementation(libs.multiplatform.settings)
            implementation(libs.multiplatform.settings.no.arg)
            implementation(libs.ktor.client.logging)

//            implementation(libs.lottie.compose)
            implementation(compose.foundation)
            implementation(libs.foundation)
            implementation(libs.foundation.layout)
//            implementation(libs.accompanist.pager)
//            implementation(libs.accompanist.pager.indicators)
            implementation(libs.material.icons.extended)
            implementation(libs.calf.file.picker)


        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            implementation(libs.sqldelight.ios)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "com.dailydevchallenge.devstreaks"
    compileSdk = 35
    defaultConfig {
        minSdk = 24
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
dependencies {

    implementation(libs.androidx.runtime.android)
    implementation(compose.material)
    debugImplementation(compose.uiTooling)
    implementation(libs.androidx.foundation.android)
}
sqldelight {
    databases {
        create("ChallengeDatabase") {
            packageName = "com.dailydevchallenge.database"
            version = 1
        }
    }
}