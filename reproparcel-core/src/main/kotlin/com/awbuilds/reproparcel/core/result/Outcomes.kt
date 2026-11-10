// SPDX-License-Identifier: AGPL-3.0-only
package com.awbuilds.reproparcel.core.result

import com.awbuilds.reproparcel.core.model.OmissionReason

public enum class ErrorCode {
    INVALID_CONFIG,
    NOT_INITIALIZED,
    DISABLED,
    ALREADY_INITIALIZED,
    CONFIG_MISMATCH,
    INVALID_TRANSITION,
    SERIALIZATION_FAILED,
    VALIDATION_FAILED,
    LIMIT_EXCEEDED,
    PROVIDER_TIMEOUT,
    PROVIDER_FAILED,
    ENCRYPTION_UNAVAILABLE,
    HOST_UNAVAILABLE,
    UNSUPPORTED,
    STORAGE_FAILED,
    EXPORT_FAILED,
    DUPLICATE_EXPORT,
    WRONG_THREAD,
    REPORTER_BUSY,
    INTERNAL_SAFE_FAILURE,
}

public sealed interface SdkResult {
    public data object Success : SdkResult
    public data class Omission(public val reason: OmissionReason) : SdkResult
    public data class Failure(public val code: ErrorCode) : SdkResult
    public data object Cancelled : SdkResult
    public data object Unsupported : SdkResult
    public data object HostUnavailable : SdkResult
    public data object NotInitialized : SdkResult
    public data object Disabled : SdkResult
}

public data class Diagnostic(
    public val code: ErrorCode,
    public val safeDetail: String? = null,
)
