// SPDX-License-Identifier: AGPL-3.0-only
plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.awbuilds.reproparcel.okhttp"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    api(project(":reproparcel-core"))
    implementation(libs.okhttp)
    testImplementation(libs.junit)
}
