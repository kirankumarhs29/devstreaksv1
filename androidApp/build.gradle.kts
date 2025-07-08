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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation(projects.composeApp)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.runtime.android)
}