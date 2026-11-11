// SPDX-License-Identifier: AGPL-3.0-only
package com.awbuilds.reproparcel.core

import com.awbuilds.reproparcel.core.config.Checksums
import com.awbuilds.reproparcel.core.model.Manifest
import com.awbuilds.reproparcel.core.model.Report
import com.awbuilds.reproparcel.core.parcel.ManifestValidator
import com.awbuilds.reproparcel.core.parcel.ParcelPayload
import com.awbuilds.reproparcel.core.parcel.ParcelWriter
import com.awbuilds.reproparcel.core.serialization.ReproParcelJson
import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ParcelWriterTest {
    private val root: Path = Path.of(System.getProperty("reproparcel.root"))

    @Test
    fun writesManifestReportEventsAndChecksums() {
        val manifest = loadManifest()
        val payload = ParcelPayload(
            report = Report(
                title = "Synthetic",
                description = "Synthetic description",
                expectedBehavior = "Expected",
                actualBehavior = "Actual",
            ),
        )
        val bytes = ParcelWriter.writeToBytes(manifest, payload)
        assertNotNull(bytes)
        val entries = unzip(bytes!!)
        assertTrue("manifest.json" in entries)
        assertTrue("report.json" in entries)

        val writtenManifest = ReproParcelJson.decodeManifest(
            entries["manifest.json"]!!.toString(Charsets.UTF_8),
        )
        assertNotNull(writtenManifest)
        val reportEntry = writtenManifest!!.entries.single { it.path == "report.json" }
        assertEquals(
            Checksums.sha256(entries["report.json"]!!),
            reportEntry.sha256,
        )
        assertTrue(ManifestValidator.validate(writtenManifest) is com.awbuilds.reproparcel.core.parcel.ValidationResult.Valid)
    }

    @Test
    fun writesEventsAndSemanticsEntries() {
        val payload = ParcelPayload(
            report = Report(description = "Synthetic"),
            events = listOf(
                com.awbuilds.reproparcel.core.model.ReportEvent(
                    sequence = 0,
                    offsetMs = 0,
                    category = "tap",
                    actionId = "save",
                    attributes = mapOf("result" to "no_effect"),
                ),
            ),
            semantics = com.awbuilds.reproparcel.core.model.SemanticsSnapshot(
                nodes = listOf(
                    com.awbuilds.reproparcel.core.model.SemanticNode(
                        nodeId = "n1",
                        role = "button",
                        label = "Save",
                        bounds = com.awbuilds.reproparcel.core.model.Bounds(0, 0, 10, 10),
                    ),
                ),
            ),
        )
        val entries = unzip(ParcelWriter.writeToBytes(loadManifest(), payload)!!)
        assertTrue("events.json" in entries)
        assertTrue("semantics.json" in entries)
        assertTrue(entries["events.json"]!!.toString(Charsets.UTF_8).contains("no_effect"))
        assertTrue(entries["semantics.json"]!!.toString(Charsets.UTF_8).contains("Save"))
    }

    @Test
    fun rejectsBlankDescription() {
        val payload = ParcelPayload(report = Report(description = ""))
        assertEquals(null, ParcelWriter.writeToBytes(loadManifest(), payload))
    }

    @Test
    fun writesOptionalImageAsPngEntry() {
        val image = java.io.ByteArrayOutputStream().also { output ->
            val buffered = java.awt.image.BufferedImage(10, 10, java.awt.image.BufferedImage.TYPE_INT_ARGB)
            javax.imageio.ImageIO.write(buffered, "png", output)
        }.toByteArray()
        val payload = ParcelPayload(
            report = Report(description = "Synthetic"),
            imagePng = image,
        )
        val entries = unzip(ParcelWriter.writeToBytes(loadManifest(), payload)!!)
        assertTrue("attachments/screen.png" in entries)
    }

    @Test
    fun aggregateUncompressedBudgetIsRejected() {
        val payload = ParcelPayload(
            report = Report(description = "Synthetic"),
            imagePng = ByteArray(10 * 1024 * 1024) { 1 },
        )
        assertEquals(null, ParcelWriter.writeToBytes(loadManifest(), payload))
    }

    @Test
    fun oversizedEntryIsRejected() {
        val payload = ParcelPayload(
            report = Report(description = "x".repeat(2_100_000)),
        )
        assertEquals(null, ParcelWriter.writeToBytes(loadManifest(), payload))
    }

    private fun loadManifest(): Manifest {
        val text = Files.readString(root.resolve("schema/fixtures/v1/valid/manifest.json"))
        return requireNotNull(ReproParcelJson.decodeManifest(text))
    }

    private fun unzip(bytes: ByteArray): Map<String, ByteArray> {
        val result = linkedMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                result[entry.name] = zip.readBytes()
            }
        }
        return result
    }
}
