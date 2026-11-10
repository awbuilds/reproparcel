// SPDX-License-Identifier: AGPL-3.0-only
package com.awbuilds.reproparcel.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

public const val SCHEMA_VERSION: Int = 1

@Serializable
public data class BuildInfo(
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val variant: String,
    val gitCommit: String? = null,
)

@Serializable
public data class AppInfo(
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val variant: String,
    val gitCommit: String? = null,
)

@Serializable
public data class DeviceInfo(
    val apiLevel: Int,
    val manufacturer: String,
    val model: String,
    val windowWidthPx: Int,
    val windowHeightPx: Int,
    val densityDpi: Int,
    val locale: String,
    val fontScale: Float,
    val orientation: String,
)

@Serializable
public data class ProducerInfo(
    val sdkVersion: String,
    val platform: String = "android",
    val collectionConfigurationId: String,
)

@Serializable
public data class PrivacyInfo(
    val policyVersion: String,
    val redactionMode: String,
    val reviewedCategories: Set<String> = emptySet(),
    val previewContentHash: String? = null,
)

@Serializable
public data class CaptureInfo(
    val capturedAt: String,
    val collectionModes: Set<String> = emptySet(),
    val triggerOffsetMs: Long? = null,
    val snapshotCutoffMs: Long? = null,
    val providers: List<ProviderStatus> = emptyList(),
)

@Serializable
public data class ProviderStatus(
    val providerId: String,
    val status: EvidenceStatus,
)

@Serializable
public data class EvidenceStatus(
    val state: EvidenceState,
    val reason: OmissionReason? = null,
    val detail: String? = null,
)

@Serializable
public enum class EvidenceState {
    @SerialName("collected")
    COLLECTED,

    @SerialName("omitted")
    OMITTED,

    @SerialName("truncated")
    TRUNCATED,

    @SerialName("unsupported")
    UNSUPPORTED,
}

@Serializable
public enum class OmissionReason {
    @SerialName("policy_disabled")
    POLICY_DISABLED,

    @SerialName("secure_window")
    SECURE_WINDOW,

    @SerialName("unsupported_surface")
    UNSUPPORTED_SURFACE,

    @SerialName("capture_timeout")
    CAPTURE_TIMEOUT,

    @SerialName("capture_cancelled")
    CAPTURE_CANCELLED,

    @SerialName("policy_changed")
    POLICY_CHANGED,

    @SerialName("stale_frame")
    STALE_FRAME,

    @SerialName("provider_failed")
    PROVIDER_FAILED,

    @SerialName("encryption_unavailable")
    ENCRYPTION_UNAVAILABLE,

    @SerialName("size_limit")
    SIZE_LIMIT,

    @SerialName("serialization_failed")
    SERIALIZATION_FAILED,

    @SerialName("host_unavailable")
    HOST_UNAVAILABLE,

    @SerialName("capability_unsupported")
    CAPABILITY_UNSUPPORTED,

    @SerialName("invalid_config")
    INVALID_CONFIG,

    @SerialName("not_initialized")
    NOT_INITIALIZED,

    @SerialName("disabled")
    DISABLED,
}

@Serializable
public data class EntryInfo(
    val path: String,
    val mediaType: String,
    val uncompressedSize: Long,
    val sha256: String,
)

@Serializable
public data class ParcelLimits(
    val maxCompressedBytes: Long = 5L * 1024 * 1024,
    val maxUncompressedBytes: Long = 10L * 1024 * 1024,
    val maxEntries: Int = 16,
    val maxCompressionRatio: Int = 100,
    val maxImageLongEdgePx: Int = 1600,
    val maxDecodedImageBytes: Long = 12L * 1024 * 1024,
    val maxConcurrentDecodedImageBytes: Long = 24L * 1024 * 1024,
    val maxJsonBytes: Long = 2L * 1024 * 1024,
    val maxJsonDepth: Int = 32,
    val maxJsonNodes: Int = 10_000,
)

@Serializable
public data class Manifest(
    @SerialName("schemaVersion")
    val schemaVersion: Int = SCHEMA_VERSION,
    val reportId: String,
    val createdAt: String,
    val producer: ProducerInfo,
    val app: AppInfo,
    val device: DeviceInfo,
    val privacy: PrivacyInfo,
    val capture: CaptureInfo,
    val entries: List<EntryInfo>,
    val limits: ParcelLimits = ParcelLimits(),
)

@Serializable
public data class Report(
    val title: String = "",
    val description: String,
    val expectedBehavior: String = "",
    val actualBehavior: String = "",
    val reproSteps: String? = null,
    val screenId: String? = null,
    val evidence: List<EvidenceStatus> = emptyList(),
)

@Serializable
public data class ReportEvent(
    val sequence: Long,
    val offsetMs: Long,
    val category: String,
    val actionId: String? = null,
    val screenId: String? = null,
    val attributes: Map<String, String> = emptyMap(),
    val outcome: String? = null,
)

@Serializable
public data class StructuredLogEvent(
    val sequence: Long,
    val offsetMs: Long,
    val level: String,
    val code: String,
    val attributes: Map<String, String> = emptyMap(),
)

@Serializable
public data class NetworkMetadata(
    val method: String,
    val statusCode: Int,
    val durationMs: Long,
    val routeTemplate: String? = null,
    val outcome: String = "completed",
)

@Serializable
public data class Bounds(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
    val coordinateSpace: String = "window",
) {
    public val width: Int get() = right - left
    public val height: Int get() = bottom - top
}

@Serializable
public data class SemanticNode(
    val nodeId: String,
    val role: String,
    val label: String? = null,
    val testTag: String? = null,
    val bounds: Bounds,
    val state: Map<String, String> = emptyMap(),
    val parentId: String? = null,
    val sensitive: Boolean = false,
)

@Serializable
public data class SemanticsSnapshot(
    val nodes: List<SemanticNode>,
    val truncated: Boolean = false,
    val omission: OmissionReason? = null,
)
