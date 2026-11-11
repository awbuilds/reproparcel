// SPDX-License-Identifier: Apache-2.0
package com.awbuilds.reproparcel.android.export

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.awbuilds.reproparcel.core.model.Manifest
import com.awbuilds.reproparcel.core.parcel.ParcelPayload
import com.awbuilds.reproparcel.core.parcel.ParcelWriter
import com.awbuilds.reproparcel.core.result.ErrorCode
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

public sealed interface ExportOutcome {
    public data object FileSaved : ExportOutcome
    public data object ShareLaunched : ExportOutcome
    public data object Cancelled : ExportOutcome
    public data class Failed(public val code: ErrorCode) : ExportOutcome
}

public class ExportManager(private val context: Context) {
    private val exportsDirectory: File = File(context.cacheDir, "reproparcel/exports")

    public fun buildParcel(manifest: Manifest, payload: ParcelPayload): ByteArray? =
        ParcelWriter.writeToBytes(manifest, payload)

    public suspend fun saveToUri(uri: Uri, manifest: Manifest, payload: ParcelPayload): ExportOutcome =
        withContext(Dispatchers.IO) {
            val bytes = buildParcel(manifest, payload) ?: return@withContext ExportOutcome.Failed(ErrorCode.SERIALIZATION_FAILED)
            try {
                context.contentResolver.openOutputStream(uri, "w").use { stream ->
                    if (stream == null) return@withContext ExportOutcome.Failed(ErrorCode.EXPORT_FAILED)
                    stream.write(bytes)
                    stream.flush()
                }
                ExportOutcome.FileSaved
            } catch (_: IOException) {
                ExportOutcome.Failed(ErrorCode.EXPORT_FAILED)
            } catch (_: SecurityException) {
                ExportOutcome.Failed(ErrorCode.EXPORT_FAILED)
            }
        }

    public suspend fun share(
        activity: Activity,
        manifest: Manifest,
        payload: ParcelPayload,
        fileName: String = "report.reproparcel",
    ): ExportOutcome {
        val bytes = withContext(Dispatchers.Default) {
            buildParcel(manifest, payload)
        } ?: return ExportOutcome.Failed(ErrorCode.SERIALIZATION_FAILED)
        val safeName = fileName.filter { it.isLetterOrDigit() || it == '.' || it == '-' || it == '_' }
            .ifEmpty { "report.reproparcel" }
        val file = withContext(Dispatchers.IO) {
            try {
                exportsDirectory.mkdirs()
                cleanupOldExports()
                val target = File(exportsDirectory, safeName)
                target.writeBytes(bytes)
                target
            } catch (_: IOException) {
                null
            }
        } ?: return ExportOutcome.Failed(ErrorCode.EXPORT_FAILED)
        val uri = try {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.reproparcel.fileprovider",
                file,
            )
        } catch (_: IllegalArgumentException) {
            return ExportOutcome.Failed(ErrorCode.EXPORT_FAILED)
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return withContext(Dispatchers.Main) {
            try {
                activity.startActivity(Intent.createChooser(intent, "Share report"))
                ExportOutcome.ShareLaunched
            } catch (_: ActivityNotFoundException) {
                ExportOutcome.Failed(ErrorCode.EXPORT_FAILED)
            } catch (_: SecurityException) {
                ExportOutcome.Failed(ErrorCode.EXPORT_FAILED)
            }
        }
    }

    public fun clearExports(): Int {
        val files = exportsDirectory.listFiles()?.size ?: 0
        exportsDirectory.deleteRecursively()
        return if (exportsDirectory.exists()) -1 else files
    }

    private fun cleanupOldExports() {
        val cutoff = System.currentTimeMillis() - 24L * 60L * 60L * 1000L
        exportsDirectory.listFiles()?.forEach { file ->
            if (file.lastModified() < cutoff) file.delete()
        }
    }
}
