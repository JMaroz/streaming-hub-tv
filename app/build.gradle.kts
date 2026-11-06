import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "it.streaminghub.tv"
    compileSdk = 37

    val envVersionName = System.getenv("APP_VERSION_NAME")
        ?: (project.findProperty("versionName") as? String)
        ?: "1.1.1" // x-release-please-version

    val envVersionCode = System.getenv("APP_VERSION_CODE")?.toIntOrNull()
        ?: (project.findProperty("versionCode") as? String)?.toIntOrNull()
        ?: 1

    defaultConfig {
        applicationId = "it.streaminghub.tv"
        minSdk = 26
        targetSdk = 36
        versionCode = envVersionCode
        versionName = envVersionName

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            val props = project.rootProject.file("local.properties")
            val localProperties = Properties()
            if (props.exists()) {
                localProperties.load(props.inputStream())
            }
            val storeFilePath = (System.getenv("RELEASE_STORE_FILE")
                ?: localProperties.getProperty("RELEASE_STORE_FILE", "release-keystore.jks")).trim()
            storeFile = file(storeFilePath)
            storePassword = (System.getenv("RELEASE_STORE_PASSWORD")
                ?: localProperties.getProperty("RELEASE_STORE_PASSWORD", "")).trim()
            keyAlias = (System.getenv("RELEASE_KEY_ALIAS")
                ?: localProperties.getProperty("RELEASE_KEY_ALIAS", "")).trim()
            keyPassword = (System.getenv("RELEASE_KEY_PASSWORD")
                ?: localProperties.getProperty("RELEASE_KEY_PASSWORD", "")).trim()
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)

    // Jetpack Compose for TV
    implementation(libs.androidx.tv.foundation)
    implementation(libs.androidx.tv.material)


    // Compose Foundation & UI
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Media3 (ExoPlayer with HLS)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.exoplayer.hls)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.session)

    // Image loading
    implementation(libs.coil.compose)

    // Ktor Client Network Stack (Pure Kotlin)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.logging)

    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
}
