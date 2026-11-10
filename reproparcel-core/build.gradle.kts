// SPDX-License-Identifier: AGPL-3.0-only
plugins {
    alias(libs.plugins.binary.compatibility.validator)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    id("maven-publish")
}

kotlin {
    jvmToolchain(21)
    explicitApi()
}

java {
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("release") {
            from(components["java"])
            artifactId = "reproparcel-core"
            pom {
                name.set("ReproParcel Core")
                description.set("Versioned schema, limits, redaction, state and parcel contracts for ReproParcel.")
                url.set("https://github.com/awbuilds/reproparcel")
                licenses {
                    license {
                        name.set("GNU Affero General Public License v3.0 only")
                        url.set("https://www.gnu.org/licenses/agpl-3.0.html")
                        distribution.set("repo")
                    }
                }
                scm {
                    url.set("https://github.com/awbuilds/reproparcel")
                    connection.set("scm:git:https://github.com/awbuilds/reproparcel.git")
                    developerConnection.set("scm:git:ssh://git@github.com/awbuilds/reproparcel.git")
                }
                developers {
                    developer {
                        id.set("awbuilds")
                        name.set("awbuilds")
                        url.set("https://github.com/awbuilds")
                    }
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
