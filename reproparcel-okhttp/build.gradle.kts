// SPDX-License-Identifier: AGPL-3.0-only
plugins {
    alias(libs.plugins.android.library)
    id("maven-publish")
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
                artifactId = "reproparcel-okhttp"
                pom {
                    name.set("reproparcel-okhttp")
                    description.set("Opt-in OkHttp request metadata for ReproParcel.")
                    url.set("https://github.com/awbuilds/reproparcel")
                    licenses {
                        license {
                            name.set("GNU Affero General Public License v3.0 only")
                            url.set("https://www.gnu.org/licenses/agpl-3.0.html")
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
    implementation(libs.okhttp)
    testImplementation(libs.junit)
    testImplementation(libs.okhttp.mockwebserver)
}
