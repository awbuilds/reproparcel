// SPDX-License-Identifier: Apache-2.0
package com.awbuilds.reproparcel.android

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.awbuilds.reproparcel.android.draft.DraftSaveResult
import com.awbuilds.reproparcel.android.draft.DraftStore
import com.awbuilds.reproparcel.android.export.ExportManager
import com.awbuilds.reproparcel.android.export.ExportOutcome
import com.awbuilds.reproparcel.android.sensor.ShakeDetector
import com.awbuilds.reproparcel.core.config.ReproParcelConfig
import com.awbuilds.reproparcel.core.config.RetentionPolicy
import com.awbuilds.reproparcel.core.model.AppInfo
import com.awbuilds.reproparcel.core.model.CaptureInfo
import com.awbuilds.reproparcel.core.model.DeviceInfo
import com.awbuilds.reproparcel.core.model.Manifest
import com.awbuilds.reproparcel.core.model.ParcelLimits
import com.awbuilds.reproparcel.core.model.PrivacyInfo
import com.awbuilds.reproparcel.core.model.ProducerInfo
import com.awbuilds.reproparcel.core.model.Report
import com.awbuilds.reproparcel.core.parcel.ParcelPayload
import com.awbuilds.reproparcel.core.result.ErrorCode
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FaultInjectionTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun revokedContentUriReturnsTypedExportFailureWithoutCrashing() = runBlocking {
        val outcome = ExportManager(context).saveToUri(
            Uri.parse("content://com.example.revoked/report"),
            manifest(),
            ParcelPayload(Report(description = "Synthetic")),
        )
        assertTrue(outcome is ExportOutcome.Failed)
        assertEquals(ErrorCode.EXPORT_FAILED, (outcome as ExportOutcome.Failed).code)
    }

    @Test
    fun storageFailureReturnsTypedDraftFailureWithoutPlaintextFallback() {
        val rootFile = File(context.cacheDir, "synthetic-draft-root")
        rootFile.writeText("not a directory")
        val store = DraftStore(
            context,
            RetentionPolicy(),
            rootOverride = rootFile,
        )
        val result = store.save(Report(description = "synthetic secret"))
        assertTrue(result is DraftSaveResult.Failure)
        assertEquals(ErrorCode.STORAGE_FAILED, (result as DraftSaveResult.Failure).code)
        assertFalse(rootFile.readText().contains("synthetic secret"))
        rootFile.delete()
    }

    @Test
    fun unavailableSensorIsAnExplicitFalseStart() {
        val detector = ShakeDetector(context, onShake = {}, sensorManager = null)
        assertFalse(detector.start())
        assertFalse(detector.isStarted)
        detector.stop()
    }

    @Test
    fun invalidConfigIsTypedAndLeavesRuntimeUninitialized() {
        val config = ReproParcelConfig(
            buildInfo = com.awbuilds.reproparcel.core.model.BuildInfo(
                packageName = "NOT A PACKAGE",
                versionName = "",
                versionCode = 0,
                variant = "debug",
            ),
        )
        assertEquals(
            com.awbuilds.reproparcel.core.result.SdkResult.Failure(ErrorCode.INVALID_CONFIG),
            ReproParcel.initialize(context, config),
        )
        assertFalse(ReproParcel.isInitialized())
    }

    private fun manifest(): Manifest = Manifest(
        reportId = "rpt_fault_test",
        createdAt = "2026-11-11T00:00:00Z",
        producer = ProducerInfo("0.0.1-SNAPSHOT", "android", "fault-test"),
        app = AppInfo("com.awbuilds.reproparcel.synthetic", "1.0", 1, "debug"),
        device = DeviceInfo(30, "Synthetic", "Fixture", 1080, 1920, 420, "en-US", 1f, "portrait"),
        privacy = PrivacyInfo("fault-policy", "opaque"),
        capture = CaptureInfo("2026-11-11T00:00:01Z"),
        entries = emptyList(),
        limits = ParcelLimits(),
    )
}
