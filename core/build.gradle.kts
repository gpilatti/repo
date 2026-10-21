plugins {
    id("org.jetbrains.kotlin.jvm")
}

// Target Java 17 bytecode so the engine can be consumed by the Android app,
// without requiring a specific JDK to be installed.
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

    testImplementation(kotlin("test"))
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    // Used only to read the JSON scenario fixtures in tests (no compiler plugin needed).
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
}

tasks.test {
    useJUnit()
    testLogging {
        events("failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
    // The evaluation tests write metrics reports here. Declared as outputs so the
    // reports are restored when Gradle reuses cached test results.
    val reports = layout.buildDirectory.dir("reports")
    systemProperty("evaluation.report", reports.get().file("evaluation.md").asFile.absolutePath)
    outputs.file(reports.map { it.file("evaluation.md") })
    outputs.file(reports.map { it.file("holdout.md") })
}
