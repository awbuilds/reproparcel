// SPDX-License-Identifier: Apache-2.0
package com.awbuilds.reproparcel.core.state

import com.awbuilds.reproparcel.core.result.ErrorCode
import com.awbuilds.reproparcel.core.result.SdkResult

public enum class SessionState {
    DISABLED,
    READY,
    CAPTURING,
    EDITING,
    REVIEWING,
    FINALIZING,
    EXPORTABLE,
    COMPLETED,
    CANCELLED,
    EXPORT_FAILED,
    SHARE_LAUNCHED,
}

public sealed interface SessionEvent {
    public data object Enable : SessionEvent
    public data object Disable : SessionEvent
    public data object StartCapture : SessionEvent
    public data object CaptureSucceeded : SessionEvent
    public data object CaptureOmitted : SessionEvent
    public data object CancelCapture : SessionEvent
    public data object Edit : SessionEvent
    public data object Review : SessionEvent
    public data object Finalize : SessionEvent
    public data object FinalizeSucceeded : SessionEvent
    public data object FinalizeFailed : SessionEvent
    public data object SaveSucceeded : SessionEvent
    public data object SaveCancelled : SessionEvent
    public data object ShareLaunched : SessionEvent
    public data object ExportFailed : SessionEvent
    public data object HostDestroyed : SessionEvent
    public data object Reset : SessionEvent
}

public data class Transition(
    public val from: SessionState,
    public val to: SessionState,
    public val result: SdkResult,
)

public class SessionStateMachine(initial: SessionState = SessionState.DISABLED) {
    public var state: SessionState = initial
        private set

    public fun transition(event: SessionEvent): Transition {
        val from = state
        val to = target(from, event)
        return if (to == null) {
            Transition(from, from, SdkResult.Failure(ErrorCode.INVALID_TRANSITION))
        } else {
            state = to
            Transition(from, to, SdkResult.Success)
        }
    }

    private fun target(from: SessionState, event: SessionEvent): SessionState? {
        if (event == SessionEvent.Disable) return SessionState.DISABLED
        if (event == SessionEvent.HostDestroyed) {
            return if (from in ACTIVE_STATES) SessionState.CANCELLED else SessionState.DISABLED
        }

        return when (from) {
            SessionState.DISABLED -> when (event) {
                SessionEvent.Enable -> SessionState.READY
                else -> null
            }

            SessionState.READY -> when (event) {
                SessionEvent.StartCapture -> SessionState.CAPTURING
                else -> null
            }

            SessionState.CAPTURING -> when (event) {
                SessionEvent.CaptureSucceeded,
                SessionEvent.CaptureOmitted,
                SessionEvent.CancelCapture,
                -> SessionState.EDITING

                SessionEvent.Reset -> SessionState.READY
                else -> null
            }

            SessionState.EDITING -> when (event) {
                SessionEvent.Review -> SessionState.REVIEWING
                SessionEvent.Reset -> SessionState.READY
                else -> null
            }

            SessionState.REVIEWING -> when (event) {
                SessionEvent.Edit -> SessionState.EDITING
                SessionEvent.Finalize -> SessionState.FINALIZING
                SessionEvent.Reset -> SessionState.READY
                else -> null
            }

            SessionState.FINALIZING -> when (event) {
                SessionEvent.FinalizeSucceeded -> SessionState.EXPORTABLE
                SessionEvent.FinalizeFailed -> SessionState.EXPORT_FAILED
                else -> null
            }

            SessionState.EXPORTABLE -> when (event) {
                SessionEvent.SaveSucceeded -> SessionState.COMPLETED
                SessionEvent.SaveCancelled -> SessionState.CANCELLED
                SessionEvent.ShareLaunched -> SessionState.SHARE_LAUNCHED
                SessionEvent.ExportFailed -> SessionState.EXPORT_FAILED
                SessionEvent.Reset -> SessionState.READY
                else -> null
            }

            SessionState.COMPLETED,
            SessionState.CANCELLED,
            SessionState.EXPORT_FAILED,
            SessionState.SHARE_LAUNCHED,
            -> when (event) {
                SessionEvent.Reset -> SessionState.READY
                else -> null
            }
        }
    }

    private companion object {
        val ACTIVE_STATES = setOf(
            SessionState.CAPTURING,
            SessionState.EDITING,
            SessionState.REVIEWING,
            SessionState.FINALIZING,
            SessionState.EXPORTABLE,
        )
    }
}
