// SPDX-License-Identifier: Apache-2.0
package com.awbuilds.reproparcel.flutter

import android.app.Activity
import com.awbuilds.reproparcel.android.Breadcrumb
import com.awbuilds.reproparcel.android.LogEvent
import com.awbuilds.reproparcel.android.ReproParcel
import com.awbuilds.reproparcel.compose.ReproParcelCompose
import com.awbuilds.reproparcel.core.config.CapturePolicy
import com.awbuilds.reproparcel.core.config.EvidenceConfig
import com.awbuilds.reproparcel.core.config.ReproParcelConfig
import com.awbuilds.reproparcel.core.model.BuildInfo
import com.awbuilds.reproparcel.core.result.SdkResult
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel

class ReproParcelFlutterPlugin : FlutterPlugin, MethodChannel.MethodCallHandler, ActivityAware {
    private var channel: MethodChannel? = null
    private var activity: Activity? = null
    private var appContext: android.content.Context? = null

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        appContext = binding.applicationContext
        channel = MethodChannel(binding.binaryMessenger, "reproparcel").also {
            it.setMethodCallHandler(this)
        }
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel?.setMethodCallHandler(null)
        channel = null
        appContext = null
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activity = binding.activity
    }

    override fun onDetachedFromActivityForConfigChanges() {
        activity = null
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        activity = binding.activity
    }

    override fun onDetachedFromActivity() {
        activity = null
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        try {
            when (call.method) {
                "initialize" -> result.success(initialize(call))
                "setEnabled" -> {
                    val enabled = call.arguments as? Boolean ?: false
                    result.success(ReproParcel.setEnabled(enabled) == SdkResult.Success)
                }
                "recordBreadcrumb" -> result.success(recordBreadcrumb(call))
                "recordLog" -> result.success(recordLog(call))
                "showReporter" -> showReporter(call, result)
                "deleteLocalReports" -> result.success(ReproParcel.deleteLocalReports() == SdkResult.Success)
                else -> result.notImplemented()
            }
        } catch (_: Exception) {
            // Plugin callbacks are a host seam. Return a typed false/fallback string
            // rather than letting a recoverable failure crash the Flutter engine.
            when (call.method) {
                "showReporter" -> result.success("failure")
                else -> result.success(false)
            }
        }
    }

    private fun initialize(call: MethodCall): Boolean {
        val context = appContext ?: return false
        val map = call.arguments as? Map<*, *> ?: return false
        val policyMap = map["capturePolicy"] as? Map<*, *>
        fun string(key: String, fallback: String = ""): String =
            (map[key] as? String) ?: fallback
        fun bool(key: String): Boolean = map[key] as? Boolean ?: false
        val policy = policyMap?.let { policy ->
            CapturePolicy(
                policyVersion = (policy["policyVersion"] as? String) ?: "flutter-policy",
                screenshotEnabled = policy["screenshotEnabled"] as? Boolean ?: false,
                semanticTextEnabled = policy["semanticTextEnabled"] as? Boolean ?: false,
                eligibleSurfaces = (policy["eligibleSurfaces"] as? List<*>)
                    ?.mapNotNull { it as? String }
                    ?.toSet()
                    .orEmpty(),
            )
        }
        val config = ReproParcelConfig(
            enabled = bool("enabled"),
            buildInfo = BuildInfo(
                packageName = string("packageName"),
                versionName = string("versionName"),
                versionCode = (map["versionCode"] as? Number)?.toLong() ?: 0L,
                variant = string("variant", "debug"),
                gitCommit = null,
            ),
            capturePolicy = policy,
            evidence = EvidenceConfig(
                screenshotEnabled = policy?.screenshotEnabled == true,
                semanticTextEnabled = policy?.semanticTextEnabled == true,
                structuredLogsEnabled = true,
            ),
        )
        return ReproParcel.initialize(context, config) == SdkResult.Success
    }

    private fun recordBreadcrumb(call: MethodCall): Boolean {
        val map = call.arguments as? Map<*, *> ?: return false
        val attributes = (map["attributes"] as? Map<*, *>)
            ?.entries
            ?.mapNotNull { (key, value) ->
                if (key is String && value is String) key to value else null
            }
            ?.toMap()
            .orEmpty()
        return ReproParcel.recordBreadcrumb(
            Breadcrumb(
                category = (map["category"] as? String) ?: "navigation",
                actionId = map["actionId"] as? String,
                screenId = map["screenId"] as? String,
                attributes = attributes,
                outcome = map["outcome"] as? String,
            ),
        ) == SdkResult.Success
    }

    private fun recordLog(call: MethodCall): Boolean {
        val map = call.arguments as? Map<*, *> ?: return false
        val attributes = (map["attributes"] as? Map<*, *>)
            ?.entries
            ?.mapNotNull { (key, value) ->
                if (key is String && value is String) key to value else null
            }
            ?.toMap()
            .orEmpty()
        return ReproParcel.recordLog(
            LogEvent(
                level = (map["level"] as? String) ?: "info",
                code = (map["code"] as? String) ?: "flutter_event",
                attributes = attributes,
            ),
        ) == SdkResult.Success
    }

    private fun showReporter(call: MethodCall, result: MethodChannel.Result) {
        val target = activity
        if (target == null) {
            result.success("host_unavailable")
            return
        }
        val surfaceId = call.arguments as? String ?: target.javaClass.name
        target.runOnUiThread {
            val outcome = try {
                ReproParcelCompose.showReporter(target, surfaceId)
            } catch (_: Exception) {
                SdkResult.Failure(
                    com.awbuilds.reproparcel.core.result.ErrorCode.INTERNAL_SAFE_FAILURE,
                )
            }
            result.success(
                when (outcome) {
                    is SdkResult.Success -> "success"
                    is SdkResult.Failure -> outcome.code.name.lowercase()
                    is SdkResult.NotInitialized -> "not_initialized"
                    is SdkResult.Disabled -> "disabled"
                    is SdkResult.HostUnavailable -> "host_unavailable"
                    is SdkResult.Omission -> "omission"
                    is SdkResult.Cancelled -> "cancelled"
                    is SdkResult.Unsupported -> "unsupported"
                },
            )
        }
    }
}
