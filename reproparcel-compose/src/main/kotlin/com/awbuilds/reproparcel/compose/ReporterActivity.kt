// SPDX-License-Identifier: AGPL-3.0-only
package com.awbuilds.reproparcel.compose

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.awbuilds.reproparcel.android.ReproParcel
import com.awbuilds.reproparcel.android.capture.HandoffCapture
import com.awbuilds.reproparcel.android.capture.ReporterHandoff
import com.awbuilds.reproparcel.android.device.DeviceInfoProvider
import com.awbuilds.reproparcel.android.draft.DraftSaveResult
import com.awbuilds.reproparcel.android.draft.DraftStore
import com.awbuilds.reproparcel.android.export.ExportManager
import com.awbuilds.reproparcel.android.export.ExportOutcome
import com.awbuilds.reproparcel.core.config.RetentionPolicy
import com.awbuilds.reproparcel.core.model.AppInfo
import com.awbuilds.reproparcel.core.model.CaptureInfo
import com.awbuilds.reproparcel.core.model.EvidenceState
import com.awbuilds.reproparcel.core.model.EvidenceStatus
import com.awbuilds.reproparcel.core.model.Manifest
import com.awbuilds.reproparcel.core.model.OmissionReason
import com.awbuilds.reproparcel.core.model.ParcelLimits
import com.awbuilds.reproparcel.core.model.PrivacyInfo
import com.awbuilds.reproparcel.core.model.ProducerInfo
import com.awbuilds.reproparcel.core.model.ProviderStatus
import com.awbuilds.reproparcel.core.model.Report
import com.awbuilds.reproparcel.core.parcel.ParcelPayload
import java.time.Instant
import java.util.UUID

class ReporterActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                ReporterScreen()
            }
        }
    }

    override fun onDestroy() {
        ReporterHandoff.clear()
        super.onDestroy()
    }
}

private enum class ReporterStep {
    FORM,
    EDIT,
    REVIEW,
}

@Composable
private fun ReporterScreen() {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val handoff = remember { ReporterHandoff.take() }
    val capturedBitmap = (handoff as? HandoffCapture.Captured)?.bitmap
    val omission = (handoff as? HandoffCapture.Omitted)?.reason
    val reportId = remember { UUID.randomUUID().toString() }
    val createdAt = remember { Instant.now().toString() }
    val annotations = remember { mutableStateListOf<Annotation>() }
    val draftStore = remember {
        DraftStore(
            context,
            ReproParcel.currentRetentionPolicy() ?: RetentionPolicy(),
        )
    }
    val exportManager = remember { ExportManager(context) }

    var step by remember { mutableStateOf(if (capturedBitmap != null) ReporterStep.EDIT else ReporterStep.FORM) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var expected by remember { mutableStateOf("") }
    var actual by remember { mutableStateOf("") }
    var repro by remember { mutableStateOf("") }
    var tool by remember { mutableStateOf(AnnotationTool.PEN) }
    var finalPng by remember { mutableStateOf<ByteArray?>(null) }
    var reviewed by remember { mutableStateOf(false) }
    var includeImage by remember { mutableStateOf(true) }
    var exported by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    var pendingExport by remember { mutableStateOf<Pair<Manifest, ParcelPayload>?>(null) }

    DisposableEffect(capturedBitmap) {
        onDispose { capturedBitmap?.recycle() }
    }

    LaunchedEffect(description, expected, actual, repro, finalPng, includeImage) {
        reviewed = false
    }

    fun buildReport(): Report {
        return Report(
            title = title.take(200),
            description = description.take(20_000),
            expectedBehavior = expected.take(20_000),
            actualBehavior = actual.take(20_000),
            reproSteps = repro.take(20_000).ifBlank { null },
            screenId = null,
            evidence = listOf(
                EvidenceStatus(
                    state = if (capturedBitmap != null && includeImage) EvidenceState.COLLECTED else EvidenceState.OMITTED,
                    reason = if (capturedBitmap == null || !includeImage) omission ?: OmissionReason.POLICY_DISABLED else null,
                ),
            ),
        )
    }

    fun buildManifest(): Manifest? {
        val build = ReproParcel.currentBuildInfo() ?: return null
        val policy = ReproParcel.currentCapturePolicy()
        return Manifest(
            reportId = reportId,
            createdAt = createdAt,
            producer = ProducerInfo(
                sdkVersion = "0.0.1-SNAPSHOT",
                platform = "android",
                collectionConfigurationId = ReproParcel.currentCollectionConfigurationId() ?: "unavailable",
            ),
            app = AppInfo(
                packageName = build.packageName,
                versionName = build.versionName,
                versionCode = build.versionCode,
                variant = build.variant,
                gitCommit = build.gitCommit,
            ),
            device = DeviceInfoProvider.collect(context),
            privacy = PrivacyInfo(
                policyVersion = policy?.policyVersion ?: "none",
                redactionMode = "opaque-and-user-annotations",
                reviewedCategories = setOf("description", "expected", "actual", "image"),
            ),
            capture = CaptureInfo(
                capturedAt = createdAt,
                collectionModes = if (capturedBitmap != null) setOf("manual_screenshot") else emptySet(),
                providers = listOf(
                    ProviderStatus(
                        providerId = "screenshot",
                        status = EvidenceStatus(
                            state = if (capturedBitmap != null) EvidenceState.COLLECTED else EvidenceState.OMITTED,
                            reason = if (capturedBitmap == null) omission ?: OmissionReason.POLICY_DISABLED else null,
                        ),
                    ),
                ),
            ),
            entries = emptyList(),
            limits = ParcelLimits(),
        )
    }

    fun buildPayload(): ParcelPayload = ParcelPayload(
        buildReport(),
        imagePng = if (includeImage) finalPng else null,
    )

    fun handleOutcome(outcome: ExportOutcome) {
        status = when (outcome) {
            ExportOutcome.FileSaved -> {
                exported = true
                "File saved locally. Files already shared cannot be recalled."
            }
            ExportOutcome.ShareLaunched -> {
                exported = true
                "Share chooser opened. Receipt by the receiving app is unknown."
            }
            ExportOutcome.Cancelled -> "Save cancelled."
            is ExportOutcome.Failed -> "Export failed (${outcome.code})."
        }
    }

    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri ->
        val pending = pendingExport
        if (uri == null) {
            status = "Save cancelled."
        } else if (pending != null) {
            handleOutcome(exportManager.saveToUri(uri, pending.first, pending.second))
        }
        pendingExport = null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Report a problem", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Only the reviewed content below leaves the app. No automatic upload occurs.",
            style = MaterialTheme.typography.bodySmall,
        )
        if (status.isNotBlank()) {
            Text(status, style = MaterialTheme.typography.bodyMedium)
        }

        when (step) {
            ReporterStep.FORM -> {
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (omission != null) {
                        Text("Image omitted: ${omission.name.lowercase()}", style = MaterialTheme.typography.bodySmall)
                    }
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("What happened?") },
                        modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Description" },
                    )
                    OutlinedTextField(
                        value = expected,
                        onValueChange = { expected = it },
                        label = { Text("Expected behavior") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = actual,
                        onValueChange = { actual = it },
                        label = { Text("Actual behavior") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = repro,
                        onValueChange = { repro = it },
                        label = { Text("Steps to reproduce (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            enabled = description.isNotBlank(),
                            onClick = { step = ReporterStep.REVIEW },
                        ) { Text("Review text-only report") }
                        if (capturedBitmap != null) {
                            Button(onClick = { step = ReporterStep.EDIT }) { Text("Edit image") }
                        }
                    }
                }
            }

            ReporterStep.EDIT -> {
                val bitmap = capturedBitmap ?: return@Column
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(onClick = { tool = AnnotationTool.PEN }) { Text("Pen") }
                        Button(onClick = { tool = AnnotationTool.ARROW }) { Text("Arrow") }
                        Button(onClick = { tool = AnnotationTool.MASK }) { Text("Mask") }
                        TextButton(
                            enabled = annotations.isNotEmpty(),
                            onClick = { if (annotations.isNotEmpty()) annotations.removeAt(annotations.lastIndex) },
                        ) { Text("Undo") }
                    }
                    AnnotatedImage(
                        bitmap = bitmap,
                        annotations = annotations,
                        tool = tool,
                        modifier = Modifier.fillMaxWidth().weight(1f),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            finalPng = BitmapFlattener.flatten(bitmap, annotations)
                            step = ReporterStep.REVIEW
                        }) { Text("Review image report") }
                        TextButton(onClick = { step = ReporterStep.FORM }) { Text("Back") }
                    }
                }
            }

            ReporterStep.REVIEW -> {
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Exact final content", style = MaterialTheme.typography.titleMedium)
                    if (finalPng != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = includeImage, onCheckedChange = { includeImage = it })
                            Text("Include the reviewed image")
                        }
                    }
                    if (includeImage && finalPng != null) {
                        val png = finalPng!!
                        val preview = remember(png) { BitmapFactory.decodeByteArray(png, 0, png.size) }
                        DisposableEffect(preview) {
                            onDispose { preview?.recycle() }
                        }
                        preview?.let {
                            Image(
                                bitmap = it.asImageBitmap(),
                                contentDescription = "Final reviewed image",
                                modifier = Modifier.fillMaxWidth().aspectRatio(it.width.toFloat() / it.height),
                                contentScale = ContentScale.Fit,
                            )
                        }
                    } else {
                        Text("Text-only report")
                    }
                    Text("Description: $description")
                    if (expected.isNotBlank()) Text("Expected: $expected")
                    if (actual.isNotBlank()) Text("Actual: $actual")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = reviewed, onCheckedChange = { reviewed = it })
                        Text("I reviewed the exact content above.")
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            enabled = canExport(exported, reviewed),
                            onClick = {
                                if (activity == null) {
                                    status = "No activity for sharing."
                                } else {
                                    val manifest = buildManifest()
                                    if (manifest == null) {
                                        status = "Manifest unavailable."
                                    } else {
                                        handleOutcome(exportManager.share(activity, manifest, buildPayload()))
                                    }
                                }
                            },
                        ) { Text("Share") }
                        Button(
                            enabled = canExport(exported, reviewed),
                            onClick = {
                                val manifest = buildManifest()
                                if (manifest == null) {
                                    status = "Manifest unavailable."
                                } else {
                                    pendingExport = manifest to buildPayload()
                                    saveLauncher.launch("report.reproparcel")
                                }
                            },
                        ) { Text("Save file") }
                    }
                    Button(
                        onClick = {
                            val manifest = buildManifest()
                            if (manifest == null) {
                                status = "Manifest unavailable."
                            } else {
                                status = when (val result = draftStore.save(buildReport(), finalPng)) {
                                    is DraftSaveResult.Saved -> "Encrypted draft saved locally."
                                    is DraftSaveResult.Failure -> "Draft failed (${result.code}); no plaintext fallback."
                                }
                            }
                        },
                    ) { Text("Save encrypted draft") }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { step = if (capturedBitmap != null) ReporterStep.EDIT else ReporterStep.FORM }) {
                            Text("Back")
                        }
                        TextButton(onClick = {
                            exported = false
                            reviewed = false
                            status = ""
                            step = ReporterStep.FORM
                        }) { Text("New report") }
                    }
                    Text(
                        "A shared file cannot be recalled from the recipient. " +
                            "System sharing reports only that the chooser opened.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

private fun canExport(exported: Boolean, reviewed: Boolean): Boolean =
    !exported && reviewed

@Composable
private fun AnnotatedImage(
    bitmap: Bitmap,
    annotations: MutableList<Annotation>,
    tool: AnnotationTool,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier
            .aspectRatio(bitmap.width.toFloat() / bitmap.height)
            .background(Color(0xFF111111), RoundedCornerShape(8.dp))
            .pointerInput(tool) {
                detectDragGestures(
                    onDragStart = { offset ->
                        annotations += Annotation(
                            tool = tool,
                            points = listOf(normalize(offset, size.width.toFloat(), size.height.toFloat())),
                        )
                    },
                    onDrag = { change, _ ->
                        val last = annotations.lastOrNull() ?: return@detectDragGestures
                        annotations[annotations.lastIndex] = last.copy(
                            points = last.points + normalize(
                                change.position,
                                size.width.toFloat(),
                                size.height.toFloat(),
                            ),
                        )
                    },
                )
            },
    ) {
        drawImage(
            image = bitmap.asImageBitmap(),
            dstOffset = IntOffset.Zero,
            dstSize = androidx.compose.ui.unit.IntSize(size.width.toInt(), size.height.toInt()),
        )
        annotations.forEach { annotation ->
            val points = annotation.points.map { Offset(it.x * size.width, it.y * size.height) }
            if (points.isEmpty()) return@forEach
            val color = if (annotation.tool == AnnotationTool.MASK) Color.Black else Color.Red
            val width = if (annotation.tool == AnnotationTool.MASK) 36.dp.toPx() else 4.dp.toPx()
            if (points.size == 1) {
                drawCircle(color, radius = width / 2f, center = points.first())
            } else {
                for (index in 1 until points.size) {
                    drawLine(color, points[index - 1], points[index], strokeWidth = width)
                }
            }
        }
    }
}

private fun normalize(offset: Offset, width: Float, height: Float): Offset =
    Offset((offset.x / width).coerceIn(0f, 1f), (offset.y / height).coerceIn(0f, 1f))
