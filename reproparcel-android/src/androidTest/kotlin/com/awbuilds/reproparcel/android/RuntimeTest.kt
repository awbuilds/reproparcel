// SPDX-License-Identifier: AGPL-3.0-only
package com.awbuilds.reproparcel.android

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.awbuilds.reproparcel.core.config.CapturePolicy
import com.awbuilds.reproparcel.core.config.EvidenceConfig
import com.awbuilds.reproparcel.core.config.ReproParcelConfig
import com.awbuilds.reproparcel.core.model.BuildInfo
import com.awbuilds.reproparcel.core.model.Report
import com.awbuilds.reproparcel.core.config.RetentionPolicy
import com.awbuilds.reproparcel.android.draft.DraftStore
import com.awbuilds.reproparcel.core.model.Bounds
import com.awbuilds.reproparcel.core.model.NetworkMetadata
import com.awbuilds.reproparcel.core.model.OmissionReason
import com.awbuilds.reproparcel.core.model.SemanticNode
import com.awbuilds.reproparcel.core.result.ErrorCode
import com.awbuilds.reproparcel.core.result.SdkResult
import com.awbuilds.reproparcel.core.state.SessionEvent
import com.awbuilds.reproparcel.core.state.SessionState
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RuntimeTest {
    private lateinit var context: Context
    private val build = BuildInfo(
        packageName = "com.awbuilds.reproparcel.synthetic",
        versionName = "1.0.0",
        versionCode = 1,
        variant = "debug",
    )

    private fun config(enabled: Boolean = false): ReproParcelConfig = ReproParcelConfig(
        enabled = enabled,
        buildInfo = build,
        capturePolicy = CapturePolicy(
            policyVersion = "synthetic-v1",
            screenshotEnabled = false,
            semanticTextEnabled = true,
        ),
        evidence = EvidenceConfig(
            structuredLogsEnabled = true,
            networkMetadataEnabled = true,
            semanticTextEnabled = true,
        ),
    )

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        ReproParcel.resetForTest()
    }

    @After
    fun tearDown() {
        ReproParcel.resetForTest()
    }

    @Test
    fun disabledDefaultHasNoCollectionOrActiveJobs() {
        assertEquals(SdkResult.Success, ReproParcel.initialize(context, config(enabled = false)))
        assertTrue(ReproParcel.isInitialized())
        assertFalse(ReproParcel.isEnabled())
        assertFalse(ReproParcel.hasActiveScopeForTest())
        assertEquals(
            SdkResult.Disabled,
            ReproParcel.recordBreadcrumb(Breadcrumb("navigation", actionId = "open")),
        )
        assertEquals(
            SdkResult.Disabled,
            ReproParcel.recordLog(LogEvent("info", "sample")),
        )
        assertTrue(ReproParcel.eventSnapshotForTest().isEmpty())
        assertTrue(ReproParcel.logSnapshotForTest().isEmpty())
    }

    @Test
    fun invalidConfigReturnsTypedFailureAndStaysUninitialized() {
        val result = ReproParcel.initialize(
            context,
            config().copy(buildInfo = build.copy(packageName = "NOT A PACKAGE")),
        )
        assertEquals(SdkResult.Failure(ErrorCode.INVALID_CONFIG), result)
        assertFalse(ReproParcel.isInitialized())
    }

    @Test
    fun repeatedInitializeIsIdempotentAndConfigMismatchIsTyped() {
        assertEquals(SdkResult.Success, ReproParcel.initialize(context, config()))
        assertEquals(SdkResult.Success, ReproParcel.initialize(context, config()))
        assertEquals(
            SdkResult.Failure(ErrorCode.CONFIG_MISMATCH),
            ReproParcel.initialize(context, config(enabled = true)),
        )
    }

    @Test
    fun enableStartsExplicitCollectionAndDisableStopsIt() {
        assertEquals(SdkResult.Success, ReproParcel.initialize(context, config(enabled = false)))
        assertEquals(SdkResult.Success, ReproParcel.setEnabled(true))
        assertTrue(ReproParcel.isEnabled())
        assertTrue(ReproParcel.hasActiveScopeForTest())
        assertEquals(SdkResult.Success, ReproParcel.recordBreadcrumb(Breadcrumb("navigation", actionId = "open")))
        assertEquals(1, ReproParcel.eventSnapshotForTest().size)
        assertEquals(SdkResult.Success, ReproParcel.setEnabled(false))
        assertFalse(ReproParcel.isEnabled())
        assertFalse(ReproParcel.hasActiveScopeForTest())
        assertEquals(
            SdkResult.Disabled,
            ReproParcel.recordBreadcrumb(Breadcrumb("navigation", actionId = "open")),
        )
    }

    @Test
    fun unsafeAttributesAreNotBuffered() {
        assertEquals(SdkResult.Success, ReproParcel.initialize(context, config(enabled = true)))
        val result = ReproParcel.recordBreadcrumb(
            Breadcrumb(
                category = "navigation",
                actionId = "open",
                attributes = mapOf(
                    "screen_id" to "home",
                    "password" to "hunter2",
                    "route_template" to "/home?token=secret",
                ),
            ),
        )
        assertEquals(SdkResult.Success, result)
        val event = ReproParcel.eventSnapshotForTest().single()
        assertEquals(setOf("screen_id"), event.attributes.keys)
        assertEquals("home", event.attributes["screen_id"])
    }

    @Test
    fun sessionStateFollowsExplicitEnableAndDisable() {
        assertEquals(SdkResult.Success, ReproParcel.initialize(context, config(enabled = false)))
        assertEquals(SessionState.DISABLED, ReproParcel.sessionState.value)
        assertEquals(SdkResult.Success, ReproParcel.setEnabled(true))
        assertEquals(SessionState.READY, ReproParcel.sessionState.value)
        assertEquals(SdkResult.Success, ReproParcel.advanceSession(SessionEvent.StartCapture))
        assertEquals(SessionState.CAPTURING, ReproParcel.sessionState.value)
        assertEquals(SdkResult.Success, ReproParcel.setEnabled(false))
        assertEquals(SessionState.DISABLED, ReproParcel.sessionState.value)
    }

    @Test
    fun providerFailureIsIsolatedAndTyped() = runBlocking {
        assertEquals(SdkResult.Success, ReproParcel.initialize(context, config(enabled = true)))
        val registration = ProviderRegistration(
            id = "throwing",
            timeoutMs = 100,
            maxBytes = 100,
            provider = EvidenceProvider { error("synthetic provider failure") },
        )
        assertEquals(SdkResult.Success, ReproParcel.registerEvidenceProvider(registration))
        assertEquals(
            ProviderResult.Failed(ErrorCode.PROVIDER_FAILED),
            ReproParcel.collectProviderForTest("throwing"),
        )
    }

    @Test
    fun providerByteLimitIsEnforced() = runBlocking {
        assertEquals(SdkResult.Success, ReproParcel.initialize(context, config(enabled = true)))
        assertEquals(
            SdkResult.Success,
            ReproParcel.registerEvidenceProvider(
                ProviderRegistration(
                    id = "large",
                    timeoutMs = 100,
                    maxBytes = 10,
                    provider = EvidenceProvider { ProviderResult.Collected(ByteArray(11)) },
                ),
            ),
        )
        assertEquals(
            ProviderResult.Failed(ErrorCode.LIMIT_EXCEEDED),
            ReproParcel.collectProviderForTest("large"),
        )
    }

    @Test
    fun providerTimeoutIsAnOmission() = runBlocking {
        assertEquals(SdkResult.Success, ReproParcel.initialize(context, config(enabled = true)))
        assertEquals(
            SdkResult.Success,
            ReproParcel.registerEvidenceProvider(
                ProviderRegistration(
                    id = "slow",
                    timeoutMs = 10,
                    maxBytes = 100,
                    provider = EvidenceProvider {
                        delay(100)
                        ProviderResult.Collected(ByteArray(1))
                    },
                ),
            ),
        )
        assertEquals(
            ProviderResult.Omitted(OmissionReason.PROVIDER_FAILED),
            ReproParcel.collectProviderForTest("slow"),
        )
    }

    @Test
    fun networkMetadataKeepsSafeRouteAndDropsQuery() {
        assertEquals(SdkResult.Success, ReproParcel.initialize(context, config(enabled = true)))
        assertEquals(
            SdkResult.Success,
            ReproParcel.recordNetworkMetadata(
                NetworkMetadata("GET", 200, 12, "/orders/{id}"),
            ),
        )
        val metadata = ReproParcel.networkSnapshotForTest().single()
        assertEquals("/orders/{id}", metadata.routeTemplate)
        assertEquals(SdkResult.Success, ReproParcel.recordNetworkMetadata(
            NetworkMetadata("GET", 200, 12, "/orders?id=secret"),
        ))
        assertEquals(2, ReproParcel.networkSnapshotForTest().size)
        assertEquals(null, ReproParcel.networkSnapshotForTest().last().routeTemplate)
    }

    @Test
    fun explicitSemanticsBoundsAndSensitiveNodesAreSafe() {
        assertEquals(SdkResult.Success, ReproParcel.initialize(context, config(enabled = true)))
        assertEquals(
            SdkResult.Success,
            ReproParcel.registerSemanticNode(
                SemanticNode(
                    nodeId = "n1",
                    role = "button",
                    label = "Save",
                    bounds = Bounds(10, 10, 100, 50),
                ),
            ),
        )
        assertEquals(
            SdkResult.Success,
            ReproParcel.registerSemanticNode(
                SemanticNode(
                    nodeId = "n2",
                    role = "input",
                    label = "Synthetic secret",
                    testTag = "secret",
                    bounds = Bounds(10, 60, 100, 90),
                    sensitive = true,
                ),
            ),
        )
        val snapshot = ReproParcel.semanticSnapshot()
        assertEquals(2, snapshot.nodes.size)
        assertEquals(null, snapshot.nodes.single { it.nodeId == "n2" }.label)
        assertEquals(null, snapshot.nodes.single { it.nodeId == "n2" }.testTag)
        assertEquals(
            SdkResult.Failure(ErrorCode.INVALID_CONFIG),
            ReproParcel.registerSemanticNode(
                SemanticNode("bad", "button", bounds = Bounds(10, 10, 10, 20)),
            ),
        )
        assertEquals(SdkResult.Success, ReproParcel.clearSemanticEvidence())
        assertTrue(ReproParcel.semanticSnapshot().nodes.isEmpty())
    }

    @Test
    fun disableCancelsSdkOwnedJob() {
        assertEquals(SdkResult.Success, ReproParcel.initialize(context, config(enabled = true)))
        val job = ReproParcel.launchForTest { awaitCancellation() }
        assertEquals(SdkResult.Success, ReproParcel.setEnabled(false))
        assertTrue(job?.isCancelled == true)
    }

    @Test
    fun optionalCollectorsRespectDisabledFlags() {
        assertEquals(
            SdkResult.Success,
            ReproParcel.initialize(context, config(enabled = true).copy(evidence = EvidenceConfig())),
        )
        assertEquals(SdkResult.Disabled, ReproParcel.recordLog(LogEvent("info", "sample")))
        assertEquals(
            SdkResult.Disabled,
            ReproParcel.recordNetworkMetadata(NetworkMetadata("GET", 200, 1)),
        )
        assertEquals(
            SdkResult.Disabled,
            ReproParcel.registerSemanticNode(
                SemanticNode("n1", "button", bounds = Bounds(0, 0, 10, 10)),
            ),
        )
    }

    @Test
    fun deleteLocalReportsActuallyRemovesDraftsAndBuffers() {
        val store = DraftStore(context, RetentionPolicy())
        store.deleteAll()
        assertTrue(store.save(Report(description = "synthetic draft")).toString().contains("Saved"))
        assertEquals(SdkResult.Success, ReproParcel.initialize(context, config(enabled = true)))
        assertEquals(SdkResult.Success, ReproParcel.recordBreadcrumb(Breadcrumb("navigation", actionId = "open")))
        assertEquals(SdkResult.Success, ReproParcel.deleteLocalReports())
        assertTrue(store.list().isEmpty())
        assertTrue(ReproParcel.eventSnapshotForTest().isEmpty())
    }

    @Test
    fun disableClearsBuffersAndSemantics() {
        assertEquals(SdkResult.Success, ReproParcel.initialize(context, config(enabled = true)))
        assertEquals(SdkResult.Success, ReproParcel.recordBreadcrumb(Breadcrumb("navigation", actionId = "open")))
        assertEquals(
            SdkResult.Success,
            ReproParcel.registerSemanticNode(
                SemanticNode("n1", "button", bounds = Bounds(0, 0, 10, 10)),
            ),
        )
        assertEquals(SdkResult.Success, ReproParcel.setEnabled(false))
        assertTrue(ReproParcel.eventSnapshotForTest().isEmpty())
        assertTrue(ReproParcel.semanticSnapshot().nodes.isEmpty())
    }
}
