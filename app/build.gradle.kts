plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "dev.classityreal.pext"
    compileSdk = 35

    defaultConfig {
        applicationId = "dev.classityreal.pext"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        vectorDrawables.useSupportLibrary = true

        // libunpac.so / libpacextractor.so are only built for arm64-v8a.
        // They're real executables (see native/README notes), not JNI
        // shared libs, so there's no dlopen() fallback for other ABIs —
        // restrict the APK so it isn't installed somewhere they can't run.
        ndk {
            abiFilters += "arm64-v8a"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
    composeOptions {
        // Kotlin 2.0.20 uses the Compose compiler Gradle plugin normally,
        // pin explicitly here for clarity since that plugin isn't applied above.
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            // libunpac.so / libpacextractor.so are executables we run via
            // ProcessBuilder, not libraries we dlopen(). "Run from APK"
            // (the AGP default since API 23) mmaps libs directly out of the
            // zip and never gives them a real, executable on-disk path —
            // that breaks exec(). Legacy packaging forces them to be
            // extracted to nativeLibraryDir at install time instead.
            useLegacyPackaging = true
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2024.10.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")

    // Material 3 + Material 3 Expressive (1.4.x brings MaterialExpressiveTheme,
    // updated shapes/motion, loading indicators, button groups, etc.)
    implementation("androidx.compose.material3:material3:1.4.0")
    implementation("androidx.compose.material.icons:material-icons-extended:1.7.4")

    // WorkManager for durable background extraction
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // Document tree / SAF helpers
    implementation("androidx.documentfile:documentfile:1.0.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
