// SPDX-License-Identifier: Apache-2.0

package com.awbuilds.reproparcel.spike.capturelab

import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Paint
import android.graphics.Rect
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                CaptureLabScreen()
            }
        }
    }
}

@Composable
private fun CaptureLabScreen() {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var status by remember { mutableStateOf("Ready") }
    var semantics by remember { mutableStateOf("Semantics not collected") }
    var showDialog by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf("") }

    fun capture(label: String, mask: Boolean = true) {
        if (activity == null) {
            status = "No activity"
            return
        }
        scope.launch {
            status = "$label: capturing…"
            val result = CaptureLab.captureWindow(activity.window)
            when (result.status) {
                CaptureStatus.SUCCESS -> {
                    val bitmap = result.bitmap ?: return@launch
                    val region = Rect(0, 0, minOf(96, bitmap.width), minOf(48, bitmap.height))
                    val output = if (mask) CaptureLab.applyOpaqueMask(bitmap, region) else bitmap
                    val pixel = CaptureLab.samplePixel(output, 8, 8)
                    val file = File(
                        context.cacheDir,
                        "spike-${System.currentTimeMillis()}.png",
                    )
                    val saved = CaptureLab.savePng(output, file)
                    status = buildString {
                        append(label)
                        append(": SUCCESS ")
                        append(result.width)
                        append("x")
                        append(result.height)
                        append(" in ")
                        append(result.elapsedMs)
                        append("ms; masked=#")
                        append("%08X".format(pixel))
                        append("; saved=")
                        append(saved)
                    }
                    bitmap.recycle()
                    if (output !== bitmap) output.recycle()
                }

                CaptureStatus.SECURE_WINDOW -> status = "$label: OMITTED (secure window)"
                CaptureStatus.TIMEOUT -> status = "$label: OMITTED (timeout ${result.elapsedMs}ms)"
                CaptureStatus.FAILED -> status = "$label: FAILED (${result.detail})"
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Capture Lab", style = MaterialTheme.typography.headlineSmall)
        Text("Synthetic feasibility spike. No real user data.", fontSize = 12.sp)
        Text(status, style = MaterialTheme.typography.bodyMedium)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(onClick = { capture("capture") }) { Text("Capture") }
            Button(onClick = { capture("unmasked", mask = false) }) { Text("Raw") }
            Button(onClick = {
                val intent = Intent(context, SecureActivity::class.java)
                context.startActivity(intent)
            }) { Text("Secure") }
            Button(onClick = {
                val snapshot = activity?.window?.decorView?.let(CaptureLab::dumpSemantics)
                semantics = snapshot?.let {
                    "nodes=${it.nodes.size} truncated=${it.truncated} " +
                        "bytes≈${it.approximateBytes} ms=${it.elapsedMs} " +
                        "first=${it.nodes.firstOrNull { node -> node.text != null }?.text}"
                } ?: "No activity"
            }) { Text("Semantics") }
            Button(onClick = {
                scope.launch {
                    listState.scrollToItem(80)
                    delay(30)
                    capture("scroll-race")
                }
            }) { Text("Scroll race") }
            Button(onClick = {
                activity?.requestedOrientation =
                    if (activity.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    } else {
                        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    }
            }) { Text("Rotate") }
            Button(onClick = { showDialog = true }) { Text("Dialog") }
        }

        Text(semantics, fontSize = 11.sp)

        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("Synthetic input") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .size(72.dp)
                .background(Color(0xFFE8DEF8), RoundedCornerShape(12.dp))
                .semantics { contentDescription = "Synthetic canvas" },
        ) {
            drawRect(
                color = Color(0xFF6750A4),
                topLeft = Offset(size.width / 4f, size.height / 4f),
                size = Size(size.width / 2f, size.height / 2f),
                style = androidx.compose.ui.graphics.drawscope.Fill,
            )
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items((1..100).map { "Synthetic row $it" }) { label ->
                Text(
                    text = label,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF3EDF7), RoundedCornerShape(8.dp))
                        .padding(12.dp),
                )
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Synthetic dialog") },
            text = { Text("This dialog tests capture and semantics coverage.") },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) { Text("Close") }
            },
        )
    }
}
