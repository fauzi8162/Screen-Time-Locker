plugins {
    // Dengan AGP 9.0.0+, dukungan Kotlin sudah terintegrasi secara bawaan (built-in).
    // Kita tidak perlu lagi menerapkan plugin "org.jetbrains.kotlin.android" secara manual.
    id("com.android.application")
}

android {
    namespace = "com.example.locktimer"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.locktimer"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    
    compileOptions {
        // AGP 9.0 secara otomatis menyelaraskan target Kotlin JVM dengan versi Java compileOptions di bawah ini
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
}
