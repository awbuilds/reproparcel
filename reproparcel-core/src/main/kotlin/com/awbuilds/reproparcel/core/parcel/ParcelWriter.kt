// SPDX-License-Identifier: AGPL-3.0-only
package com.awbuilds.reproparcel.core.parcel

import com.awbuilds.reproparcel.core.config.Checksums
import com.awbuilds.reproparcel.core.limits.Limits
import com.awbuilds.reproparcel.core.model.EntryInfo
import com.awbuilds.reproparcel.core.model.Manifest
import com.awbuilds.reproparcel.core.model.Report
import com.awbuilds.reproparcel.core.model.ReportEvent
import com.awbuilds.reproparcel.core.model.SemanticsSnapshot
import com.awbuilds.reproparcel.core.result.ErrorCode
import com.awbuilds.reproparcel.core.result.SdkResult
import com.awbuilds.reproparcel.core.serialization.JsonBudget
import com.awbuilds.reproparcel.core.serialization.ReproParcelJson
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

public data class ParcelPayload(
    public val report: Report,
    public val events: List<ReportEvent> = emptyList(),
    public val semantics: SemanticsSnapshot? = null,
    public val imagePng: ByteArray? = null,
)

public object ParcelWriter {
    public fun writeToBytes(manifest: Manifest, payload: ParcelPayload): ByteArray? {
        val output = ByteArrayOutputStream()
        val result = write(manifest, payload, output)
        return if (result == SdkResult.Success) output.toByteArray() else null
    }

    public fun write(
        manifest: Manifest,
        payload: ParcelPayload,
        destination: OutputStream,
    ): SdkResult {
        val entries = linkedMapOf<String, ByteArray>()
        val reportBytes = encode(Report.serializer(), payload.report) ?: return failure()
        entries["report.json"] = reportBytes

        if (payload.events.isNotEmpty()) {
            val eventsBytes = encode(
                kotlinx.serialization.builtins.ListSerializer(ReportEvent.serializer()),
                payload.events,
            ) ?: return failure()
            entries["events.json"] = eventsBytes
        }
        payload.semantics?.let { semantics ->
            val semanticsBytes = encode(SemanticsSnapshot.serializer(), semantics)
                ?: return failure()
            entries["semantics.json"] = semanticsBytes
        }
        payload.imagePng?.let { image ->
            if (image.size > Limits.MAX_PARCEL_UNCOMPRESSED_BYTES) return failure()
            entries["attachments/screen.png"] = image
        }

        if (entries.size + 1 > Limits.MAX_PARCEL_ENTRIES) {
            return SdkResult.Failure(ErrorCode.LIMIT_EXCEEDED)
        }
        val entryMetadata = entries.map { (path, bytes) ->
            val mediaType = if (path.endsWith(".png")) "image/png" else "application/json"
            ManifestValidator.entryFor(path, mediaType, bytes)
        }
        val finalManifest = manifest.copy(entries = entryMetadata)
        if (ManifestValidator.validate(finalManifest) !is ValidationResult.Valid) {
            return SdkResult.Failure(ErrorCode.VALIDATION_FAILED)
        }
        val manifestBytes = encode(Manifest.serializer(), finalManifest) ?: return failure()
        val uncompressedTotal = manifestBytes.size.toLong() + entries.values.sumOf { it.size.toLong() }
        if (uncompressedTotal > Limits.MAX_PARCEL_UNCOMPRESSED_BYTES) {
            return SdkResult.Failure(ErrorCode.LIMIT_EXCEEDED)
        }

        return try {
            val buffer = ByteArrayOutputStream()
            ZipOutputStream(buffer).use { zip ->
                writeEntry(zip, "manifest.json", manifestBytes)
                entries.forEach { (path, bytes) -> writeEntry(zip, path, bytes) }
            }
            val compressed = buffer.toByteArray()
            if (compressed.size > Limits.MAX_PARCEL_COMPRESSED_BYTES) {
                return SdkResult.Failure(ErrorCode.LIMIT_EXCEEDED)
            }
            destination.write(compressed)
            SdkResult.Success
        } catch (_: java.io.IOException) {
            SdkResult.Failure(ErrorCode.EXPORT_FAILED)
        } catch (_: SecurityException) {
            SdkResult.Failure(ErrorCode.EXPORT_FAILED)
        }
    }

    private fun writeEntry(zip: ZipOutputStream, path: String, bytes: ByteArray) {
        val entry = ZipEntry(path).apply { time = 0L }
        zip.putNextEntry(entry)
        zip.write(bytes)
        zip.closeEntry()
    }

    private fun <T> encode(
        serializer: kotlinx.serialization.KSerializer<T>,
        value: T,
    ): ByteArray? {
        return try {
            val text = ReproParcelJson.json.encodeToString(serializer, value)
            if (!JsonBudget.inspect(text).withinBudget) null else text.toByteArray(Charsets.UTF_8)
        } catch (_: kotlinx.serialization.SerializationException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private fun failure(): SdkResult = SdkResult.Failure(ErrorCode.SERIALIZATION_FAILED)

    public fun sha256(bytes: ByteArray): String = Checksums.sha256(bytes)
}
