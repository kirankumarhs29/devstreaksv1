plugins {
    alias(libs.plugins.androidApplication)
//    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeCompiler)
}


android {
    namespace = "com.dailydevchallenge.devstreaks.android"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.dailydevchallenge.devstreaks.android"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.runtime.android)
    implementation(libs.work.runtime.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.material3)
    implementation(libs.firebase.messaging.ktx)
    implementation(libs.google.firebase.crashlytics.ktx) // or whatever version you're using

}