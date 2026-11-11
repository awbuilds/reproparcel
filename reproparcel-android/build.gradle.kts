// SPDX-License-Identifier: Apache-2.0
plugins {
    alias(libs.plugins.android.library)
    id("maven-publish")
}

android {
    namespace = "com.awbuilds.reproparcel.android"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                artifactId = "reproparcel-android"
                pom {
                    name.set("ReproParcel Android")
                    description.set("Android lifecycle, capture, encrypted draft and export runtime for ReproParcel.")
                    url.set("https://github.com/awbuilds/reproparcel")
                    licenses {
                        license {
                            name.set("Apache License, Version 2.0")
                            url.set("https://www.apache.org/licenses/LICENSE-2.0")
                        }
                    }
                    scm {
                        url.set("https://github.com/awbuilds/reproparcel")
                        connection.set("scm:git:https://github.com/awbuilds/reproparcel.git")
                    }
                }
            }
        }
        repositories {
            maven {
                name = "localVerification"
                url = uri(rootProject.layout.buildDirectory.dir("local-repo"))
            }
        }
    }
}

dependencies {
    api(project(":reproparcel-core"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.kotlinx.coroutines.core)
}
