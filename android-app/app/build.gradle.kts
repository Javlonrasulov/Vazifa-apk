import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21"
    id("com.google.gms.google-services")
}

android {
    namespace = "uz.vazifa.app"
    compileSdk = 35

    val localProperties = Properties().also { props ->
        val localFile = rootProject.file("local.properties")
        if (localFile.exists()) props.load(localFile.inputStream())
    }

    signingConfigs {
        create("release") {
            val storeFileName = localProperties.getProperty("RELEASE_STORE_FILE")
            if (!storeFileName.isNullOrBlank()) {
                storeFile = rootProject.file(storeFileName)
                storePassword = localProperties.getProperty("RELEASE_STORE_PASSWORD")
                keyAlias = localProperties.getProperty("RELEASE_KEY_ALIAS")
                keyPassword = localProperties.getProperty("RELEASE_KEY_PASSWORD")
            }
        }
    }

    defaultConfig {
        applicationId = "uz.vazifa.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 11
        versionName = "1.0.10"
    }

    buildTypes {
        debug {
            val localProperties = Properties()
            val localFile = rootProject.file("local.properties")
            if (localFile.exists()) localProperties.load(localFile.inputStream())
            val apiScheme = localProperties.getProperty("api.scheme", "http")
            val apiHost = localProperties.getProperty("api.host", "10.0.2.2")
            val apiPort = localProperties.getProperty("api.port", "3000").trim()
            val apiBase = if (apiPort.isEmpty()) {
                "$apiScheme://$apiHost/api/v1/"
            } else {
                "$apiScheme://$apiHost:$apiPort/api/v1/"
            }
            buildConfigField("String", "API_BASE_URL", "\"$apiBase\"")
            signingConfig = signingConfigs.getByName("release")
        }
        release {
            val localProperties = Properties()
            val localFile = rootProject.file("local.properties")
            if (localFile.exists()) localProperties.load(localFile.inputStream())
            val apiScheme = localProperties.getProperty("release.api.scheme", "https")
            val apiHost = localProperties.getProperty("release.api.host", "vazifa.liderplast.uz")
            val apiPort = localProperties.getProperty("release.api.port", "").trim()
            val apiBase = if (apiPort.isEmpty()) {
                "$apiScheme://$apiHost/api/v1/"
            } else {
                "$apiScheme://$apiHost:$apiPort/api/v1/"
            }
            buildConfigField("String", "API_BASE_URL", "\"$apiBase\"")
            signingConfig = signingConfigs.getByName("release")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-process:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.8.5")

    implementation("com.google.dagger:hilt-android:2.52")
    ksp("com.google.dagger:hilt-android-compiler:2.52")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.room:room-runtime:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    implementation("androidx.work:work-runtime-ktx:2.10.0")
    implementation("androidx.hilt:hilt-work:1.2.0")
    ksp("androidx.hilt:hilt-compiler:1.2.0")

    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-messaging-ktx")
    implementation("io.coil-kt:coil-compose:2.7.0")

    implementation("io.socket:socket.io-client:2.1.0") {
        exclude(group = "org.json", module = "json")
    }
}
