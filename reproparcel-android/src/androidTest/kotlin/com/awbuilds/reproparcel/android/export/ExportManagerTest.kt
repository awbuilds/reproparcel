// SPDX-License-Identifier: AGPL-3.0-only
package com.awbuilds.reproparcel.android.export

import android.content.Context
import androidx.core.content.FileProvider
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.awbuilds.reproparcel.core.model.AppInfo
import com.awbuilds.reproparcel.core.model.CaptureInfo
import com.awbuilds.reproparcel.core.model.DeviceInfo
import com.awbuilds.reproparcel.core.model.Manifest
import com.awbuilds.reproparcel.core.model.ParcelLimits
import com.awbuilds.reproparcel.core.model.PrivacyInfo
import com.awbuilds.reproparcel.core.model.ProducerInfo
import com.awbuilds.reproparcel.core.model.Report
import com.awbuilds.reproparcel.core.parcel.ParcelPayload
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExportManagerTest {
    private lateinit var context: Context
    private lateinit var manager: ExportManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        manager = ExportManager(context)
        manager.clearExports()
    }

    @Test
    fun writesAValidParcelToAProviderUri() {
        val directory = File(context.cacheDir, "reproparcel/exports").apply { mkdirs() }
        val target = File(directory, "synthetic.reproparcel").apply { createNewFile() }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.reproparcel.fileprovider",
            target,
        )
        val result = runBlocking {
            manager.saveToUri(uri, manifest(), ParcelPayload(Report(description = "Synthetic")))
        }
        assertEquals(ExportOutcome.FileSaved, result)
        assertTrue(target.length() > 0L)
    }

    @Test
    fun clearExportsRemovesTemporaryFiles() {
        val directory = File(context.cacheDir, "reproparcel/exports").apply { mkdirs() }
        File(directory, "one.reproparcel").writeText("synthetic")
        assertTrue(manager.clearExports() >= 1)
    }

    private fun manifest(): Manifest = Manifest(
        reportId = "rpt_export_test",
        createdAt = "2026-11-11T00:00:00Z",
        producer = ProducerInfo("0.0.1-SNAPSHOT", "android", "test"),
        app = AppInfo("com.awbuilds.reproparcel.synthetic", "1.0", 1, "debug"),
        device = DeviceInfo(30, "Synthetic", "Fixture", 1080, 1920, 420, "en-US", 1f, "portrait"),
        privacy = PrivacyInfo("test-policy", "opaque"),
        capture = CaptureInfo("2026-11-11T00:00:01Z"),
        entries = emptyList(),
        limits = ParcelLimits(),
    )
}
