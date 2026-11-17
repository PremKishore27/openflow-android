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
        versionCode = 7
        versionName = "0.7"
        // Whisper on-device is only practical on 64-bit phones, which keeps the APK smaller too.
        ndk { abiFilters += "arm64-v8a" }
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
    // On-device speech recognition (Apache-2.0). Fetched by scripts/fetch-sherpa-onnx.sh, not committed.
    implementation(files("libs/sherpa-onnx-2.0.1.aar"))
    testImplementation("junit:junit:4.13.2")
}

val fetchSherpaOnnx = tasks.register<Exec>("fetchSherpaOnnx") {
    description = "Downloads the pinned sherpa-onnx library if it is missing."
    onlyIf { !file("libs/sherpa-onnx-2.0.1.aar").exists() }
    commandLine("bash", "${rootProject.projectDir}/scripts/fetch-sherpa-onnx.sh")
}
tasks.named("preBuild") { dependsOn(fetchSherpaOnnx) }
