# Fault-injection and privacy-omission evidence

Date: 2026-11-11
Device: API 37.2 emulator (synthetic data only).

| Failure | Expected result | Test |
|---|---|---|
| Invalid config | `INVALID_CONFIG`, runtime stays uninitialized | `FaultInjectionTest.invalidConfigIsTypedAndLeavesRuntimeUninitialized` |
| Denied/secure window | `SECURE_WINDOW` omission | `CaptureControllerTest.secureWindowIsOmitted` |
| Out-of-bounds mask | `UNSUPPORTED_SURFACE` omission before copy | `CaptureControllerTest.outOfBoundsMaskIsOmittedWithoutCapturing` |
| Policy change/stale attempt | `POLICY_CHANGED`/`STALE_FRAME` | `CaptureControllerTest.mismatchedPolicyVersionIsRejected`, `captureAttemptValidationRejectsStaleOrChangedState` |
| Capture timeout | typed timeout without crash; bitmap ownership preserved | `CaptureControllerTest.timeoutHelperReturnsNullWithoutCrashing` |
| Missing/finished activity | `HOST_UNAVAILABLE` | `ReporterFlowTest.finishedHostReturnsHostUnavailableFromReporterEntry` |
| Missing sensor | explicit `false` start, stop is safe | `FaultInjectionTest.unavailableSensorIsAnExplicitFalseStart` |
| Revoked/unwritable URI | `EXPORT_FAILED`, no crash | `FaultInjectionTest.revokedContentUriReturnsTypedExportFailureWithoutCrashing` |
| Corrupt/tampered draft | typed failure, no plaintext | `DraftStoreTest.tamperedCiphertextFailsClosed`, `deletedKeyFailsClosed` |
| Storage failure/full disk analogue | `STORAGE_FAILED`, no plaintext fallback | `FaultInjectionTest.storageFailureReturnsTypedDraftFailureWithoutPlaintextFallback` |
| Provider exception/timeout | typed `PROVIDER_FAILED`, host not crashed | `RuntimeTest.providerFailureIsIsolatedAndTyped`, `providerTimeoutIsAnOmission` |
| Disconnected network | original `IOException` preserved | `MetadataInterceptorTest.disconnectedNetworkExceptionIsPreservedUnchanged` |
| Serialization failure | writer returns no parcel | `ParcelWriterTest.rejectsBlankDescription`, `oversizedEntryIsRejected` |
| Cancellation | SDK-owned job cancelled on disable | `RuntimeTest.disableCancelsSdkOwnedJob` |
| Seeded unsafe attributes | dropped before buffer | `RuntimeTest.unsafeAttributesAreNotBuffered`, `RedactorTest` |

These are local synthetic tests, not proof of crash-free behavior on all devices.
A physical-device fault/thermal matrix and TalkBack manual pass remain blocked.
