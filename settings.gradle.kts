// SPDX-License-Identifier: AGPL-3.0-only

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
include(":spike:capture-lab")
