// SPDX-License-Identifier: AGPL-3.0-only
package com.awbuilds.reproparcel.android.draft

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.awbuilds.reproparcel.core.config.RetentionPolicy
import com.awbuilds.reproparcel.core.model.Report
import com.awbuilds.reproparcel.core.result.ErrorCode
import com.awbuilds.reproparcel.core.serialization.ReproParcelJson
import com.awbuilds.reproparcel.core.time.Clock
import com.awbuilds.reproparcel.core.time.SystemClock
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import org.json.JSONArray
import org.json.JSONObject

public sealed interface DraftSaveResult {
    public data class Saved(public val id: String, public val bytes: Long) : DraftSaveResult
    public data class Failure(public val code: ErrorCode) : DraftSaveResult
}

public sealed interface DraftLoadResult {
    public data class Loaded(public val report: Report, public val imagePng: ByteArray?) : DraftLoadResult
    public data class Failure(public val code: ErrorCode) : DraftLoadResult
    public data object NotFound : DraftLoadResult
}

public data class DraftSummary(
    public val id: String,
    public val createdAtMs: Long,
    public val bytes: Long,
)

/**
 * Small file-based encrypted draft store.
 *
 * File layout:
 *   files/reproparcel/drafts/index.json       (id, createdAt, size only)
 *   files/reproparcel/drafts/<id>/report.enc  (AES-GCM)
 *   files/reproparcel/drafts/<id>/image.enc   (AES-GCM, optional)
 *
 * The key is Android Keystore-backed. There is no plaintext fallback: a key or
 * crypto failure returns a typed failure and the caller may keep an in-memory
 * draft for the current session only.
 */
public class DraftStore(
    context: Context,
    private val retention: RetentionPolicy = RetentionPolicy(),
    private val clock: Clock = SystemClock,
) {
    private val root: File = File(context.applicationContext.filesDir, "reproparcel/drafts")
    private val indexFile: File = File(root, "index.json")
    private val aad = "reproparcel-draft-v1".toByteArray(Charsets.UTF_8)

    public fun save(report: Report, imagePng: ByteArray? = null): DraftSaveResult {
        return try {
            pruneExpired()
            val reportJson = ReproParcelJson.json.encodeToString(Report.serializer(), report)
            val reportBytes = encrypt(reportJson.toByteArray(Charsets.UTF_8))
            val imageBytes = imagePng?.let(::encrypt)
            val total = reportBytes.size.toLong() + (imageBytes?.size?.toLong() ?: 0L)
            if (total > retention.maxBytes) return DraftSaveResult.Failure(ErrorCode.LIMIT_EXCEEDED)

            val index = readIndex().toMutableList()
            while (index.size >= retention.maxReports ||
                index.sumOf { it.bytes } + total > retention.maxBytes
            ) {
                val oldest = index.minByOrNull { it.createdAtMs } ?: break
                index.remove(oldest)
                deleteFiles(oldest.id)
            }
            if (index.size >= retention.maxReports ||
                index.sumOf { it.bytes } + total > retention.maxBytes
            ) {
                return DraftSaveResult.Failure(ErrorCode.LIMIT_EXCEEDED)
            }

            val id = UUID.randomUUID().toString()
            val directory = File(root, id)
            if (!directory.mkdirs() && !directory.isDirectory) {
                return DraftSaveResult.Failure(ErrorCode.STORAGE_FAILED)
            }
            if (!writeAtomically(File(directory, "report.enc"), reportBytes)) {
                directory.deleteRecursively()
                return DraftSaveResult.Failure(ErrorCode.STORAGE_FAILED)
            }
            if (imageBytes != null && !writeAtomically(File(directory, "image.enc"), imageBytes)) {
                directory.deleteRecursively()
                return DraftSaveResult.Failure(ErrorCode.STORAGE_FAILED)
            }
            index += DraftSummary(id, clock.nowMs(), total)
            if (!writeIndex(index)) {
                directory.deleteRecursively()
                return DraftSaveResult.Failure(ErrorCode.STORAGE_FAILED)
            }
            DraftSaveResult.Saved(id, total)
        } catch (_: GeneralSecurityException) {
            DraftSaveResult.Failure(ErrorCode.ENCRYPTION_UNAVAILABLE)
        } catch (_: java.io.IOException) {
            DraftSaveResult.Failure(ErrorCode.STORAGE_FAILED)
        }
    }

    public fun load(id: String): DraftLoadResult {
        if (!id.matches(Regex("^[0-9a-fA-F-]{36}$"))) return DraftLoadResult.NotFound
        val directory = File(root, id)
        if (!directory.isDirectory) return DraftLoadResult.NotFound
        return try {
            val reportBytes = decrypt(File(directory, "report.enc").readBytes())
            val report = ReproParcelJson.json.decodeFromString(Report.serializer(), reportBytes.toString(Charsets.UTF_8))
            val image = File(directory, "image.enc").takeIf(File::isFile)?.let { decrypt(it.readBytes()) }
            DraftLoadResult.Loaded(report, image)
        } catch (_: GeneralSecurityException) {
            DraftLoadResult.Failure(ErrorCode.ENCRYPTION_UNAVAILABLE)
        } catch (_: java.io.IOException) {
            DraftLoadResult.Failure(ErrorCode.STORAGE_FAILED)
        } catch (_: kotlinx.serialization.SerializationException) {
            DraftLoadResult.Failure(ErrorCode.VALIDATION_FAILED)
        }
    }

    public fun list(): List<DraftSummary> {
        pruneExpired()
        return readIndex()
    }

    public fun delete(id: String): Boolean {
        if (!id.matches(Regex("^[0-9a-fA-F-]{36}$"))) return false
        val index = readIndex().toMutableList()
        val removed = index.removeAll { it.id == id }
        deleteFiles(id)
        writeIndex(index)
        return removed
    }

    public fun deleteAll(): Int {
        val count = readIndex().size
        root.deleteRecursively()
        root.mkdirs()
        writeIndex(emptyList())
        return count
    }

    public fun deleteKeyForTest() {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        if (keyStore.containsAlias(KEY_ALIAS)) keyStore.deleteEntry(KEY_ALIAS)
    }

    private fun encrypt(plain: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        cipher.updateAAD(aad)
        val ciphertext = cipher.doFinal(plain)
        val iv = cipher.iv
        val output = java.io.ByteArrayOutputStream()
        DataOutputStream(output).use { data ->
            data.write(MAGIC)
            data.writeByte(FORMAT_VERSION)
            data.writeInt(iv.size)
            data.write(iv)
            data.write(ciphertext)
        }
        return output.toByteArray()
    }

    private fun decrypt(payload: ByteArray): ByteArray {
        DataInputStream(payload.inputStream()).use { data ->
            val magic = ByteArray(MAGIC.size)
            data.readFully(magic)
            if (!magic.contentEquals(MAGIC)) throw GeneralSecurityException("bad draft magic")
            if (data.readByte().toInt() != FORMAT_VERSION) throw GeneralSecurityException("bad draft version")
            val ivLength = data.readInt()
            if (ivLength !in 12..32) throw GeneralSecurityException("bad draft IV")
            val iv = ByteArray(ivLength)
            data.readFully(iv)
            val ciphertext = data.readBytes()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
            cipher.updateAAD(aad)
            return cipher.doFinal(ciphertext)
        }
    }

    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build(),
        )
        return generator.generateKey()
    }

    private fun pruneExpired() {
        val cutoff = clock.nowMs() - retention.ttlHours * 60L * 60L * 1000L
        val index = readIndex()
        val kept = index.filter { summary ->
            val keep = summary.createdAtMs >= cutoff
            if (!keep) deleteFiles(summary.id)
            keep
        }
        if (kept.size != index.size) writeIndex(kept)
    }

    private fun readIndex(): List<DraftSummary> {
        if (!indexFile.isFile) return emptyList()
        return try {
            val array = JSONArray(indexFile.readText())
            buildList {
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    add(
                        DraftSummary(
                            id = item.getString("id"),
                            createdAtMs = item.getLong("createdAtMs"),
                            bytes = item.getLong("bytes"),
                        ),
                    )
                }
            }
        } catch (_: org.json.JSONException) {
            // A corrupt non-sensitive index must not expose data or crash the
            // reporter. Start a fresh index; encrypted files remain unlinked.
            emptyList()
        } catch (_: java.io.IOException) {
            emptyList()
        } catch (_: NumberFormatException) {
            emptyList()
        }
    }

    private fun writeIndex(entries: List<DraftSummary>): Boolean {
        return try {
            val array = JSONArray()
            entries.forEach { summary ->
                array.put(
                    JSONObject().apply {
                        put("id", summary.id)
                        put("createdAtMs", summary.createdAtMs)
                        put("bytes", summary.bytes)
                    },
                )
            }
            writeAtomically(indexFile, array.toString().toByteArray(Charsets.UTF_8))
        } catch (_: org.json.JSONException) {
            false
        } catch (_: java.io.IOException) {
            false
        }
    }

    private fun writeAtomically(target: File, bytes: ByteArray): Boolean {
        return try {
            target.parentFile?.mkdirs()
            val temp = File(target.parentFile, target.name + ".tmp")
            FileOutputStream(temp).use { stream ->
                stream.write(bytes)
                stream.fd.sync()
            }
            if (!temp.renameTo(target)) {
                target.delete()
                temp.renameTo(target)
            }
            target.isFile && target.length() == bytes.size.toLong()
        } catch (_: java.io.IOException) {
            false
        }
    }

    private fun deleteFiles(id: String) {
        File(root, id).deleteRecursively()
    }

    private companion object {
        const val KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "reproparcel.draft.v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val FORMAT_VERSION = 1
        val MAGIC = byteArrayOf('R'.code.toByte(), 'P'.code.toByte(), 'D'.code.toByte(), '1'.code.toByte())
    }
}
