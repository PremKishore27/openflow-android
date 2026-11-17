plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.openflow.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.openflow.android"
        minSdk = 26
        targetSdk = 35
        versionCode = 4
        versionName = "0.4"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
    testImplementation("junit:junit:4.13.2")
}
