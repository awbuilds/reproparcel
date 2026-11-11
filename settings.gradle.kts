// SPDX-License-Identifier: Apache-2.0

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "reproparcel"

include(":reproparcel-core")
include(":reproparcel-android")
include(":reproparcel-compose")
include(":reproparcel-okhttp")
include(":sample-app")
include(":sample-baseline")
include(":benchmark")
include(":spike:capture-lab")
