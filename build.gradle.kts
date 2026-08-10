// Top-level build file
plugins {
    id("com.android.application") version "8.6.0" apply false
    id("org.jetbrains.kotlin.android") version "2.0.20" apply false
    // Required since Kotlin 2.0: Compose no longer gets its compiler
    // bundled into the Kotlin plugin, it needs this plugin applied
    // wherever buildFeatures.compose = true is set (see app/build.gradle.kts).
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.20" apply false
}
