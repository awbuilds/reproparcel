// SPDX-License-Identifier: AGPL-3.0-only
package com.awbuilds.reproparcel.core.serialization

import com.awbuilds.reproparcel.core.limits.Limits
import com.awbuilds.reproparcel.core.model.Manifest
import com.awbuilds.reproparcel.core.model.Report
import com.awbuilds.reproparcel.core.model.ReportEvent
import com.awbuilds.reproparcel.core.model.SCHEMA_VERSION
import com.awbuilds.reproparcel.core.model.SemanticsSnapshot
import com.awbuilds.reproparcel.core.result.ErrorCode
import com.awbuilds.reproparcel.core.result.SdkResult
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

public data class JsonBudgetResult(
    public val bytes: Long,
    public val depth: Int,
    public val nodes: Int,
    public val withinBudget: Boolean,
)

public object JsonBudget {
    public fun inspect(text: String): JsonBudgetResult {
        val bytes = text.toByteArray(Charsets.UTF_8).size.toLong()
        var depth = 0
        var maxDepth = 0
        var nodes = 0
        var inString = false
        var escaped = false
        for (character in text) {
            if (inString) {
                if (escaped) {
                    escaped = false
                } else if (character == '\\') {
                    escaped = true
                } else if (character == '"') {
                    inString = false
                }
                continue
            }
            when (character) {
                '"' -> inString = true
                '{', '[' -> {
                    depth += 1
                    nodes += 1
                    if (depth > maxDepth) maxDepth = depth
                }
                '}', ']' -> if (depth > 0) depth -= 1
            }
        }
        return JsonBudgetResult(
            bytes = bytes,
            depth = maxDepth,
            nodes = nodes,
            withinBudget = bytes <= Limits.MAX_JSON_BYTES &&
                maxDepth <= Limits.MAX_JSON_DEPTH &&
                nodes <= Limits.MAX_JSON_NODES,
        )
    }
}

public object ReproParcelJson {
    public val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
        isLenient = false
    }

    public fun encodeManifest(manifest: Manifest): SdkResult = encode(Manifest.serializer(), manifest)

    public fun decodeManifest(text: String): Manifest? {
        val budget = JsonBudget.inspect(text)
        if (!budget.withinBudget) return null
        return try {
            val decoded = json.decodeFromString(Manifest.serializer(), text)
            if (decoded.schemaVersion != SCHEMA_VERSION) null else decoded
        } catch (_: SerializationException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    public fun encodeReport(report: Report): SdkResult = encode(Report.serializer(), report)

    public fun decodeReport(text: String): Report? = decode(Report.serializer(), text)

    public fun encodeEvents(events: List<ReportEvent>): SdkResult =
        encode(kotlinx.serialization.builtins.ListSerializer(ReportEvent.serializer()), events)

    public fun decodeEvents(text: String): List<ReportEvent>? =
        decode(kotlinx.serialization.builtins.ListSerializer(ReportEvent.serializer()), text)

    public fun encodeSemantics(snapshot: SemanticsSnapshot): SdkResult =
        encode(SemanticsSnapshot.serializer(), snapshot)

    public fun decodeSemantics(text: String): SemanticsSnapshot? =
        decode(SemanticsSnapshot.serializer(), text)

    private fun <T> encode(
        serializer: kotlinx.serialization.KSerializer<T>,
        value: T,
    ): SdkResult {
        return try {
            val text = json.encodeToString(serializer, value)
            if (JsonBudget.inspect(text).withinBudget) {
                SdkResult.Success
            } else {
                SdkResult.Failure(ErrorCode.LIMIT_EXCEEDED)
            }
        } catch (_: SerializationException) {
            SdkResult.Failure(ErrorCode.SERIALIZATION_FAILED)
        } catch (_: IllegalArgumentException) {
            SdkResult.Failure(ErrorCode.SERIALIZATION_FAILED)
        }
    }

    private fun <T> decode(
        serializer: kotlinx.serialization.KSerializer<T>,
        text: String,
    ): T? {
        if (!JsonBudget.inspect(text).withinBudget) return null
        return try {
            json.decodeFromString(serializer, text)
        } catch (_: SerializationException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
    }
}
