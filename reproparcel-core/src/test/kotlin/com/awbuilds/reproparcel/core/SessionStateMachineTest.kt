// SPDX-License-Identifier: AGPL-3.0-only
package com.awbuilds.reproparcel.core

import com.awbuilds.reproparcel.core.result.ErrorCode
import com.awbuilds.reproparcel.core.result.SdkResult
import com.awbuilds.reproparcel.core.state.SessionEvent
import com.awbuilds.reproparcel.core.state.SessionState
import com.awbuilds.reproparcel.core.state.SessionStateMachine
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionStateMachineTest {
    @Test
    fun fullHappyPathEndsInCompleted() {
        val machine = SessionStateMachine()
        machine.transition(SessionEvent.Enable)
        machine.transition(SessionEvent.StartCapture)
        machine.transition(SessionEvent.CaptureSucceeded)
        machine.transition(SessionEvent.Review)
        machine.transition(SessionEvent.Finalize)
        machine.transition(SessionEvent.FinalizeSucceeded)
        assertEquals(SessionState.EXPORTABLE, machine.state)
        machine.transition(SessionEvent.SaveSucceeded)
        assertEquals(SessionState.COMPLETED, machine.state)
    }

    @Test
    fun duplicateExportIsRejected() {
        val machine = SessionStateMachine()
        machine.transition(SessionEvent.Enable)
        machine.transition(SessionEvent.StartCapture)
        machine.transition(SessionEvent.CaptureSucceeded)
        machine.transition(SessionEvent.Review)
        machine.transition(SessionEvent.Finalize)
        machine.transition(SessionEvent.FinalizeSucceeded)
        machine.transition(SessionEvent.SaveSucceeded)
        val duplicate = machine.transition(SessionEvent.SaveSucceeded)
        assertEquals(SdkResult.Failure(ErrorCode.INVALID_TRANSITION), duplicate.result)
        assertEquals(SessionState.COMPLETED, machine.state)
    }

    @Test
    fun hostDestructionCancelsActiveSession() {
        val machine = SessionStateMachine()
        machine.transition(SessionEvent.Enable)
        machine.transition(SessionEvent.StartCapture)
        machine.transition(SessionEvent.CaptureSucceeded)
        machine.transition(SessionEvent.Review)
        machine.transition(SessionEvent.HostDestroyed)
        assertEquals(SessionState.CANCELLED, machine.state)
    }

    @Test
    fun disableStopsTheSession() {
        val machine = SessionStateMachine()
        machine.transition(SessionEvent.Enable)
        machine.transition(SessionEvent.StartCapture)
        val result = machine.transition(SessionEvent.Disable)
        assertEquals(SdkResult.Success, result.result)
        assertEquals(SessionState.DISABLED, machine.state)
    }

    @Test
    fun omittedCaptureStillAllowsATextReport() {
        val machine = SessionStateMachine()
        machine.transition(SessionEvent.Enable)
        machine.transition(SessionEvent.StartCapture)
        machine.transition(SessionEvent.CaptureOmitted)
        assertEquals(SessionState.EDITING, machine.state)
    }

    @Test
    fun resetStartsANewReadySession() {
        val machine = SessionStateMachine()
        machine.transition(SessionEvent.Enable)
        machine.transition(SessionEvent.StartCapture)
        machine.transition(SessionEvent.CaptureSucceeded)
        machine.transition(SessionEvent.Reset)
        assertEquals(SessionState.READY, machine.state)
    }
}
