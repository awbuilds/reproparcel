// SPDX-License-Identifier: AGPL-3.0-only
plugins {
    alias(libs.plugins.binary.compatibility.validator)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvmToolchain(21)
    explicitApi()
}

dependencies {
    api(libs.kotlinx.serialization.json)
    api(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit)
    testImplementation(libs.networknt.json.schema.validator)
}

tasks.test {
    systemProperty("reproparcel.root", rootProject.projectDir.absolutePath)
    testLogging {
        events("failed", "skipped")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}
