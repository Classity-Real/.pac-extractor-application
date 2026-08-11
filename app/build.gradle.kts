import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "dev.classityreal.pext"
    compileSdk = 35

    defaultConfig {
        applicationId = "dev.classityreal.pext"
        minSdk = 26
        targetSdk = 35
        // Overridden from CI via -PappVersionCode=… -PappVersionName=… (see build.yml) so
        // the "version" you type into the workflow's manual trigger actually becomes the
        // installed app's version, instead of this hardcoded fallback.
        versionCode = (project.findProperty("appVersionCode") as String?)?.toIntOrNull() ?: 1
        versionName = (project.findProperty("appVersionName") as String?) ?: "0.1.0"

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
            // No dedicated release keystore yet — sign with the debug key so a
            // "release" build type is still directly installable for testing.
            // Swap this for a real signingConfig before shipping to actual users.
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
    // composeOptions.kotlinCompilerExtensionVersion is no longer needed —
    // the org.jetbrains.kotlin.plugin.compose plugin (applied above) picks
    // a compiler version that matches the Kotlin Gradle plugin automatically.

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

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
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

    // Material 3 (stable). Material 3 Expressive — MaterialExpressiveTheme, bouncy
    // MotionScheme — is intentionally NOT used: as of 1.4.0 those APIs are still
    // alpha-only (material3 1.5.0-alpha+), not shipped in a stable release yet.
    implementation("androidx.compose.material3:material3:1.4.0")
    implementation("androidx.compose.material:material-icons-extended")

    // WorkManager for durable background extraction
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // Document tree / SAF helpers
    implementation("androidx.documentfile:documentfile:1.0.1")

    // Consistent animated splash screen back to minSdk, not just Android 12+
    // (where the platform SplashScreen API exists natively).
    implementation("androidx.core:core-splashscreen:1.0.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
