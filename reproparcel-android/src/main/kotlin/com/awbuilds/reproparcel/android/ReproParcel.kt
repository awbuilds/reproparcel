// SPDX-License-Identifier: AGPL-3.0-only
package com.awbuilds.reproparcel.android

import android.app.Activity
import android.content.Context
import android.os.Looper
import androidx.annotation.MainThread
import com.awbuilds.reproparcel.core.buffer.BoundedEventBuffer
import com.awbuilds.reproparcel.core.buffer.BoundedLogBuffer
import com.awbuilds.reproparcel.core.buffer.BoundedNetworkBuffer
import com.awbuilds.reproparcel.core.config.ConfigValidation
import com.awbuilds.reproparcel.core.config.ConfigValidator
import com.awbuilds.reproparcel.core.config.ReproParcelConfig
import com.awbuilds.reproparcel.core.model.OmissionReason
import com.awbuilds.reproparcel.core.model.NetworkMetadata
import com.awbuilds.reproparcel.core.model.SemanticNode
import com.awbuilds.reproparcel.core.model.SemanticsSnapshot
import com.awbuilds.reproparcel.core.model.ReportEvent
import com.awbuilds.reproparcel.core.model.StructuredLogEvent
import com.awbuilds.reproparcel.core.privacy.Redactor
import com.awbuilds.reproparcel.core.privacy.SafeAttribute
import com.awbuilds.reproparcel.core.result.ErrorCode
import com.awbuilds.reproparcel.core.result.SdkResult
import com.awbuilds.reproparcel.core.state.SessionEvent
import com.awbuilds.reproparcel.core.state.SessionState
import com.awbuilds.reproparcel.core.state.SessionStateMachine
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

public data class ReproParcelDispatchers(
    public val io: CoroutineDispatcher = Dispatchers.IO,
    public val main: CoroutineDispatcher = Dispatchers.Main,
)

public data class Breadcrumb(
    public val category: String,
    public val actionId: String? = null,
    public val screenId: String? = null,
    public val attributes: Map<String, String> = emptyMap(),
    public val outcome: String? = null,
)

public data class LogEvent(
    public val level: String,
    public val code: String,
    public val attributes: Map<String, String> = emptyMap(),
)

public fun interface EvidenceProvider {
    public suspend fun collect(): ProviderResult
}

public data class ProviderRegistration(
    public val id: String,
    public val timeoutMs: Long = 1_000L,
    public val maxBytes: Long = 256L * 1024,
    public val provider: EvidenceProvider,
)

public sealed interface ProviderResult {
    public data class Collected(public val bytes: ByteArray) : ProviderResult
    public data class Omitted(public val reason: OmissionReason) : ProviderResult
    public data class Failed(public val code: ErrorCode) : ProviderResult
}

public object ReproParcel {
    private enum class RuntimeState {
        UNINITIALIZED,
        DISABLED,
        ENABLED,
    }

    private val lock = Any()
    private var runtimeState: RuntimeState = RuntimeState.UNINITIALIZED
    private var config: ReproParcelConfig? = null
    private var appContext: Context? = null
    private var dispatchers: ReproParcelDispatchers = ReproParcelDispatchers()
    private var scope: CoroutineScope? = null
    private var eventBuffer: BoundedEventBuffer? = null
    private var logBuffer: BoundedLogBuffer? = null
    private var networkBuffer: BoundedNetworkBuffer? = null
    private val semanticNodes = linkedMapOf<String, SemanticNode>()
    private var sequence: Long = 0L
    private var stateMachine = SessionStateMachine()
    private val providers = linkedMapOf<String, ProviderRegistration>()
    private val _sessionState = MutableStateFlow(SessionState.DISABLED)

    public val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    public fun initialize(
        context: Context,
        config: ReproParcelConfig,
        dispatchers: ReproParcelDispatchers = ReproParcelDispatchers(),
    ): SdkResult {
        val validation = ConfigValidator.validate(config)
        if (validation is ConfigValidation.Invalid) {
            return SdkResult.Failure(ErrorCode.INVALID_CONFIG)
        }
        synchronized(lock) {
            if (runtimeState != RuntimeState.UNINITIALIZED) {
                val existing = this.config
                return if (existing?.buildInfo == config.buildInfo &&
                    existing.enabled == config.enabled &&
                    existing.capturePolicy == config.capturePolicy &&
                    existing.retention == config.retention &&
                    existing.evidence == config.evidence
                ) {
                    SdkResult.Success
                } else {
                    SdkResult.Failure(ErrorCode.CONFIG_MISMATCH)
                }
            }
            this.config = config
            this.appContext = context.applicationContext
            this.dispatchers = dispatchers
            this.eventBuffer = BoundedEventBuffer()
            this.logBuffer = BoundedLogBuffer()
            this.networkBuffer = BoundedNetworkBuffer()
            if (config.enabled) {
                enableLocked()
            } else {
                runtimeState = RuntimeState.DISABLED
                stateMachine = SessionStateMachine(SessionState.DISABLED)
                _sessionState.value = SessionState.DISABLED
            }
            return SdkResult.Success
        }
    }

    public fun setEnabled(enabled: Boolean): SdkResult {
        synchronized(lock) {
            if (runtimeState == RuntimeState.UNINITIALIZED) return SdkResult.NotInitialized
            if (enabled) {
                enableLocked()
            } else {
                disableLocked()
            }
            return SdkResult.Success
        }
    }

    public fun isInitialized(): Boolean = synchronized(lock) {
        runtimeState != RuntimeState.UNINITIALIZED
    }

    public fun isEnabled(): Boolean = synchronized(lock) {
        runtimeState == RuntimeState.ENABLED
    }

    public fun currentCapturePolicy(): com.awbuilds.reproparcel.core.config.CapturePolicy? =
        synchronized(lock) { config?.capturePolicy }

    public fun currentBuildInfo(): com.awbuilds.reproparcel.core.model.BuildInfo? =
        synchronized(lock) { config?.buildInfo }

    public fun currentRetentionPolicy(): com.awbuilds.reproparcel.core.config.RetentionPolicy? =
        synchronized(lock) { config?.retention }

    public fun currentCollectionConfigurationId(): String? =
        synchronized(lock) { config?.let(ConfigValidator::collectionConfigurationId) }

    @MainThread
    public fun showReporter(activity: Activity): SdkResult {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            return SdkResult.Failure(ErrorCode.WRONG_THREAD)
        }
        synchronized(lock) {
            if (runtimeState == RuntimeState.UNINITIALIZED) return SdkResult.NotInitialized
            if (runtimeState == RuntimeState.DISABLED) return SdkResult.Disabled
        }
        if (activity.isFinishing || activity.isDestroyed) return SdkResult.HostUnavailable
        // The Compose reporter is delivered in Phase 2. Returning a typed
        // unsupported result is safer than pretending a session started.
        return SdkResult.Unsupported
    }

    public fun registerEvidenceProvider(registration: ProviderRegistration): SdkResult {
        if (!registration.id.matches(Regex("^[A-Za-z0-9_.:-]{1,64}$"))) {
            return SdkResult.Failure(ErrorCode.INVALID_CONFIG)
        }
        if (registration.timeoutMs !in 1..5_000 || registration.maxBytes !in 1..(1L * 1024 * 1024)) {
            return SdkResult.Failure(ErrorCode.INVALID_CONFIG)
        }
        synchronized(lock) {
            if (runtimeState == RuntimeState.UNINITIALIZED) return SdkResult.NotInitialized
            if (providers.containsKey(registration.id)) {
                return SdkResult.Failure(ErrorCode.ALREADY_INITIALIZED)
            }
            providers[registration.id] = registration
            return SdkResult.Success
        }
    }

    internal suspend fun collectProviderForTest(id: String): ProviderResult {
        val registration = synchronized(lock) { providers[id] } ?: return ProviderResult.Failed(ErrorCode.PROVIDER_FAILED)
        return try {
            val result = kotlinx.coroutines.withTimeout(registration.timeoutMs) {
                registration.provider.collect()
            }
            when (result) {
                is ProviderResult.Collected ->
                    if (result.bytes.size.toLong() <= registration.maxBytes) result
                    else ProviderResult.Failed(ErrorCode.LIMIT_EXCEEDED)
                else -> result
            }
        } catch (_: kotlinx.coroutines.TimeoutCancellationException) {
            ProviderResult.Omitted(OmissionReason.PROVIDER_FAILED)
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            // The provider is an untrusted host callback. Convert only its
            // recoverable failure to a typed result; never surface its message.
            ProviderResult.Failed(ErrorCode.PROVIDER_FAILED)
        }
    }

    public fun recordBreadcrumb(breadcrumb: Breadcrumb): SdkResult {
        synchronized(lock) {
            if (runtimeState == RuntimeState.UNINITIALIZED) return SdkResult.NotInitialized
            if (runtimeState == RuntimeState.DISABLED) return SdkResult.Disabled
            val safeAction = breadcrumb.actionId?.let {
                if (it.matches(Regex("^[A-Za-z0-9_.:-]{1,128}$"))) it else null
            }
            val safeScreen = breadcrumb.screenId?.let {
                if (it.matches(Regex("^[A-Za-z0-9_.:-]{1,128}$"))) it else null
            }
            val event = ReportEvent(
                sequence = ++sequence,
                offsetMs = 0L,
                category = breadcrumb.category.take(64),
                actionId = safeAction,
                screenId = safeScreen,
                attributes = Redactor.sanitizeAttributes(breadcrumb.attributes),
                outcome = breadcrumb.outcome?.take(64),
            )
            return if (eventBuffer?.add(event) == true) SdkResult.Success
            else SdkResult.Failure(ErrorCode.LIMIT_EXCEEDED)
        }
    }

    public fun recordLog(event: LogEvent): SdkResult {
        synchronized(lock) {
            if (runtimeState == RuntimeState.UNINITIALIZED) return SdkResult.NotInitialized
            if (runtimeState == RuntimeState.DISABLED) return SdkResult.Disabled
            val safeLevel = event.level.lowercase().takeIf {
                it in setOf("debug", "info", "warn", "error")
            } ?: "info"
            val safeCode = event.code.takeIf {
                it.matches(Regex("^[A-Za-z0-9_.:-]{1,80}$"))
            } ?: "event"
            val safe = StructuredLogEvent(
                sequence = ++sequence,
                offsetMs = 0L,
                level = safeLevel,
                code = safeCode,
                attributes = Redactor.sanitizeAttributes(event.attributes),
            )
            return if (logBuffer?.add(safe) == true) SdkResult.Success
            else SdkResult.Failure(ErrorCode.LIMIT_EXCEEDED)
        }
    }

    public fun recordNetworkMetadata(metadata: NetworkMetadata): SdkResult {
        synchronized(lock) {
            if (runtimeState == RuntimeState.UNINITIALIZED) return SdkResult.NotInitialized
            if (runtimeState == RuntimeState.DISABLED) return SdkResult.Disabled
            val method = Redactor.sanitize(SafeAttribute.HTTP_METHOD, metadata.method)
                ?: return SdkResult.Omission(OmissionReason.POLICY_DISABLED)
            val route = metadata.routeTemplate?.let {
                Redactor.sanitize(SafeAttribute.ROUTE_TEMPLATE, it)
            }
            val safe = NetworkMetadata(
                method = method,
                statusCode = metadata.statusCode.coerceIn(100, 599),
                durationMs = metadata.durationMs.coerceAtLeast(0),
                routeTemplate = route,
                outcome = metadata.outcome.take(32),
            )
            return if (networkBuffer?.add(safe) == true) SdkResult.Success
            else SdkResult.Failure(ErrorCode.LIMIT_EXCEEDED)
        }
    }

    public fun registerSemanticNode(node: SemanticNode): SdkResult {
        synchronized(lock) {
            if (runtimeState == RuntimeState.UNINITIALIZED) return SdkResult.NotInitialized
            if (runtimeState == RuntimeState.DISABLED) return SdkResult.Disabled
            if (node.nodeId.isBlank() || node.nodeId.length > 64 ||
                node.role.isBlank() || node.role.length > 64 ||
                node.bounds.left < 0 || node.bounds.top < 0 ||
                node.bounds.right <= node.bounds.left || node.bounds.bottom <= node.bounds.top
            ) {
                return SdkResult.Failure(ErrorCode.INVALID_CONFIG)
            }
            if (semanticNodes.size >= com.awbuilds.reproparcel.core.limits.Limits.MAX_SEMANTIC_NODES &&
                !semanticNodes.containsKey(node.nodeId)
            ) {
                return SdkResult.Failure(ErrorCode.LIMIT_EXCEEDED)
            }
            val safe = if (node.sensitive) {
                node.copy(label = null, testTag = null, state = emptyMap())
            } else {
                node.copy(
                    label = node.label?.take(120),
                    testTag = node.testTag?.take(120),
                    state = Redactor.sanitizeAttributes(node.state),
                )
            }
            semanticNodes[node.nodeId] = safe
            return SdkResult.Success
        }
    }

    public fun semanticSnapshot(): SemanticsSnapshot {
        synchronized(lock) {
            var truncated = false
            val bounded = semanticNodes.values.filter { node ->
                var depth = 0
                var parent = node.parentId
                while (parent != null && depth <= com.awbuilds.reproparcel.core.limits.Limits.MAX_SEMANTIC_DEPTH) {
                    depth += 1
                    parent = semanticNodes[parent]?.parentId
                }
                if (depth > com.awbuilds.reproparcel.core.limits.Limits.MAX_SEMANTIC_DEPTH) {
                    truncated = true
                    false
                } else {
                    true
                }
            }.toMutableList()
            while (bounded.isNotEmpty()) {
                val candidate = SemanticsSnapshot(bounded, truncated)
                val bytes = com.awbuilds.reproparcel.core.serialization.ReproParcelJson.json
                    .encodeToString(SemanticsSnapshot.serializer(), candidate)
                    .toByteArray(Charsets.UTF_8).size
                if (bytes <= com.awbuilds.reproparcel.core.limits.Limits.MAX_SEMANTIC_BYTES) {
                    return candidate
                }
                truncated = true
                bounded.removeAt(bounded.lastIndex)
            }
            return SemanticsSnapshot(emptyList(), truncated)
        }
    }

    public fun clearSemanticEvidence(): SdkResult {
        synchronized(lock) {
            semanticNodes.clear()
            return SdkResult.Success
        }
    }

    internal fun networkSnapshotForTest(): List<NetworkMetadata> =
        synchronized(lock) { networkBuffer?.snapshot().orEmpty() }

    public fun deleteLocalReports(): SdkResult {
        synchronized(lock) {
            if (runtimeState == RuntimeState.UNINITIALIZED) return SdkResult.NotInitialized
            // Draft storage is delivered in Phase 2. This is an explicit,
            // side-effect-free operation until then.
            return SdkResult.Success
        }
    }

    public fun advanceSession(event: SessionEvent): SdkResult {
        synchronized(lock) {
            if (runtimeState == RuntimeState.UNINITIALIZED) return SdkResult.NotInitialized
            if (runtimeState == RuntimeState.DISABLED && event != SessionEvent.Enable) {
                return SdkResult.Disabled
            }
            val transition = stateMachine.transition(event)
            _sessionState.value = transition.to
            return transition.result
        }
    }

    internal fun eventSnapshotForTest(): List<ReportEvent> =
        synchronized(lock) { eventBuffer?.snapshot().orEmpty() }

    internal fun logSnapshotForTest(): List<StructuredLogEvent> =
        synchronized(lock) { logBuffer?.snapshot().orEmpty() }

    internal fun hasActiveScopeForTest(): Boolean =
        synchronized(lock) { scope != null }

    internal fun launchForTest(block: suspend kotlinx.coroutines.CoroutineScope.() -> Unit): Job? =
        synchronized(lock) { scope?.launch(block = block) }

    internal fun resetForTest() {
        synchronized(lock) {
            scope?.cancel()
            scope = null
            runtimeState = RuntimeState.UNINITIALIZED
            config = null
            appContext = null
            eventBuffer = null
            logBuffer = null
            networkBuffer = null
            semanticNodes.clear()
            providers.clear()
            sequence = 0L
            stateMachine = SessionStateMachine()
            _sessionState.value = SessionState.DISABLED
        }
    }

    private fun enableLocked() {
        if (runtimeState == RuntimeState.ENABLED) return
        scope = CoroutineScope(SupervisorJob() + dispatchers.io)
        runtimeState = RuntimeState.ENABLED
        stateMachine = SessionStateMachine(SessionState.DISABLED)
        stateMachine.transition(SessionEvent.Enable)
        _sessionState.value = stateMachine.state
    }

    private fun disableLocked() {
        scope?.cancel()
        scope = null
        runtimeState = RuntimeState.DISABLED
        stateMachine.transition(SessionEvent.Disable)
        _sessionState.value = SessionState.DISABLED
    }
}
