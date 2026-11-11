#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Build a clean consumer against the locally published candidate artifacts.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
REPO="$ROOT/build/local-repo"
TMP="$(mktemp -d "${TMPDIR:-/tmp}/reproparcel-consumer.XXXXXX")"
trap 'rm -rf "$TMP"' EXIT

if [[ ! -d "$REPO" ]]; then
  echo "local repo missing; run the publish tasks first" >&2
  exit 1
fi

mkdir -p "$TMP/app" "$TMP/gradle"
cp "$ROOT/local.properties" "$TMP/local.properties"

cat > "$TMP/settings.gradle.kts" <<EOF
pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven { url = uri("$REPO") }
        google()
        mavenCentral()
    }
}
rootProject.name = "reproparcel-consumer"
include(":app")
EOF

cat > "$TMP/build.gradle.kts" <<'EOF'
plugins {
    id("com.android.application") version "9.5.1" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.21" apply false
}
EOF

cat > "$TMP/gradle.properties" <<'EOF'
android.useAndroidX=true
org.gradle.jvmargs=-Xmx2g
EOF

cat > "$TMP/app/build.gradle.kts" <<'EOF'
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.example.consumer"
    compileSdk = 37
    defaultConfig {
        applicationId = "com.example.consumer"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true }
}

dependencies {
    implementation("io.github.awbuilds:reproparcel-core:0.0.1-SNAPSHOT")
    implementation("io.github.awbuilds:reproparcel-android:0.0.1-SNAPSHOT")
    implementation("io.github.awbuilds:reproparcel-compose:0.0.1-SNAPSHOT")
    implementation("io.github.awbuilds:reproparcel-okhttp:0.0.1-SNAPSHOT")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation(platform("androidx.compose:compose-bom:2026.09.00"))
    implementation("androidx.compose.material3:material3")
}
EOF

mkdir -p "$TMP/app/src/main"
cat > "$TMP/app/src/main/AndroidManifest.xml" <<'EOF'
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application android:allowBackup="false" android:label="Consumer" />
</manifest>
EOF

cd "$ROOT"
./gradlew -p "$TMP" :app:assembleDebug --refresh-dependencies
echo "consumer-smoke: PASS"
