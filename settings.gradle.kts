pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    // Plugin versions live in the root build.gradle.kts, so every module shares one copy.
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "guardiao"

// The detection engine is plain Kotlin and builds anywhere a JDK is available.
include(":core")

// The Android app needs the Android SDK. It is included only when an SDK is
// configured, so the engine can still be built and tested on machines (or CI
// sandboxes) without one. See README.md.
val localProperties = file("local.properties")
val hasSdkDir = localProperties.exists() && localProperties.readText().contains("sdk.dir")
val hasAndroidSdk = hasSdkDir ||
    !System.getenv("ANDROID_HOME").isNullOrBlank() ||
    !System.getenv("ANDROID_SDK_ROOT").isNullOrBlank()
if (hasAndroidSdk) {
    include(":app")
} else {
    logger.lifecycle("Android SDK not found: building :core only. Set ANDROID_HOME to build :app.")
}
