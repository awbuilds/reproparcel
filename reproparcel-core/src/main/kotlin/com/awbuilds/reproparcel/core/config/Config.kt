// SPDX-License-Identifier: AGPL-3.0-only
package com.awbuilds.reproparcel.core.config

import com.awbuilds.reproparcel.core.model.BuildInfo
import kotlinx.serialization.Serializable
import java.security.MessageDigest

@Serializable
public data class RetentionPolicy(
    val maxReports: Int = 10,
    val maxBytes: Long = 25L * 1024 * 1024,
    val ttlHours: Int = 72,
)

@Serializable
public data class MaskRegion(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
)

@Serializable
public data class CapturePolicy(
    val policyVersion: String,
    val screenshotEnabled: Boolean = false,
    val semanticTextEnabled: Boolean = false,
    val eligibleSurfaces: Set<String> = emptySet(),
    val sensitiveSurfaces: Set<String> = emptySet(),
    val excludedSurfaces: Set<String> = emptySet(),
    val maskRegions: List<MaskRegion> = emptyList(),
)

@Serializable
public data class EvidenceConfig(
    val screenshotEnabled: Boolean = false,
    val semanticTextEnabled: Boolean = false,
    val structuredLogsEnabled: Boolean = false,
    val networkMetadataEnabled: Boolean = false,
)

@Serializable
public data class ReproParcelConfig(
    val enabled: Boolean = false,
    val buildInfo: BuildInfo,
    val capturePolicy: CapturePolicy? = null,
    val retention: RetentionPolicy = RetentionPolicy(),
    val evidence: EvidenceConfig = EvidenceConfig(),
)

public enum class ConfigErrorCode {
    MISSING_PACKAGE,
    INVALID_PACKAGE,
    INVALID_VERSION_NAME,
    INVALID_VERSION_CODE,
    INVALID_VARIANT,
    INVALID_GIT_COMMIT,
    INVALID_RETENTION,
    INVALID_CAPTURE_POLICY,
    SCREENSHOT_WITHOUT_POLICY,
    SEMANTICS_WITHOUT_POLICY,
    SURFACE_OVERLAP,
}

public data class ConfigError(
    public val code: ConfigErrorCode,
    public val field: String,
)

public sealed interface ConfigValidation {
    public data object Valid : ConfigValidation
    public data class Invalid(public val errors: List<ConfigError>) : ConfigValidation
}

public object ConfigValidator {
    private val packageRegex = Regex("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+$")
    private val commitRegex = Regex("^[0-9a-f]{7,64}$")
    private val surfaceRegex = Regex("^[A-Za-z0-9_.:/-]{1,128}$")

    public fun validate(config: ReproParcelConfig): ConfigValidation {
        val errors = mutableListOf<ConfigError>()
        val build = config.buildInfo

        if (build.packageName.isBlank()) {
            errors += ConfigError(ConfigErrorCode.MISSING_PACKAGE, "buildInfo.packageName")
        } else if (!packageRegex.matches(build.packageName)) {
            errors += ConfigError(ConfigErrorCode.INVALID_PACKAGE, "buildInfo.packageName")
        }
        if (build.versionName.isBlank() || build.versionName.length > 64) {
            errors += ConfigError(ConfigErrorCode.INVALID_VERSION_NAME, "buildInfo.versionName")
        }
        if (build.versionCode <= 0) {
            errors += ConfigError(ConfigErrorCode.INVALID_VERSION_CODE, "buildInfo.versionCode")
        }
        if (build.variant.isBlank() || build.variant.length > 32) {
            errors += ConfigError(ConfigErrorCode.INVALID_VARIANT, "buildInfo.variant")
        }
        build.gitCommit?.let {
            if (!commitRegex.matches(it)) {
                errors += ConfigError(ConfigErrorCode.INVALID_GIT_COMMIT, "buildInfo.gitCommit")
            }
        }

        val retention = config.retention
        if (retention.maxReports !in 1..100 ||
            retention.maxBytes !in 1..(100L * 1024 * 1024) ||
            retention.ttlHours !in 1..720
        ) {
            errors += ConfigError(ConfigErrorCode.INVALID_RETENTION, "retention")
        }

        val policy = config.capturePolicy
        if (config.evidence.screenshotEnabled && policy == null) {
            errors += ConfigError(ConfigErrorCode.SCREENSHOT_WITHOUT_POLICY, "capturePolicy")
        }
        if (config.evidence.semanticTextEnabled && policy == null) {
            errors += ConfigError(ConfigErrorCode.SEMANTICS_WITHOUT_POLICY, "capturePolicy")
        }
        if (policy != null) {
            if (policy.policyVersion.isBlank() || policy.policyVersion.length > 64) {
                errors += ConfigError(ConfigErrorCode.INVALID_CAPTURE_POLICY, "capturePolicy.policyVersion")
            }
            if (policy.screenshotEnabled && policy.eligibleSurfaces.isEmpty()) {
                errors += ConfigError(ConfigErrorCode.INVALID_CAPTURE_POLICY, "capturePolicy.eligibleSurfaces")
            }
            val allSurfaces = policy.eligibleSurfaces + policy.sensitiveSurfaces + policy.excludedSurfaces
            if (allSurfaces.any { !surfaceRegex.matches(it) }) {
                errors += ConfigError(ConfigErrorCode.INVALID_CAPTURE_POLICY, "capturePolicy.surfaces")
            }
            if (policy.sensitiveSurfaces.any { it in policy.eligibleSurfaces }) {
                errors += ConfigError(ConfigErrorCode.SURFACE_OVERLAP, "capturePolicy.sensitiveSurfaces")
            }
            if (policy.excludedSurfaces.any { it in policy.eligibleSurfaces }) {
                errors += ConfigError(ConfigErrorCode.SURFACE_OVERLAP, "capturePolicy.excludedSurfaces")
            }
            if (policy.maskRegions.size > 32 || policy.maskRegions.any {
                    it.left < 0 || it.top < 0 || it.right <= it.left || it.bottom <= it.top
                }
            ) {
                errors += ConfigError(ConfigErrorCode.INVALID_CAPTURE_POLICY, "capturePolicy.maskRegions")
            }
        }

        return if (errors.isEmpty()) ConfigValidation.Valid else ConfigValidation.Invalid(errors)
    }

    public fun collectionConfigurationId(config: ReproParcelConfig): String {
        val canonical = buildString {
            append(config.evidence.screenshotEnabled)
            append('|')
            append(config.evidence.semanticTextEnabled)
            append('|')
            append(config.evidence.structuredLogsEnabled)
            append('|')
            append(config.evidence.networkMetadataEnabled)
            append('|')
            append(config.capturePolicy?.policyVersion.orEmpty())
        }
        return Checksums.sha256(canonical.toByteArray(Charsets.UTF_8)).take(16)
    }
}

public object Checksums {
    public fun sha256(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    public fun isSha256Hex(value: String): Boolean =
        value.length == 64 && value.all { it in '0'..'9' || it in 'a'..'f' }
}
