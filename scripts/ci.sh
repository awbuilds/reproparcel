#!/usr/bin/env bash
# SPDX-License-Identifier: AGPL-3.0-only
# Local CI gate. No device/emulator tests are run here; those are recorded
# separately in the phase evidence.
set -euo pipefail

cd "$(dirname "$0")/.."

echo "== public hygiene =="
./scripts/public-hygiene.sh --all

echo "== whitespace =="
git diff --check -- . ':(exclude)reproparcel-core/api/reproparcel-core.api'

echo "== dependency inventory ==="
python3 scripts/dependency_inventory.py

echo "== dependency allowlist =="
python3 scripts/check_dependencies.py

echo "== compile and JVM tests =="
./gradlew \
  :reproparcel-core:test \
            :reproparcel-core:apiCheck \
  :reproparcel-android:assembleDebug \
            :reproparcel-android:assembleDebugAndroidTest \
  :reproparcel-android:testDebugUnitTest \
  :reproparcel-compose:assembleDebug \
            :reproparcel-compose:assembleDebugAndroidTest \
  :reproparcel-okhttp:assembleDebug \
  :sample-app:assembleDebug \
  :sample-app:assembleRelease \
  :benchmark:assembleDebug

echo "== desktop tests =="
(cd desktop && npm test)

echo "== relay tests =="
(cd server && npm test)

echo "== lint =="
./gradlew lint

echo "ci: PASS"
