// Load the Kotlin and Android Gradle plugins once, here in the root project, so
// :core and :app share a single copy. Loading them separately in each module
// triggers Gradle's "Kotlin Gradle plugin was loaded multiple times" warning.
// The Android plugin is only added when :app is part of the build, so :core
// still builds on machines without the Android SDK (see settings.gradle.kts).
buildscript {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.2.21")
        classpath("org.jetbrains.kotlin:compose-compiler-gradle-plugin:2.2.21")
        if (rootProject.findProject(":app") != null) {
            classpath("com.android.tools.build:gradle:8.13.0")
        }
    }
}
