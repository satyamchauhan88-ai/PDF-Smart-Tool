
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.pdfsmart.toolkit"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.pdfsmart.toolkit"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Android Core
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")

    // PDF Processing
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")

    // English OCR
    implementation("com.google.mlkit:text-recognition:16.0.1")

    // Hindi OCR (Devanagari)
    implementation("com.google.mlkit:text-recognition-devanagari:16.0.1")
}
