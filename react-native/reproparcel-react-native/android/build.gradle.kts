// SPDX-License-Identifier: Apache-2.0
plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.awbuilds.reproparcel.rn"
    compileSdk = 37
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    // The host app must include the published Android artifacts and React Native.
    implementation("io.github.awbuilds:reproparcel-android:0.0.1-SNAPSHOT")
    implementation("io.github.awbuilds:reproparcel-compose:0.0.1-SNAPSHOT")
    implementation("com.facebook.react:react-android")
}
