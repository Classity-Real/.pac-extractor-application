import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

fun semverToCode(name: String): Int {
    val parts = name.removePrefix("v").removePrefix("V")
        .split(Regex("[^0-9]+"))
        .filter { it.isNotEmpty() }
        .mapNotNull { it.toIntOrNull() }
    val major = parts.getOrElse(0) { 0 }
    val minor = parts.getOrElse(1) { 0 }
    val patch = parts.getOrElse(2) { 0 }
    return major * 1_000_000 + minor * 1_000 + patch * 10
}

android {
    namespace = "dev.classityreal.pext"
    compileSdk = 35

    defaultConfig {
        applicationId = "dev.classityreal.pext"
        minSdk = 26
        targetSdk = 35

        val nameProp = (project.findProperty("appVersionName") as String?) ?: "2.0.3"
        val codeProp = (project.findProperty("appVersionCode") as String?)?.toIntOrNull()
        versionName = nameProp
        // Prefer CI-provided code; otherwise encode from versionName so upgrades work.
        versionCode = codeProp ?: maxOf(semverToCode(nameProp), 100)

        vectorDrawables.useSupportLibrary = true

        ndk {
            abiFilters += "arm64-v8a"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
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

    implementation(platform("androidx.compose:compose-bom:2024.10.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")

    implementation("androidx.compose.material3:material3:1.4.0")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.work:work-runtime-ktx:2.9.1")
    implementation("androidx.documentfile:documentfile:1.0.1")
    implementation("androidx.core:core-splashscreen:1.0.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
