// SPDX-License-Identifier: AGPL-3.0-only

package com.awbuilds.reproparcel.spike.capturelab

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

class SecureActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                SecureScreen()
            }
        }
    }
}

@Composable
private fun SecureScreen() {
    val activity = LocalActivity.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf("FLAG_SECURE is set") }

    fun attempt(respect: Boolean) {
        val target = activity ?: return
        scope.launch {
            status = "Attempting (respect=$respect)…"
            val result = CaptureLab.captureWindow(
                window = target.window,
                respectSecureFlag = respect,
            )
            status = buildString {
                append("respect=")
                append(respect)
                append(" -> ")
                append(result.status)
                append(" ")
                append(result.detail)
                result.bitmap?.let { bitmap ->
                    val pixel = CaptureLab.samplePixel(bitmap, bitmap.width / 2, bitmap.height / 2)
                    append(" center=#")
                    append("%08X".format(pixel))
                }
            }
            result.bitmap?.recycle()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Protected window", style = MaterialTheme.typography.headlineSmall)
        Text("Synthetic content. Capture must be omitted or fail.")
        Text(status)
        Button(onClick = { attempt(true) }) { Text("Capture with secure check") }
        Button(onClick = { attempt(false) }) { Text("Attempt raw PixelCopy") }
    }
}
