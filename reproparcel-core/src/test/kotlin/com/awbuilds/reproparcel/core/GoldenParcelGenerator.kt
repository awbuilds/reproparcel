// SPDX-License-Identifier: AGPL-3.0-only
package com.awbuilds.reproparcel.core

import com.awbuilds.reproparcel.core.model.AppInfo
import com.awbuilds.reproparcel.core.model.Bounds
import com.awbuilds.reproparcel.core.model.CaptureInfo
import com.awbuilds.reproparcel.core.model.DeviceInfo
import com.awbuilds.reproparcel.core.model.Manifest
import com.awbuilds.reproparcel.core.model.ParcelLimits
import com.awbuilds.reproparcel.core.model.PrivacyInfo
import com.awbuilds.reproparcel.core.model.ProducerInfo
import com.awbuilds.reproparcel.core.model.Report
import com.awbuilds.reproparcel.core.model.ReportEvent
import com.awbuilds.reproparcel.core.model.SemanticNode
import com.awbuilds.reproparcel.core.model.SemanticsSnapshot
import com.awbuilds.reproparcel.core.parcel.ParcelPayload
import com.awbuilds.reproparcel.core.parcel.ParcelWriter
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO

fun main(args: Array<String>) {
    require(args.isNotEmpty()) { "output path required" }
    val image = ByteArrayOutputStream().also { output ->
        val buffered = BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB)
        for (x in 0 until 10) for (y in 0 until 10) buffered.setRGB(x, y, 0xFFFFFFFF.toInt())
        ImageIO.write(buffered, "png", output)
    }.toByteArray()
    val manifest = Manifest(
        reportId = "rpt_golden_0001",
        createdAt = "2026-11-11T00:00:00Z",
        producer = ProducerInfo("0.0.1-SNAPSHOT", "android", "golden-config"),
        app = AppInfo("com.example.synthetic", "1.0.0", 1, "debug"),
        device = DeviceInfo(30, "Synthetic", "Fixture", 100, 100, 420, "en-US", 1f, "portrait"),
        privacy = PrivacyInfo("golden-policy", "opaque", setOf("description", "image")),
        capture = CaptureInfo("2026-11-11T00:00:01Z", setOf("manual")),
        entries = emptyList(),
        limits = ParcelLimits(),
    )
    val payload = ParcelPayload(
        report = Report(
            title = "Synthetic golden report",
            description = "Synthetic golden description",
            expectedBehavior = "Expected",
            actualBehavior = "Actual",
        ),
        events = listOf(
            ReportEvent(0, 0, "tap", actionId = "save", screenId = "golden_screen", attributes = mapOf("result" to "no_effect")),
        ),
        semantics = SemanticsSnapshot(
            nodes = listOf(
                SemanticNode("n1", "button", label = "Save", bounds = Bounds(0, 0, 10, 10)),
            ),
        ),
        imagePng = image,
    )
    val bytes = ParcelWriter.writeToBytes(manifest, payload) ?: error("writer failed")
    File(args[0]).apply { parentFile.mkdirs() }.writeBytes(bytes)
    println("wrote ${args[0]} (${bytes.size} bytes)")
}
