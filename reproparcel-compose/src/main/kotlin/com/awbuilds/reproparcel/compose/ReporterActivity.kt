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
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.res.stringResource
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
import com.awbuilds.reproparcel.core.model.ReportEvent
import com.awbuilds.reproparcel.core.parcel.ParcelPayload
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    val scope = rememberCoroutineScope()
    val descriptionFieldDescription = stringResource(R.string.reporter_description_field_description)
    val surfaceId = activity?.intent?.getStringExtra("reproparcel_surface_id")
    val handoff = remember { ReporterHandoff.take() }
    var capturedBitmap by remember { mutableStateOf((handoff as? HandoffCapture.Captured)?.bitmap) }
    val omission = (handoff as? HandoffCapture.Omitted)?.reason
    var reportId by remember { mutableStateOf(UUID.randomUUID().toString()) }
    var createdAt by remember { mutableStateOf(Instant.now().toString()) }
    val annotations = remember { mutableStateListOf<Annotation>() }
    val draftStore = remember {
        DraftStore(
            context,
            ReproParcel.currentRetentionPolicy() ?: RetentionPolicy(),
        )
    }
    val exportManager = remember { ExportManager(context) }

    var step by remember { mutableStateOf(ReporterStep.FORM) }
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
    var frozenPayload by remember { mutableStateOf<ParcelPayload?>(null) }
    var previewHash by remember { mutableStateOf<String?>(null) }

    DisposableEffect(capturedBitmap) {
        onDispose { capturedBitmap?.recycle() }
    }

    LaunchedEffect(title, description, expected, actual, repro, finalPng, includeImage) {
        reviewed = false
        frozenPayload = null
        previewHash = null
    }

    fun payloadHash(payload: ParcelPayload): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(payload.report.toString().toByteArray(Charsets.UTF_8))
        digest.update(payload.events.toString().toByteArray(Charsets.UTF_8))
        digest.update((payload.semantics?.toString() ?: "none").toByteArray(Charsets.UTF_8))
        payload.imagePng?.let { digest.update(it) }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun beginReview(payload: ParcelPayload) {
        frozenPayload = payload
        previewHash = payloadHash(payload)
        reviewed = false
        step = ReporterStep.REVIEW
    }

    fun buildReport(): Report {
        return Report(
            title = title.take(200),
            description = description.take(20_000),
            expectedBehavior = expected.take(20_000),
            actualBehavior = actual.take(20_000),
            reproSteps = repro.take(20_000).ifBlank { null },
            screenId = surfaceId,
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
                reviewedCategories = setOf("title", "description", "expected", "actual", "repro", "image", "events", "semantics"),
                previewContentHash = previewHash,
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

    fun buildPayload(): ParcelPayload {
        val logs = ReproParcel.logSnapshot().map { log ->
            ReportEvent(
                sequence = 1_000_000 + log.sequence,
                offsetMs = log.offsetMs,
                category = "log",
                attributes = log.attributes + mapOf("level" to log.level, "code" to log.code),
            )
        }
        val network = ReproParcel.networkSnapshot().mapIndexed { index, item ->
            ReportEvent(
                sequence = 2_000_000L + index,
                offsetMs = 0,
                category = "network",
                attributes = buildMap {
                    put("http_method", item.method)
                    put("status_code", item.statusCode.toString())
                    put("duration_ms", item.durationMs.toString())
                    item.routeTemplate?.let { put("route_template", it) }
                    put("result", item.outcome)
                },
            )
        }
        val snapshot = ReproParcel.semanticSnapshot()
        return ParcelPayload(
            report = buildReport(),
            events = (ReproParcel.eventSnapshot() + logs + network).take(50),
            semantics = snapshot.takeIf { it.nodes.isNotEmpty() },
            imagePng = if (includeImage) finalPng else null,
        )
    }

    fun handleOutcome(outcome: ExportOutcome) {
        status = when (outcome) {
            ExportOutcome.FileSaved -> {
                exported = true
                context.resources.getString(R.string.reporter_file_saved)
            }
            ExportOutcome.ShareLaunched -> {
                exported = true
                context.resources.getString(R.string.reporter_share_launched)
            }
            ExportOutcome.Cancelled -> context.resources.getString(R.string.reporter_save_cancelled)
            is ExportOutcome.Failed -> context.resources.getString(R.string.reporter_export_failed, outcome.code.name)
        }
    }

    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri ->
        val pending = pendingExport
        if (uri == null) {
            status = context.resources.getString(R.string.reporter_save_cancelled)
        } else if (pending != null) {
            scope.launch {
                handleOutcome(exportManager.saveToUri(uri, pending.first, pending.second))
            }
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
        Text(stringResource(R.string.reporter_title), style = MaterialTheme.typography.headlineSmall)
        Text(
            "Only the reviewed content below leaves the app. No automatic upload occurs.",
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            stringResource(R.string.reporter_retention_note),
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
                        Text(stringResource(R.string.reporter_image_omitted, omission.name.lowercase()), style = MaterialTheme.typography.bodySmall)
                    }
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it.take(200) },
                        label = { Text(stringResource(R.string.reporter_title_optional)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it.take(20_000) },
                        label = { Text(stringResource(R.string.reporter_what_happened)) },
                        modifier = Modifier.fillMaxWidth().semantics { contentDescription = descriptionFieldDescription },
                    )
                    OutlinedTextField(
                        value = expected,
                        onValueChange = { expected = it.take(20_000) },
                        label = { Text(stringResource(R.string.reporter_expected)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = actual,
                        onValueChange = { actual = it.take(20_000) },
                        label = { Text(stringResource(R.string.reporter_actual)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = repro,
                        onValueChange = { repro = it.take(20_000) },
                        label = { Text(stringResource(R.string.reporter_steps_optional)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            enabled = description.isNotBlank(),
                            onClick = { beginReview(buildPayload()) },
                        ) { Text(stringResource(R.string.reporter_review_text_only)) }
                        if (capturedBitmap != null) {
                            Button(onClick = { step = ReporterStep.EDIT }) { Text(stringResource(R.string.reporter_edit_image)) }
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
                        Button(onClick = { tool = AnnotationTool.PEN }) { Text(stringResource(R.string.reporter_pen)) }
                        Button(onClick = { tool = AnnotationTool.ARROW }) { Text(stringResource(R.string.reporter_arrow)) }
                        Button(onClick = { tool = AnnotationTool.MASK }) { Text(stringResource(R.string.reporter_mask)) }
                        TextButton(
                            enabled = annotations.isNotEmpty(),
                            onClick = { if (annotations.isNotEmpty()) annotations.removeAt(annotations.lastIndex) },
                        ) { Text(stringResource(R.string.reporter_undo)) }
                    }
                    AnnotatedImage(
                        bitmap = bitmap,
                        annotations = annotations,
                        tool = tool,
                        modifier = Modifier.fillMaxWidth().weight(1f),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            enabled = description.isNotBlank(),
                            onClick = {
                                scope.launch {
                                    val png = withContext(Dispatchers.Default) {
                                        BitmapFlattener.flatten(bitmap, annotations)
                                    }
                                    finalPng = png
                                    beginReview(buildPayload().copy(imagePng = if (includeImage) png else null))
                                }
                            },
                        ) { Text(stringResource(R.string.reporter_review_image)) }
                        TextButton(onClick = { step = ReporterStep.FORM }) { Text(stringResource(R.string.reporter_back)) }
                    }
                }
            }

            ReporterStep.REVIEW -> {
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(stringResource(R.string.reporter_exact_content), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(
                            R.string.reporter_evidence_summary,
                            frozenPayload?.events?.size ?: 0,
                            frozenPayload?.semantics?.nodes?.size ?: 0,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(stringResource(R.string.reporter_export_summary), style = MaterialTheme.typography.bodySmall)
                    if (finalPng != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = includeImage, onCheckedChange = { includeImage = it })
                            Text(stringResource(R.string.reporter_include_image))
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
                                contentDescription = stringResource(R.string.reporter_final_image_description),
                                modifier = Modifier.fillMaxWidth().aspectRatio(it.width.toFloat() / it.height),
                                contentScale = ContentScale.Fit,
                            )
                        }
                    } else {
                        Text(stringResource(R.string.reporter_text_only))
                    }
                    if (title.isNotBlank()) Text(stringResource(R.string.reporter_title_value, title))
                    Text(stringResource(R.string.reporter_description_value, description))
                    if (expected.isNotBlank()) Text(stringResource(R.string.reporter_expected_value, expected))
                    if (actual.isNotBlank()) Text(stringResource(R.string.reporter_actual_value, actual))
                    if (repro.isNotBlank()) Text(stringResource(R.string.reporter_steps_value, repro))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = reviewed, onCheckedChange = { reviewed = it })
                        Text(stringResource(R.string.reporter_reviewed))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            enabled = canExport(exported, reviewed),
                            onClick = {
                                if (activity == null) {
                                    status = context.resources.getString(R.string.reporter_no_activity)
                                } else {
                                    scope.launch {
                                        val manifest = buildManifest()
                                        if (manifest == null) {
                                            status = context.resources.getString(R.string.reporter_manifest_unavailable)
                                        } else {
                                            handleOutcome(
                                                exportManager.share(
                                                    activity,
                                                    manifest,
                                                    frozenPayload ?: buildPayload(),
                                                ),
                                            )
                                        }
                                    }
                                }
                            },
                        ) { Text(stringResource(R.string.reporter_share)) }
                        Button(
                            enabled = canExport(exported, reviewed),
                            onClick = {
                                val manifest = buildManifest()
                                if (manifest == null) {
                                    status = context.resources.getString(R.string.reporter_manifest_unavailable)
                                } else {
                                    pendingExport = manifest to (frozenPayload ?: buildPayload())
                                    saveLauncher.launch("report.reproparcel")
                                }
                            },
                        ) { Text(stringResource(R.string.reporter_save_file)) }
                    }
                    TextButton(
                        onClick = {
                            status = when (ReproParcel.deleteLocalReports()) {
                                com.awbuilds.reproparcel.core.result.SdkResult.Success ->
                                    context.resources.getString(R.string.reporter_deleted)
                                else -> context.resources.getString(R.string.reporter_delete_failed)
                            }
                        },
                    ) { Text(stringResource(R.string.reporter_delete_drafts)) }
                    Button(
                        onClick = {
                            scope.launch {
                                val manifest = buildManifest()
                                if (manifest == null) {
                                    status = context.resources.getString(R.string.reporter_manifest_unavailable)
                                } else {
                                    val result = withContext(Dispatchers.IO) {
                                        draftStore.save(
                                            buildReport(),
                                            if (includeImage) finalPng else null,
                                        )
                                    }
                                    status = when (result) {
                                        is DraftSaveResult.Saved -> context.resources.getString(R.string.reporter_draft_saved)
                                        is DraftSaveResult.Failure -> context.resources.getString(R.string.reporter_draft_failed, result.code.name)
                                    }
                                }
                            }
                        },
                    ) { Text(stringResource(R.string.reporter_save_draft)) }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { step = if (capturedBitmap != null) ReporterStep.EDIT else ReporterStep.FORM }) {
                            Text(stringResource(R.string.reporter_back))
                        }
                        TextButton(onClick = {
                            exported = false
                            reviewed = false
                            status = ""
                            title = ""
                            description = ""
                            expected = ""
                            actual = ""
                            repro = ""
                            capturedBitmap = null
                            annotations.clear()
                            finalPng = null
                            frozenPayload = null
                            previewHash = null
                            includeImage = false
                            ReproParcel.clearEvidence()
                            reportId = UUID.randomUUID().toString()
                            createdAt = Instant.now().toString()
                            step = ReporterStep.FORM
                        }) { Text(stringResource(R.string.reporter_new_report)) }
                    }
                    Text(
                        stringResource(R.string.reporter_share_warning),
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
