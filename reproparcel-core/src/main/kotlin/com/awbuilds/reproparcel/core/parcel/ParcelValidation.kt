// SPDX-License-Identifier: Apache-2.0
package com.awbuilds.reproparcel.core.parcel

import com.awbuilds.reproparcel.core.config.Checksums
import com.awbuilds.reproparcel.core.limits.Limits
import com.awbuilds.reproparcel.core.model.EntryInfo
import com.awbuilds.reproparcel.core.model.EvidenceState
import com.awbuilds.reproparcel.core.model.Manifest
import com.awbuilds.reproparcel.core.model.SCHEMA_VERSION
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

public enum class ValidationCode {
    UNSUPPORTED_SCHEMA,
    MISSING_FIELD,
    INVALID_PATH,
    PATH_TRAVERSAL,
    ABSOLUTE_PATH,
    DUPLICATE_PATH,
    UNREFERENCED_ENTRY,
    UNSUPPORTED_MEDIA_TYPE,
    INVALID_CHECKSUM,
    TOO_MANY_ENTRIES,
    ENTRY_TOO_LARGE,
    INVALID_TIMESTAMP,
    INVALID_BOUNDS,
    TOO_MANY_SEMANTIC_NODES,
    SEMANTIC_DEPTH_EXCEEDED,
    INVALID_EVIDENCE_STATE,
}

public data class ValidationIssue(
    public val code: ValidationCode,
    public val path: String,
    public val safeDetail: String? = null,
)

public sealed interface ValidationResult {
    public data object Valid : ValidationResult
    public data class Invalid(public val issues: List<ValidationIssue>) : ValidationResult
}

public object Rfc3339 {
    public fun parseToEpochMs(value: String): Long? {
        return try {
            OffsetDateTime.parse(value).toInstant().toEpochMilli()
        } catch (_: DateTimeParseException) {
            try {
                Instant.parse(value).toEpochMilli()
            } catch (_: DateTimeParseException) {
                null
            }
        }
    }

    public fun isUtcInstant(value: String): Boolean {
        val parsed = parseToEpochMs(value) ?: return false
        return try {
            OffsetDateTime.parse(value).offset.totalSeconds == 0
        } catch (_: DateTimeParseException) {
            value.endsWith("Z")
        }
    }
}

public object ParcelPathPolicy {
    private val allowedName = Regex("^[A-Za-z0-9][A-Za-z0-9._/-]*$")
    private val allowedExact = setOf(
        "manifest.json",
        "report.json",
        "events.json",
        "semantics.json",
        "attachments/screen.png",
    )
    private val allowedPrefixes = listOf("attachments/")

    public fun validate(path: String): ValidationIssue? {
        if (path.length > Limits.MAX_ENTRY_PATH_LENGTH) {
            return ValidationIssue(ValidationCode.INVALID_PATH, path, "path too long")
        }
        if (path.startsWith("/")) {
            return ValidationIssue(ValidationCode.ABSOLUTE_PATH, path, "absolute path")
        }
        if (path.contains("..") || path.contains('\\') || path.contains('\u0000')) {
            return ValidationIssue(ValidationCode.PATH_TRAVERSAL, path, "traversal or separator")
        }
        if (!allowedName.matches(path)) {
            return ValidationIssue(ValidationCode.INVALID_PATH, path, "non-allowlisted characters")
        }
        if (path in allowedExact) return null
        if (allowedPrefixes.any { path.startsWith(it) } && path.substringAfterLast('.').lowercase() == "png") {
            return null
        }
        return ValidationIssue(ValidationCode.INVALID_PATH, path, "not an allowlisted entry")
    }
}

public object ManifestValidator {
    public fun validate(manifest: Manifest): ValidationResult {
        val issues = mutableListOf<ValidationIssue>()
        if (manifest.schemaVersion != SCHEMA_VERSION) {
            issues += ValidationIssue(ValidationCode.UNSUPPORTED_SCHEMA, "manifest.json")
        }
        if (manifest.reportId.isBlank() || manifest.reportId.length > 128) {
            issues += ValidationIssue(ValidationCode.MISSING_FIELD, "reportId")
        }
        if (!Rfc3339.isUtcInstant(manifest.createdAt)) {
            issues += ValidationIssue(ValidationCode.INVALID_TIMESTAMP, "createdAt")
        }
        if (manifest.entries.size > Limits.MAX_PARCEL_ENTRIES) {
            issues += ValidationIssue(ValidationCode.TOO_MANY_ENTRIES, "entries")
        }
        val seen = mutableSetOf<String>()
        for (entry in manifest.entries) {
            ParcelPathPolicy.validate(entry.path)?.let { issues += it }
            if (!seen.add(entry.path)) {
                issues += ValidationIssue(ValidationCode.DUPLICATE_PATH, entry.path)
            }
            if (entry.path == "manifest.json") {
                issues += ValidationIssue(ValidationCode.UNREFERENCED_ENTRY, entry.path, "manifest cannot reference itself")
            }
            if (entry.uncompressedSize < 0 || entry.uncompressedSize > Limits.MAX_PARCEL_UNCOMPRESSED_BYTES) {
                issues += ValidationIssue(ValidationCode.ENTRY_TOO_LARGE, entry.path)
            }
            if (!entry.mediaType.matches(Regex("^(application/json|image/png)$"))) {
                issues += ValidationIssue(ValidationCode.UNSUPPORTED_MEDIA_TYPE, entry.path)
            }
            if (!Checksums.isSha256Hex(entry.sha256)) {
                issues += ValidationIssue(ValidationCode.INVALID_CHECKSUM, entry.path)
            }
        }
        if (manifest.entries.none { it.path == "report.json" }) {
            issues += ValidationIssue(ValidationCode.MISSING_FIELD, "report.json")
        }
        if (manifest.entries.sumOf { it.uncompressedSize } > Limits.MAX_PARCEL_UNCOMPRESSED_BYTES) {
            issues += ValidationIssue(ValidationCode.ENTRY_TOO_LARGE, "entries", "aggregate uncompressed budget exceeded")
        }
        manifest.capture.providers.forEach { provider ->
            if (provider.status.state == EvidenceState.OMITTED && provider.status.reason == null) {
                issues += ValidationIssue(ValidationCode.INVALID_EVIDENCE_STATE, "capture.providers")
            }
        }
        return if (issues.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(issues)
    }

    public fun validateSemantics(
        nodeCount: Int,
        maxDepth: Int,
        bytes: Long,
    ): ValidationResult {
        val issues = mutableListOf<ValidationIssue>()
        if (nodeCount > Limits.MAX_SEMANTIC_NODES) {
            issues += ValidationIssue(ValidationCode.TOO_MANY_SEMANTIC_NODES, "semantics.json")
        }
        if (maxDepth > Limits.MAX_SEMANTIC_DEPTH) {
            issues += ValidationIssue(ValidationCode.SEMANTIC_DEPTH_EXCEEDED, "semantics.json")
        }
        if (bytes > Limits.MAX_SEMANTIC_BYTES) {
            issues += ValidationIssue(ValidationCode.ENTRY_TOO_LARGE, "semantics.json")
        }
        return if (issues.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(issues)
    }

    public fun entryFor(path: String, mediaType: String, bytes: ByteArray): EntryInfo {
        return EntryInfo(
            path = path,
            mediaType = mediaType,
            uncompressedSize = bytes.size.toLong(),
            sha256 = Checksums.sha256(bytes),
        )
    }
}
