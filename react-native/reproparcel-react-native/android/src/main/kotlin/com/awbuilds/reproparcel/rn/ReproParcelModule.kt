// SPDX-License-Identifier: AGPL-3.0-only
package com.awbuilds.reproparcel.rn

import com.awbuilds.reproparcel.android.Breadcrumb
import com.awbuilds.reproparcel.android.LogEvent
import com.awbuilds.reproparcel.android.ReproParcel
import com.awbuilds.reproparcel.compose.ReproParcelCompose
import com.awbuilds.reproparcel.core.config.CapturePolicy
import com.awbuilds.reproparcel.core.config.EvidenceConfig
import com.awbuilds.reproparcel.core.config.ReproParcelConfig
import com.awbuilds.reproparcel.core.model.BuildInfo
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import org.json.JSONObject

class ReproParcelModule(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

    override fun getName(): String = "ReproParcel"

    @ReactMethod
    fun initialize(configJson: String, promise: Promise) {
        try {
            val json = JSONObject(configJson)
            val policyJson = json.optJSONObject("capturePolicy")
            val policy = policyJson?.let {
                CapturePolicy(
                    policyVersion = it.optString("policyVersion", "rn-policy"),
                    screenshotEnabled = it.optBoolean("screenshotEnabled", false),
                    semanticTextEnabled = it.optBoolean("semanticTextEnabled", false),
                    eligibleSurfaces = it.optJSONArray("eligibleSurfaces")?.let { array ->
                        (0 until array.length()).map { index -> array.getString(index) }.toSet()
                    }.orEmpty(),
                )
            }
            val config = ReproParcelConfig(
                enabled = json.optBoolean("enabled", false),
                buildInfo = BuildInfo(
                    packageName = json.getString("packageName"),
                    versionName = json.getString("versionName"),
                    versionCode = json.getLong("versionCode"),
                    variant = json.getString("variant"),
                    gitCommit = if (json.isNull("gitCommit")) null else json.optString("gitCommit").takeIf { it.isNotBlank() },
                ),
                capturePolicy = policy,
                evidence = EvidenceConfig(
                    screenshotEnabled = policy?.screenshotEnabled == true,
                    semanticTextEnabled = policy?.semanticTextEnabled == true,
                ),
            )
            val result = ReproParcel.initialize(reactApplicationContext, config)
            promise.resolve(result == com.awbuilds.reproparcel.core.result.SdkResult.Success)
        } catch (failure: Exception) {
            promise.reject("REPROPARCEL_INIT", "Initialization failed")
        }
    }

    @ReactMethod
    fun setEnabled(enabled: Boolean, promise: Promise) {
        promise.resolve(ReproParcel.setEnabled(enabled) == com.awbuilds.reproparcel.core.result.SdkResult.Success)
    }

    @ReactMethod
    fun showReporter(surfaceId: String, promise: Promise) {
        val activity = reactApplicationContext.currentActivity
        if (activity == null) {
            promise.resolve(false)
            return
        }
        val result = ReproParcelCompose.showReporter(activity, surfaceId)
        promise.resolve(result == com.awbuilds.reproparcel.core.result.SdkResult.Success)
    }

    @ReactMethod
    fun recordBreadcrumb(json: String, promise: Promise) {
        try {
            val value = JSONObject(json)
            val attributes = value.optJSONObject("attributes")?.let { obj ->
                buildMap {
                    obj.keys().forEach { key -> put(key, obj.optString(key)) }
                }
            }.orEmpty()
            val result = ReproParcel.recordBreadcrumb(
                Breadcrumb(
                    category = value.optString("category", "navigation"),
                    actionId = value.optString("actionId").takeIf { it.isNotBlank() },
                    screenId = value.optString("screenId").takeIf { it.isNotBlank() },
                    attributes = attributes,
                    outcome = value.optString("outcome").takeIf { it.isNotBlank() },
                ),
            )
            promise.resolve(result == com.awbuilds.reproparcel.core.result.SdkResult.Success)
        } catch (failure: Exception) {
            promise.reject("REPROPARCEL_BREADCRUMB", "Breadcrumb failed")
        }
    }

    @ReactMethod
    fun recordLog(json: String, promise: Promise) {
        try {
            val value = JSONObject(json)
            val attributes = value.optJSONObject("attributes")?.let { obj ->
                buildMap {
                    obj.keys().forEach { key -> put(key, obj.optString(key)) }
                }
            }.orEmpty()
            val result = ReproParcel.recordLog(
                LogEvent(
                    level = value.optString("level", "info"),
                    code = value.optString("code", "rn_event"),
                    attributes = attributes,
                ),
            )
            promise.resolve(result == com.awbuilds.reproparcel.core.result.SdkResult.Success)
        } catch (failure: Exception) {
            promise.reject("REPROPARCEL_LOG", "Log failed")
        }
    }

    @ReactMethod
    fun deleteLocalReports(promise: Promise) {
        promise.resolve(ReproParcel.deleteLocalReports() == com.awbuilds.reproparcel.core.result.SdkResult.Success)
    }
}
