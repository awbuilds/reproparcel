# Phase 1 evidence

Retained, sanitized evidence only. No real user data or private application
information is present.

| File | Meaning |
|---|---|
| `core-tests.txt` | JUnit XML summaries for 37 core JVM tests |
| `runtime-api26.xml`, `runtime-api30.xml`, `runtime-api37.xml` | 11 Android runtime tests per API |
| `sample-api26.xml`, `sample-api30.xml`, `sample-api37.xml` | 2 sample UI tests per API |
| `ci.txt` | `scripts/ci.sh` output (absolute paths redacted) |
| `public-hygiene.txt` / `prd-audit.txt` | Phase gate outputs |

Commands:

```sh
./scripts/ci.sh
ANDROID_SERIAL=<device> ./gradlew :reproparcel-android:connectedDebugAndroidTest
ANDROID_SERIAL=<device> ./gradlew :sample-app:connectedDebugAndroidTest
python3 scripts/prd_audit.py --phase 1
```

Emulator success is not physical-device or release evidence.
