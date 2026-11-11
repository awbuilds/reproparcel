#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Local release-candidate verification. This does NOT publish to Maven Central.
set -euo pipefail
cd "$(dirname "$0")/.."

./gradlew \
  :reproparcel-core:publishReleasePublicationToLocalVerificationRepository \
  :reproparcel-android:publishReleasePublicationToLocalVerificationRepository \
  :reproparcel-compose:publishReleasePublicationToLocalVerificationRepository \
  :reproparcel-okhttp:publishReleasePublicationToLocalVerificationRepository

./scripts/consumer-smoke.sh
echo "release-local: PASS (local repository only; no external publication)"
