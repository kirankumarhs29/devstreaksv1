import org.gradle.declarative.dsl.schema.FqName.Empty.packageName
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.googleServicesPlugin)
    alias(libs.plugins.sqlDelight)
    alias(libs.plugins.kotlinCocoapods)
    id("com.google.firebase.crashlytics")
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
            freeCompilerArgs = listOf(
                "-Xexpect-actual-classes" // Suppress expect/actual warning
            )
        }
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()
    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget> {
        binaries.framework {
            baseName = "Devstreaks" // 👈 Must match usage
            isStatic = false
        }
    }
    cocoapods {
        summary = "DevStreak Shared Code"
        homepage = "https://yourapp.dev"
        ios.deploymentTarget = "16.0"
        name = "Devstreaks"
        podfile = project.file("../iosApp/Podfile")
        pod("FirebaseCore"){
            extraOpts += listOf("-compiler-option", "-fmodules")
        }
        pod("FirebaseAuth"){
            extraOpts += listOf("-compiler-option", "-fmodules")
        }
        pod("GoogleUtilities") {
            extraOpts += listOf("-compiler-option", "-fmodules")
        }// Required by Firebase
        pod("FirebaseAnalytics"){
            extraOpts += listOf("-compiler-option", "-fmodules")
        }
        framework {
            baseName = "Devstreaks"
            isStatic = false
            @OptIn(ExperimentalKotlinGradlePluginApi::class)
            transitiveExport = true
//            freeCompilerArgs += listOf(
//                "-Xbinary=bundleId=com.dailydevchallenge.devstreaks"
//            )
        }
        xcodeConfigurationToNativeBuildType["CUSTOM_DEBUG"] = NativeBuildType.DEBUG
        xcodeConfigurationToNativeBuildType["CUSTOM_RELEASE"] = NativeBuildType.RELEASE

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
            implementation(libs.foundation)
            implementation(libs.foundation.layout)
//            implementation(libs.accompanist.pager)
//            implementation(libs.accompanist.pager.indicators)
        }
        commonMain.dependencies {
            implementation(libs.material.icons.extended)
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
//            implementation(libs.koin.compose.viewmodel)

            implementation(libs.multiplatform.settings)
            implementation(libs.multiplatform.settings.no.arg)
            implementation(libs.ktor.client.logging)

//            implementation(libs.lottie.compose)
            implementation(compose.foundation)

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
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        applicationId = "com.dailydevchallenge.devstreaks.android"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            firebaseCrashlytics {
                mappingFileUploadEnabled = true
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
dependencies {

    implementation(libs.androidx.runtime.android)
    implementation(compose.material)
    implementation(libs.work.runtime.ktx)

    debugImplementation(compose.uiTooling)
    implementation(libs.androidx.foundation.android)
}
sqldelight {
    databases {
        create("ChallengeDatabase") {
            packageName = "com.dailydevchallenge.devstreaks.database"
            version = 1
        }
    }
}